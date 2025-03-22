from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import FileResponse
import tensorflow as tf
import numpy as np
import cv2
import mediapipe as mp
import os

# Initialize FastAPI app
app = FastAPI()
print("FastAPI working directory:", os.getcwd())

# Load LSTM model
MODEL_PATH = r"C:\Users\User\VitalMixFYP\Backend\vitalmix-vscode\ml\form_check_lstm_model.h5"
model = tf.keras.models.load_model(MODEL_PATH)

# Define constants
VIDEO_SAVE_DIR = os.path.join(os.path.dirname(__file__), "temp")  # Temporary save directory
os.makedirs(VIDEO_SAVE_DIR, exist_ok=True)

KEYPOINTS = [
    "left_wrist", "left_elbow", "left_shoulder",
    "left_ankle", "left_knee", "left_hip",
    "right_ankle", "right_knee", "right_hip",
    "right_wrist", "right_elbow", "right_shoulder"
]
NUM_KEYPOINTS = 12
NUM_ANGLES = 4
NUM_ALIGNMENT = 1
FEATURE_SIZE = NUM_KEYPOINTS * 3 + NUM_ANGLES + NUM_ALIGNMENT
MAX_FRAMES = 100

# Setup MediaPipe
mp_pose = mp.solutions.pose
pose_model = mp_pose.Pose()

# Calculate joint angles
def calculate_joint_angle(a, b, c):
    if not all([a, b, c]) or any(v is None for v in (a.get("x"), a.get("y"), b.get("x"), b.get("y"), c.get("x"), c.get("y"))):
        return None
    a, b, c = np.array([a["x"], a["y"]]), np.array([b["x"], b["y"]]), np.array([c["x"], c["y"]])
    ab, cb = a - b, c - b
    dot = np.dot(ab, cb)
    norm_ab = np.linalg.norm(ab)
    norm_cb = np.linalg.norm(cb)
    if norm_ab == 0 or norm_cb == 0:
        return None
    cosine = np.clip(dot / (norm_ab * norm_cb), -1.0, 1.0)
    return float(np.degrees(np.arccos(cosine)))

# Extract keypoints for model input
def extract_keypoints_for_model(video_path):
    cap = cv2.VideoCapture(video_path)
    frames_data = []
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose_model.process(rgb)
        if results.pose_landmarks:
            kp_map = {}
            for lbl in KEYPOINTS:
                idx = getattr(mp_pose.PoseLandmark, lbl.upper(), None)
                if idx is not None:
                    lm = results.pose_landmarks.landmark[idx]
                    kp_map[lbl] = [lm.x, lm.y, lm.visibility]
                else:
                    kp_map[lbl] = [0, 0, 0]
            vec = [v for lbl in KEYPOINTS for v in kp_map[lbl]]
            raw_kp = {lbl: {"x": kp_map[lbl][0], "y": kp_map[lbl][1]} for lbl in KEYPOINTS}
            angles = [
                calculate_joint_angle(raw_kp["left_ankle"], raw_kp["left_knee"], raw_kp["left_hip"]),
                calculate_joint_angle(raw_kp["right_ankle"], raw_kp["right_knee"], raw_kp["right_hip"]),
                calculate_joint_angle(raw_kp["left_wrist"], raw_kp["left_elbow"], raw_kp["left_shoulder"]),
                calculate_joint_angle(raw_kp["right_wrist"], raw_kp["right_elbow"], raw_kp["right_shoulder"]),
            ]
            align = abs(raw_kp["left_shoulder"]["x"] - raw_kp["right_shoulder"]["x"]) if all(
                raw_kp[k]["x"] is not None for k in ["left_shoulder", "right_shoulder"]) else 0
            vec += [a if a is not None else 0 for a in angles]
            vec.append(align)
            frames_data.append(vec)
        else:
            frames_data.append([0] * FEATURE_SIZE)
    cap.release()
    return np.array(frames_data, dtype=np.float32) if frames_data else np.zeros((1, FEATURE_SIZE), dtype=np.float32)

    # Function to predict form accuracy
def predict_form(video_path, exercise_name):
    frames = extract_keypoints_for_model(video_path)
    if len(frames) == 0:
        return {"error": "No frames data"}

    # Pad or trim to MAX_FRAMES
    padded = np.zeros((MAX_FRAMES, FEATURE_SIZE), dtype=np.float32)
    if frames.shape[0] > MAX_FRAMES:
        frames = frames[-MAX_FRAMES:]
    padded[-frames.shape[0]:, :] = frames

    model_input = np.expand_dims(padded, axis=0)
    prediction = model.predict(model_input)[0][0]
    accuracy = round(prediction * 100, 2)
    feedback = "Excellent form!" if accuracy >= 90 else "Needs improvement."
    return {"exercise": exercise_name, "accuracy": accuracy, "feedback": feedback}

# Generate a processed video with keypoint overlay
def process_and_overlay_video(input_path, output_path):
    cap = cv2.VideoCapture(input_path)
    if not cap.isOpened():
        print(f"ERROR: Cannot open video at {input_path}")
        return False

    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fourcc = cv2.VideoWriter_fourcc(*'avc1')
    out = cv2.VideoWriter(output_path, fourcc, 30.0, (width, height))

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose_model.process(rgb)
        if results.pose_landmarks:
            for lm in results.pose_landmarks.landmark:
                x, y = int(lm.x * width), int(lm.y * height)
                cv2.circle(frame, (x, y), 5, (0, 255, 0), -1)

        out.write(frame)

    cap.release()
    out.release()
    return os.path.exists(output_path)

# Main endpoint
@app.post("/predict-form/")
async def predict_form_api(file: UploadFile = File(...), exercise: str = Form(...)):
    filename = file.filename
    input_path = os.path.join(VIDEO_SAVE_DIR, filename).replace("\\", "/")
    output_path = input_path.replace(".mp4", "_processed.mp4")

    with open(input_path, "wb") as f:
        f.write(await file.read())

    print(f"Video uploaded at: {input_path}")

    prediction = predict_form(input_path, exercise)

    # Generate processed video with keypoints
    success = process_and_overlay_video(input_path, output_path)
    if not success:
        return {"error": "Failed to generate processed video"}

    print(f"Processed video ready at: {output_path}")

    return {
        "success": True,
        "message": "Prediction complete",
        "data": {
            **prediction,
            "video_url": f"http://10.0.2.2:8000/stream-video/?path=./temp/{os.path.basename(output_path)}"
        }
    }

# Streaming endpoint (returns the processed video directly)
@app.get("/stream-video/")
async def stream_video(path: str):
    full_path = os.path.abspath(path)
    if not os.path.exists(full_path):
        return {"error": "Video file not found"}
    
    print(f"Serving streamable video at: {full_path}")
    return FileResponse(full_path, media_type="video/mp4", filename=os.path.basename(full_path))

