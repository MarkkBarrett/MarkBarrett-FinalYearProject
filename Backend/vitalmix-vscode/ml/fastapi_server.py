from fastapi import FastAPI, File, UploadFile, Form
from fastapi.responses import FileResponse
import tensorflow as tf
import numpy as np
import cv2
import mediapipe as mp
import os

# Initialize FastAPI app
app = FastAPI()

# Load LSTM Model
MODEL_PATH = r"C:\Users\User\vitalmix-vscode\ml\form_check_lstm_model.h5"  # Model path
model = tf.keras.models.load_model(MODEL_PATH)

# Define storage directory for processed videos
VIDEO_SAVE_DIR = "/sdcard/Download"
os.makedirs(VIDEO_SAVE_DIR, exist_ok=True)  # Ensure directory exists

# Initialize MediaPipe Pose model
mp_pose = mp.solutions.pose
pose_model = mp_pose.Pose()

# Define keypoints for feature extraction
KEYPOINTS = [
    "left_wrist", "left_elbow", "left_shoulder",
    "left_ankle", "left_knee", "left_hip",
    "right_ankle", "right_knee", "right_hip",
    "right_wrist", "right_elbow", "right_shoulder"
]

NUM_KEYPOINTS = 12  
NUM_ANGLES = 4  
NUM_ALIGNMENT = 1  # Additional alignment feature
FEATURE_SIZE = NUM_KEYPOINTS * 3 + NUM_ANGLES + NUM_ALIGNMENT  # Total feature size = 41
MAX_FRAMES = 100  # Maximum frames for model input

# Function to calculate joint angles
def calculate_joint_angle(point_a, point_b, point_c):
    """
    Calculate angle between three keypoints based on x, y coordinates.
    Handles missing keypoints safely.
    """
    if not all([point_a, point_b, point_c]) or any(v is None for v in (point_a.get("x"), point_a.get("y"), point_b.get("x"), point_b.get("y"), point_c.get("x"), point_c.get("y"))):
        return None  # Return None if any keypoint data is missing

    # Convert to NumPy arrays
    point_a = np.array([point_a["x"], point_a["y"]])
    point_b = np.array([point_b["x"], point_b["y"]])
    point_c = np.array([point_c["x"], point_c["y"]])

    # Compute vectors
    vector_ab = point_a - point_b
    vector_cb = point_c - point_b

    # Compute dot product and magnitudes
    dot_product = np.dot(vector_ab, vector_cb)
    magnitude_ab = np.linalg.norm(vector_ab)
    magnitude_cb = np.linalg.norm(vector_cb)

    if magnitude_ab == 0 or magnitude_cb == 0:
        return None  # Avoid division by zero

    # Compute angle in degrees
    cosine_angle = np.clip(dot_product / (magnitude_ab * magnitude_cb), -1.0, 1.0)
    return float(np.degrees(np.arccos(cosine_angle)))  

# Function to extract keypoints from video
def extract_keypoints_from_video(video_path):
    """
    Extract keypoints from each frame in a video.
    Returns a NumPy array with shape (num_frames, FEATURE_SIZE).
    """
    cap = cv2.VideoCapture(video_path)
    frames_data = []
    
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break  # Stop processing if no more frames

        # Convert to RGB for MediaPipe processing
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        pose_results = pose_model.process(rgb_frame)

        # Extract keypoints
        if pose_results.pose_landmarks:
            keypoints = []
            for label in KEYPOINTS:
                index = getattr(mp_pose.PoseLandmark, label.upper(), None)
                if index is not None:
                    landmark = pose_results.pose_landmarks.landmark[index]
                    keypoints.extend([landmark.x, landmark.y, landmark.visibility])
                else:
                    keypoints.extend([0, 0, 0])  # Fill missing keypoints with 0
            frames_data.append(keypoints)
        else:
            frames_data.append([0] * FEATURE_SIZE)  # Fill missing frames with zeros

    cap.release()

    # Ensure all frames have the correct feature size
    frames_array = [frame if len(frame) == FEATURE_SIZE else [0] * FEATURE_SIZE for frame in frames_data]
    frames_array = np.array(frames_array, dtype=np.float32)
    
    return frames_array if len(frames_array) > 0 else np.zeros((1, FEATURE_SIZE), dtype=np.float32)

