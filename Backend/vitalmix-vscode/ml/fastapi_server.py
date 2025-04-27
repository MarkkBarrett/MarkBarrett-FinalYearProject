from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import FileResponse
import tensorflow as tf
import numpy as np
import cv2
import mediapipe as mp
import os
from pymongo import MongoClient
from datetime import datetime

# MongoDB Setup
MONGO_URI = "mongodb://localhost:27017"  # or your Atlas connection string
mongo_client = MongoClient(MONGO_URI)
db = mongo_client["VitalMix"]  # Database 
form_results_collection = db["formResults"]  # Collection name

# Initialize FastAPI app
app = FastAPI()
print("FastAPI working directory:", os.getcwd())

# Load new LSTM model
MODEL_PATH = r"C:\Users\User\VitalMixFYP\Backend\vitalmix-vscode\ml\form_check_lstm_model_v2.h5"
model = tf.keras.models.load_model(MODEL_PATH)

# Define constants
VIDEO_SAVE_DIR = os.path.join(os.path.dirname(__file__), "temp")  # Temporary save directory
os.makedirs(VIDEO_SAVE_DIR, exist_ok=True)

KEYPOINTS = [
    "left_shoulder", "left_hip", "left_knee", "left_ankle", "left_heel",
    "right_shoulder", "right_hip", "right_knee", "right_ankle", "right_heel"
]
FEATURE_SIZE = len(KEYPOINTS) * 3 + 1  # x, y, visibility per point + 1 angle
MAX_FRAMES = 100

# Setup MediaPipe
mp_pose = mp.solutions.pose
pose_model = mp_pose.Pose()

# Calculate joint angles
def calculate_joint_angle(a, b, c):
    if not all([a, b, c]):
        return None
    a, b, c = np.array(a), np.array(b), np.array(c)
    ba = a - b
    bc = c - b
    cosine = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc) + 1e-8)
    return float(np.degrees(np.arccos(np.clip(cosine, -1.0, 1.0))))

# Detect reps using left knee angle thresholds
def detect_reps_from_angles(angle_series, down_thresh=110, up_thresh=160):
    reps = []
    in_rep = False
    start = 0
    for i, angle in enumerate(angle_series):
        if angle is None:
            continue
        if angle < down_thresh and not in_rep:
            in_rep = True
            start = i
        elif angle > up_thresh and in_rep:
            in_rep = False
            reps.append((start, i))
    return reps

# Extract keypoints for model input
def extract_keypoints_for_model(video_path):
    cap = cv2.VideoCapture(video_path)
    frames_data = []
    angle_series = []

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

            # Get x/y for angle calc
            try:
                left_knee_angle = calculate_joint_angle(
                    [kp_map["left_ankle"][0], kp_map["left_ankle"][1]],
                    [kp_map["left_knee"][0], kp_map["left_knee"][1]],
                    [kp_map["left_hip"][0], kp_map["left_hip"][1]]
                )
            except:
                left_knee_angle = None

            angle_series.append(left_knee_angle)

            # Flatten keypoints
            frame_features = []
            for lbl in KEYPOINTS:
                x, y, v = kp_map.get(lbl, [0, 0, 0])
                frame_features.extend([x, y, v])
            frame_features.append(left_knee_angle if left_knee_angle is not None else 0)

            frames_data.append(frame_features)
        else:
            frames_data.append([0] * FEATURE_SIZE)
            angle_series.append(None)

    cap.release()
    return np.array(frames_data, dtype=np.float32), angle_series

    # Predict only during reps
    # Function to predict form accuracy
def predict_form(video_path, exercise_name):
    all_frames, angle_series = extract_keypoints_for_model(video_path)

    # Detect rep segments
    rep_ranges = detect_reps_from_angles(angle_series)

    if not rep_ranges:
        return {
            "exercise": exercise_name,
            "accuracy": 0.0,
            "feedback": "No valid reps detected."
        }

    # Collect only frames within rep ranges
    rep_frames = []
    for start, end in rep_ranges:
        rep_frames.extend(all_frames[start:end + 1])

    if not rep_frames:
        return {
            "exercise": exercise_name,
            "accuracy": 0.0,
            "feedback": "Reps found but no usable frames."
        }

    # Pad or trim to MAX_FRAMES
    padded = np.zeros((MAX_FRAMES, FEATURE_SIZE), dtype=np.float32)
    rep_frames = np.array(rep_frames, dtype=np.float32)
    if len(rep_frames) > MAX_FRAMES:
        rep_frames = rep_frames[-MAX_FRAMES:]
    padded[-len(rep_frames):, :] = rep_frames

    model_input = np.expand_dims(padded, axis=0)
    prediction = model.predict(model_input)[0][0]
    accuracy = round(prediction * 100, 2)
    feedback = "Excellent form!" if accuracy >= 90 else "Needs improvement."
    return {"exercise": exercise_name, "accuracy": accuracy, "feedback": feedback}


# Generate a processed video with keypoint overlay and labeled points
def process_and_overlay_video(input_path, output_path):
    cap = cv2.VideoCapture(input_path)
    if not cap.isOpened():
        print(f"ERROR: Cannot open video at {input_path}")
        return False

    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fourcc = cv2.VideoWriter_fourcc(*'avc1')  # Using 'avc1' for streamable mp4
    out = cv2.VideoWriter(output_path, fourcc, 30.0, (width, height))

    # Only label these keypoints for now (used in squat form)
    keypoints_to_label = {
        "left_ankle", "left_knee", "left_hip",
        "right_ankle", "right_knee", "right_hip",
        "left_shoulder", "right_shoulder"
    }

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose_model.process(rgb)

        if results.pose_landmarks:
            for idx, lm in enumerate(results.pose_landmarks.landmark):
                x, y = int(lm.x * width), int(lm.y * height)
                cv2.circle(frame, (x, y), 5, (0, 255, 0), -1)

                # Get joint name from index
                joint_name = mp_pose.PoseLandmark(idx).name.lower()
                if joint_name in keypoints_to_label:
                    cv2.putText(frame, joint_name.replace("_", " "), (x + 5, y - 5),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

        out.write(frame)

    cap.release()
    out.release()
    return os.path.exists(output_path)

# Main endpoint
@app.post("/predict-form/")
async def predict_form_api(file: UploadFile = File(...), exercise: str = Form(...), userId: str = Form(...)):
    filename = file.filename
    input_path = os.path.join(VIDEO_SAVE_DIR, filename).replace("\\", "/")
    output_path = input_path.replace(".mp4", "_processed.mp4")

    with open(input_path, "wb") as f:
        f.write(await file.read())

    print(f"Video uploaded at: {input_path}")

    prediction = predict_form(input_path, exercise)

    # --- Save prediction to MongoDB ---
    form_results_collection.insert_one({
    "userId": userId,
    "exercise": exercise,
    "accuracy": prediction["accuracy"],
    "feedback": [prediction["feedback"]],  # list for new feedback
    "timestamp": datetime.utcnow()
    })

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