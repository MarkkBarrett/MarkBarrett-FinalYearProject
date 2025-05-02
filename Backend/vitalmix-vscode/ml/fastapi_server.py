from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import FileResponse
from pymongo import MongoClient
from datetime import datetime
import tensorflow as tf
import numpy as np
import cv2
import mediapipe as mp
import os

# Initialize FastAPI application
app = FastAPI()
print("FastAPI working directory:", os.getcwd())

# Load trained LSTM model for form accuracy prediction
MODEL_PATH = r"C:/Users/User/VitalMixFYP/Backend/vitalmix-vscode/ml/form_check_lstm_model_v3.h5"
form_check_model = tf.keras.models.load_model(MODEL_PATH)

# Directory to temporarily save uploaded videos
VIDEO_SAVE_DIR = os.path.join(os.path.dirname(__file__), "temp")
os.makedirs(VIDEO_SAVE_DIR, exist_ok=True)
MAX_FRAMES = 100

# Connect to MongoDB and select database and collection
MONGO_URI = "mongodb://localhost:27017"
mongo_client = MongoClient(MONGO_URI)
db = mongo_client["VitalMix"]
form_results_collection = db["formResults"]

# Initialize MediaPipe Pose detector
mp_pose = mp.solutions.pose
pose_detector = mp_pose.Pose()

# Calculate the angle at point B formed by points A-B-C
# point_a, point_b, point_c are [x, y] lists
# returns angle in degrees, or None on error
def calculate_joint_angle(point_a, point_b, point_c):
    try:
        a = np.array(point_a)
        b = np.array(point_b)
        c = np.array(point_c)
        ab = a - b
        cb = c - b
        dot = np.dot(ab, cb)
        norm = np.linalg.norm(ab) * np.linalg.norm(cb) + 1e-8
        cosine = np.clip(dot / norm, -1.0, 1.0)
        return float(np.degrees(np.arccos(cosine)))
    except:
        return None

# Extract keypoints and angles from video frames for model input
# Returns (feature_matrix, angle_series)
def extract_keypoints_for_model(video_path, exercise_name):
    cap = cv2.VideoCapture(video_path)
    frames_data = []
    angle_series = []

    # Define landmarks based on exercise
    if exercise_name == "squat":
        keypoints = [
            "left_shoulder", "left_hip", "left_knee", "left_ankle", "left_heel",
            "right_shoulder", "right_hip", "right_knee", "right_ankle", "right_heel"
        ]
    else:  # pushup
        keypoints = [
            "left_wrist", "left_elbow", "left_shoulder", "left_hip", "left_knee",
            "right_wrist", "right_elbow", "right_shoulder", "right_hip", "right_knee"
        ]

    feature_len = len(keypoints) * 3 + 2  # x,y,visibility per point + angle + exercise name

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose_detector.process(rgb)

        features = [0.0] * feature_len
        angle = None

        if results.pose_landmarks:
            lm_map = {}
            for name in keypoints:
                idx = getattr(mp_pose.PoseLandmark, name.upper(), None)
                if idx is not None:
                    lm = results.pose_landmarks.landmark[idx]
                    lm_map[name] = [lm.x, lm.y, lm.visibility]
                else:
                    lm_map[name] = [0.0, 0.0, 0.0]

            # Compute joint angle
            if exercise_name == "squat":
                angle = calculate_joint_angle(
                    lm_map["left_ankle"][:2], lm_map["left_knee"][:2], lm_map["left_hip"][:2]
                )
            else:
                angle = calculate_joint_angle(
                    lm_map["left_wrist"][:2], lm_map["left_elbow"][:2], lm_map["left_shoulder"][:2]
                )

            # Flatten features into vector
            idx = 0
            for name in keypoints:
                features[idx:idx+3] = lm_map[name]
                idx += 3
            features[idx] = angle if angle is not None else 0.0
            features[idx+1] = 0 if exercise_name == "squat" else 1

        frames_data.append(features)
        angle_series.append(angle)

    cap.release()
    return np.array(frames_data, dtype=np.float32), angle_series

# Detect start/end indices of each rep based on angle thresholds on angle thresholds
def detect_reps_from_angles(angle_series, exercise_name):
    reps = []
    in_rep = False
    start = 0

    if exercise_name == "squat":
        down_threshold, up_threshold = 110, 160
    else:
        down_threshold, up_threshold = 130, 150

    for i, a in enumerate(angle_series):
        if a is None:
            continue
        if not in_rep and a < down_threshold:
            in_rep = True
            start = i
        elif in_rep and a > up_threshold:
            in_rep = False
            reps.append((start, i))

    return reps

