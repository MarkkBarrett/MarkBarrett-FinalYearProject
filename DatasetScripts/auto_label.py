import cv2
import json
import mediapipe as mp
import numpy as np
import os

# input and output paths 
video_path = r"C:\Users\User\exercise-dataset\data\pushups\incorrect\badpushup_example18.mp4"
manual_label_json = r"C:\Users\User\exercise-dataset\data\pushups\incorrect\badpushup_example18_labelled_points.json"
output_path = r"C:\Users\User\exercise-dataset\data\pushups\incorrect\badpushup_example18_labelled_points.json"

# Load manually labeled points  
with open(manual_label_json, "r") as f:
    initial_labels = json.load(f)

# Extract only manually labeled keypoints
labeled_keypoints = list(initial_labels.keys())

# Initialize MP Pose detector 
mp_pose = mp.solutions.pose
pose = mp_pose.Pose()

# Calculate joint angle between 3 points (a,b,c)
def calculate_angle(a, b, c):
    if None in (a, b, c):
        return None
    a, b, c = np.array(a), np.array(b), np.array(c)
    ba = a - b
    bc = c - b
    cosine = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc))
    return np.degrees(np.arccos(np.clip(cosine, -1.0, 1.0)))

# Extract MP keypoints from a single frame 
def extract_keypoints_from_frame(results):
    frame_keypoints = {}
    for kp in labeled_keypoints:
        landmark = getattr(mp_pose.PoseLandmark, kp.upper(), None)
        if landmark:
            point = results.pose_landmarks.landmark[landmark]
            frame_keypoints[kp] = [point.x, point.y, point.visibility]
    return frame_keypoints

# Detect pushup reps based on elbow angle drops 
def detect_pushup_reps(elbow_angle_series, down_thresh=135, up_thresh=150):
    reps = []
    in_rep = False
    start_frame = 0

    for i, angle in enumerate(elbow_angle_series):
        if angle is None:
            continue

        # Detect beginning of a rep (elbow bending down)
        if angle < down_thresh and not in_rep:
            in_rep = True
            start_frame = i

        # Detect end of a rep (elbow straightened up)
        if angle > up_thresh and in_rep:
            in_rep = False
            reps.append({"start_frame": start_frame, "end_frame": i})

    return reps

# Right side version
def detect_pushup_reps_right(right_elbow_angle_series, down_thresh=135, up_thresh=150):
    reps = []
    in_rep = False
    start_frame = 0

    for i, angle in enumerate(right_elbow_angle_series):
        if angle is None:
            continue

        # Detect beginning of a rep (elbow bending down)
        if angle < down_thresh and not in_rep:
            in_rep = True
            start_frame = i

        # Detect end of a rep (elbow straightened up)
        if angle > up_thresh and in_rep:
            in_rep = False
            reps.append({"start_frame": start_frame, "end_frame": i})

    return reps

# Detect squatting reps based on knee angle drops 
def detect_squat_reps(knee_angle_series, down_thresh=110, up_thresh=160):
    reps = []
    in_rep = False
    start_frame = 0

    for i, angle in enumerate(knee_angle_series):
        if angle is None:
            continue

        # Detect beginning of a rep (knee bending down)
        if angle < down_thresh and not in_rep:
            in_rep = True
            start_frame = i

        # Detect end of a rep (knee straightened up)
        if angle > up_thresh and in_rep:
            in_rep = False
            reps.append({"start_frame": start_frame, "end_frame": i})

    return reps

# Process the video and auto-label each frame + reps 
def process_video(video_path, output_path):
    USE_LEFT_ELBOW = False  # use either right or left elbow
    cap = cv2.VideoCapture(video_path)
    auto_labeled = {
        "exercise": "pushup",
        "keypoints": labeled_keypoints,
        "frames": [],
        "reps": []
    }

    elbow_angles_left = []  # Track left elbow angles
    elbow_angles_right = []  # Track right elbow angles

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # Convert frame to RGB for MP
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

        # Detect pose landmarks
        results = pose.process(rgb)
        if not results.pose_landmarks:
            auto_labeled["frames"].append({"keypoints": {}, "left_elbow_angle": None, "right_elbow_angle": None})
            elbow_angles_left.append(None)
            elbow_angles_right.append(None)
            continue

        # Get 2D keypoints for labeled joints
        keypoints = extract_keypoints_from_frame(results)

        # Fetch left elbow angle
        try:
            left_elbow_angle = calculate_angle(
                keypoints.get("left_wrist")[:2],
                keypoints.get("left_elbow")[:2],
                keypoints.get("left_shoulder")[:2]
            )
        except:
            left_elbow_angle = None

        # Fetch right elbow angle
        try:
            right_elbow_angle = calculate_angle(
                keypoints.get("right_wrist")[:2],
                keypoints.get("right_elbow")[:2],
                keypoints.get("right_shoulder")[:2]
            )
        except:
            right_elbow_angle = None

        # Save frame data
        auto_labeled["frames"].append({
            "keypoints": keypoints,
            "left_elbow_angle": left_elbow_angle,
            "right_elbow_angle": right_elbow_angle
        })

        elbow_angles_left.append(left_elbow_angle)
        elbow_angles_right.append(right_elbow_angle)

    cap.release()

    # Use correct elbow angle series depending on setting
    if USE_LEFT_ELBOW:
        detected_reps = detect_pushup_reps(elbow_angles_left)
    else:
        detected_reps = detect_pushup_reps_right(elbow_angles_right)
    auto_labeled["reps"] = detected_reps

    # Save data to same JSON
    with open(output_path, "w") as f:
        json.dump(auto_labeled, f, indent=4)
    print(f"Auto-labeled data saved to {output_path}")

# run
process_video(video_path, output_path)