# Function to predict form accuracy
def predict_form(video_path, exercise_name):
    """
    Predicts form accuracy using the trained LSTM model.
    Returns accuracy score and feedback.
    """
    frames_data = extract_keypoints_from_video(video_path)
    if len(frames_data) == 0:
        return {"error": "No frames data"}

    # Ensure consistent frame count
    processed_frames = np.zeros((MAX_FRAMES, FEATURE_SIZE), dtype=np.float32)
    if frames_data.shape[0] > MAX_FRAMES:
        frames_data = frames_data[-MAX_FRAMES:]
    processed_frames[-frames_data.shape[0]:, :] = frames_data  # Pad with zeros if needed

    model_input = np.expand_dims(processed_frames, axis=0)  # Model expects shape (1, MAX_FRAMES, FEATURE_SIZE)
    prediction = model.predict(model_input)[0][0]  # Get model prediction
    accuracy_score = round(prediction * 100, 2)  # Convert probability to percentage
    feedback_text = "Excellent form!" if accuracy_score >= 90 else "Needs improvement."
    
    return {"exercise": exercise_name, "accuracy": accuracy_score, "feedback": feedback_text}

# Function to generate the processed video with keypoint overlay
def process_and_overlay_video(input_video_path, processed_video_path):
    """
    Processes the video by extracting keypoints and overlaying them onto frames.
    Saves the processed video.
    """
    print(f"Processing video: {input_video_path}")

    cap = cv2.VideoCapture(input_video_path)
    if not cap.isOpened():
        print(f"ERROR: Could not open video file: {input_video_path}")
        return False

    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    frame_width, frame_height = int(cap.get(3)), int(cap.get(4))

    out = cv2.VideoWriter(processed_video_path, fourcc, 30.0, (frame_width, frame_height))
    frame_count = 0  

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break  

        frame_count += 1  

        # Convert to RGB for MediaPipe processing
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        pose_results = pose_model.process(rgb_frame)

        # Overlay keypoints
        if pose_results.pose_landmarks:
            for landmark in pose_results.pose_landmarks.landmark:
                x, y = int(landmark.x * frame_width), int(landmark.y * frame_height)
                cv2.circle(frame, (x, y), 5, (0, 255, 0), -1)

        out.write(frame)  # Write frame to output video

    cap.release()
    out.release()

    processed_video_exists = os.path.exists(processed_video_path)
    print(f"Processed video saved at: {processed_video_path}, Exists: {processed_video_exists}")

    return processed_video_exists

# API Endpoint to upload video and predict form
@app.post("/predict-form/")
async def predict_form_api(file: UploadFile = File(...), exercise: str = Form(...)):
    """
    Receives a video file and exercise name, processes keypoints,
    predicts form accuracy, and returns results.
    """
    # Define file paths
    input_video_path = os.path.join(VIDEO_SAVE_DIR, f"{file.filename}").replace("\\", "/")
    processed_video_path = os.path.join(VIDEO_SAVE_DIR, f"{file.filename}_processed.mp4").replace("\\", "/")
    # Generate processed video
    video_success = process_and_overlay_video(input_video_path, processed_video_path)

    if not video_success:
        print("ERROR: Processed video generation failed.")
        return {"error": "Failed to generate processed video"}



    # Save uploaded video
    with open(input_video_path, "wb") as buffer:
        buffer.write(file.file.read())

    print(f"Uploaded video saved at: {input_video_path}, Exists: {os.path.exists(input_video_path)}")

    # Ensure exercise name is clean
    exercise_name = exercise.split("/")[0]

    # Predict form accuracy
    prediction_result = predict_form(input_video_path, exercise_name)

    # Verify if the processed video exists
    processed_video_exists = os.path.exists(processed_video_path)
    print(f"Processed video should be at: {processed_video_path}, Exists: {processed_video_exists}")

    if not processed_video_exists:
        print("WARNING: Processed video file is missing!")

        test_file_path = os.path.join(VIDEO_SAVE_DIR, "test_write.txt").replace("\\", "/")
        try:
            with open(test_file_path, "w") as f:
                f.write("Test write successful!")
            print(f"success : {test_file_path}, Exists: {os.path.exists(test_file_path)}")
        except Exception as e:
            print(f"Failed : {test_file_path}: {e}")

    # Construct response with a downloadable video URL
    response_data = {
        "success": True,
        "message": "Prediction complete",
        "data": {
            **prediction_result,
            "video_url": f"http://10.0.2.2:8000/download-video/?path={processed_video_path}"
        }
    }

    print("FastAPI Response:", response_data)
    return response_data

# API Endpoint to serve processed video for download
@app.get("/download-video/")
async def download_video(path: str):
    
    # Serves the processed video from the correct directory.
    
    if not os.path.exists(path):
        print(f"ERROR: File not found: {path}")
        return {"error": "File not found"}

    print(f"Serving video: {path}")
    return FileResponse(path, media_type="video/mp4")