# Generate feedback messages based on reps and form quality
def generate_feedback(angle_series, rep_ranges, exercise_name):
    messages = [f"You completed {len(rep_ranges)} reps."]

    if exercise_name == "squat":
        deep_count = sum(1 for a in angle_series if a is not None and a < 90)
        if deep_count == 0:
            messages.append("Go deeper into your squat.")
        else: messages.append("good form, only small tweaks!")
    else:
        shallow_count = sum(1 for a in angle_series if a is not None and a > 80)
        if shallow_count == 0:
            messages.append("Lower your body more during push-ups.")

    return messages or ["Excellent form!"]

# Predict form accuracy and generate feedback for a video
def predict_form(video_path, exercise_name):
    frames, angles = extract_keypoints_for_model(video_path, exercise_name)
    reps = detect_reps_from_angles(angles, exercise_name)

    if not reps:
        return {"exercise": exercise_name, "accuracy": 0.0,
                "feedback": ["No valid reps detected."]}

    rep_frames = []
    for s, e in reps:
        rep_frames.extend(frames[s:e+1])

    if not rep_frames:
        return {"exercise": exercise_name, "accuracy": 0.0,
                "feedback": ["Reps found but no usable frames."]}

    # Prepare input tensor for model
    rep_arr = np.array(rep_frames, dtype=np.float32)
    padded = np.zeros((MAX_FRAMES, rep_arr.shape[1]), dtype=np.float32)
    if len(rep_arr) > MAX_FRAMES:
        rep_arr = rep_arr[-MAX_FRAMES:]
    padded[-len(rep_arr):] = rep_arr

    prediction = form_check_model.predict(np.expand_dims(padded, axis=0))[0][0]
    accuracy_pct = round((1 - prediction) * 100, 2)
    feedback = generate_feedback(angles, reps, exercise_name)

    return {"exercise": exercise_name, "accuracy": accuracy_pct, "feedback": feedback}

# Generate processed video with keypoint overlays
# Draws circles and labels on key joints

def process_and_overlay_video(input_path, output_path):
    cap = cv2.VideoCapture(input_path)
    if not cap.isOpened():
        print(f"ERROR opening video: {input_path}")
        return False

    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    writer = cv2.VideoWriter(
        output_path,
        cv2.VideoWriter_fourcc(*'avc1'),
        30.0,
        (width, height)
    )

    keypoints_to_draw = {"left_ankle","left_knee","left_hip",
                         "right_ankle","right_knee","right_hip",
                         "left_shoulder","right_shoulder"}

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose_detector.process(rgb)
        points = {}

        if results.pose_landmarks:
            for idx, lm in enumerate(results.pose_landmarks.landmark):
                name = mp_pose.PoseLandmark(idx).name.lower()
                if name in keypoints_to_draw:
                    points[name] = (int(lm.x * width), int(lm.y * height))

            for name, (x, y) in points.items():
                cv2.circle(frame, (x, y), 8, (0, 255, 0), -1)
                cv2.putText(frame, name.replace("_", " " ), (x+10, y-10),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

            connections = [("left_hip","left_knee"), ("left_knee","left_ankle"),
                           ("right_hip","right_knee"), ("right_knee","right_ankle")]
            for a, b in connections:
                if a in points and b in points:
                    cv2.line(frame, points[a], points[b], (255, 255, 255), 3)

        writer.write(frame)

    cap.release()
    writer.release()
    return os.path.exists(output_path)

# Main endpoint: receive video, run form prediction, save to DB, return result
@app.post("/predict-form/")
async def predict_form_api(file: UploadFile = File(...),exercise: str = Form(...),userId: str = Form(...)):
    file_path = os.path.join(VIDEO_SAVE_DIR, file.filename)
    with open(file_path, "wb") as f:
        f.write(await file.read())
    print(f"Video uploaded: {file_path}")

    result = predict_form(file_path, exercise)

    # Save result in MongoDB
    form_results_collection.insert_one({
        "userId": userId,
        "exercise": exercise,
        "accuracy": result["accuracy"],
        "feedback": result["feedback"],
        "timestamp": datetime.utcnow()
    })

    # Generate processed video with keypoints overlay
    output_path = file_path.replace(".mp4", "_processed.mp4")
    success = process_and_overlay_video(file_path, output_path)
    if not success:
        return {"error": "Video processing failed"}

    print(f"Processed video saved at: {output_path}")
    return {
        "success": True,
        "message": "Prediction complete",
        "data": {
            **result,   # unpack exercise, accuracy, feedback
            "video_url": f"http://10.0.2.2:8000/stream-video/?path=./temp/{os.path.basename(output_path)}"
        }
    }

# Streaming endpoint: serve processed video file
@app.get("/stream-video/")
async def stream_video(path: str):
    full_path = os.path.abspath(path)
    if not os.path.exists(full_path):
        return {"error": "File not found"}
    print(f"Streaming video: {full_path}")
    return FileResponse(full_path, media_type="video/mp4",filename=os.path.basename(full_path))