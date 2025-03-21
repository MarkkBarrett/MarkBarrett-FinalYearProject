import os
import cv2
import json
import numpy as np
import tensorflow as tf
import mediapipe as mp
import matplotlib.pyplot as plt

NUM_KEYPOINTS = 12  
NUM_ANGLES = 4
NUM_ALIGNMENT = 1
FEATURE_SIZE = NUM_KEYPOINTS*3 + NUM_ANGLES + NUM_ALIGNMENT  # = 36 + 5 = 41
MAX_FRAMES = 100

KEYPOINTS = [
    "left_wrist","left_elbow","left_shoulder",
    "left_ankle","left_knee","left_hip",
    "right_ankle","right_knee","right_hip",
    "right_wrist","right_elbow","right_shoulder"
]

MODEL_PATH = r"C:\Users\User\vitalmix-vscode\ml\form_check_lstm_model.h5"
model = tf.keras.models.load_model(MODEL_PATH)

mp_pose = mp.solutions.pose
pose = mp_pose.Pose()

def calculate_angle(a, b, c):
    # Check if input is missing or None
    if not all([a, b, c]) or any(v is None for v in (a.get("x"), a.get("y"), b.get("x"), b.get("y"), c.get("x"), c.get("y"))):
        return None  # Handle missing keypoints safely

    # Extract x, y coordinates
    a, b, c = np.array([a["x"], a["y"]]), np.array([b["x"], b["y"]]), np.array([c["x"], c["y"]]) 

    # Create vectors for ab, cb
    ab = a - b
    cb = c - b

    # Compute dot product and magnitudes of vectors
    dot_product = np.dot(ab, cb)
    magnitude_ab = np.linalg.norm(ab)
    magnitude_cb = np.linalg.norm(cb)

    # Prevent division by zero
    if magnitude_ab == 0 or magnitude_cb == 0:
        return None  

    # Compute the angle in degrees
    cosine_angle = np.clip(dot_product / (magnitude_ab * magnitude_cb), -1.0, 1.0)
    angle = np.arccos(cosine_angle)  # Convert to radians
    return float(np.degrees(angle))  # Convert to degrees

def extract_keypoints_from_video(video_path):
    
    #Returns shape (num_frames,41), 41=36(keypoints) +4(angles)+1(alignment)
    
    cap = cv2.VideoCapture(video_path)
    frames_data=[]
    raw_data={}
    fc=0

    while True:
        ret,frame=cap.read()
        if not ret:
            break
        fc+=1
        rgb=cv2.cvtColor(frame,cv2.COLOR_BGR2RGB)
        results=pose.process(rgb)
        if results.pose_landmarks:
            # build 36 features from keypoints
            kp_map={}
            for lbl in KEYPOINTS:
                idx=getattr(mp_pose.PoseLandmark,lbl.upper(),None)
                if idx is not None:
                    lm=results.pose_landmarks.landmark[idx]
                    kp_map[lbl]=[lm.x,lm.y,lm.visibility]
                else:
                    kp_map[lbl]=[0,0,0]
            frame_vec=[]
            # flatten
            for lbl in KEYPOINTS:
                frame_vec.extend(kp_map[lbl])  # each is 3

            # angles
            raw_kp={}
            for lbl in KEYPOINTS:
                raw_kp[lbl]={"x":kp_map[lbl][0],"y":kp_map[lbl][1]}
            lk=calculate_angle(raw_kp["left_ankle"], raw_kp["left_knee"], raw_kp["left_hip"])
            rk=calculate_angle(raw_kp["right_ankle"],raw_kp["right_knee"],raw_kp["right_hip"])
            le=calculate_angle(raw_kp["left_wrist"],raw_kp["left_elbow"],raw_kp["left_shoulder"])
            re=calculate_angle(raw_kp["right_wrist"],raw_kp["right_elbow"],raw_kp["right_shoulder"])

            ls=raw_kp["left_shoulder"]["x"]
            rs=raw_kp["right_shoulder"]["x"]
            align=abs(ls-rs) if (ls is not None and rs is not None) else 0

            frame_vec.append(lk)
            frame_vec.append(rk)
            frame_vec.append(le)
            frame_vec.append(re)
            frame_vec.append(align)

            if len(frame_vec)!=FEATURE_SIZE:
                raise ValueError(f"Frame {fc} => found {len(frame_vec)} features, expected {FEATURE_SIZE}")
            frames_data.append(frame_vec)

            # raw data for optional keypoint viz
            raw_data[f"frame_{fc}"]={}
            for lbl in KEYPOINTS:
                raw_data[f"frame_{fc}"][lbl]={"x":kp_map[lbl][0],"y":kp_map[lbl][1]}
        else:
            # no pose => fill 41 zeros
            frames_data.append([0]*FEATURE_SIZE)
            raw_data[f"frame_{fc}"]={}

    cap.release()
    frames_data=np.array(frames_data,dtype=np.float32)
    return frames_data,raw_data

def predict_accuracy(video_path,exercise,angle,save_keypoints=False):
    frames_data, raw_kps=extract_keypoints_from_video(video_path)
    if len(frames_data)==0:
        return {"error":"No frames data."}

    if save_keypoints:
        outj=video_path.replace(".mp4","_keypoints.json")
        with open(outj,"w") as f:
            json.dump(raw_kps,f,indent=4)
        print("Saved keypoints to",outj)

    # pad/trunc
    out=np.zeros((MAX_FRAMES,FEATURE_SIZE),dtype=np.float32)
    if frames_data.shape[0]>MAX_FRAMES:
        frames_data=frames_data[-MAX_FRAMES:]
    out[-frames_data.shape[0]:,:]=frames_data

    inp=np.expand_dims(out,axis=0) # (1,100,41)
    print("Input shape:",inp.shape)
    print("Model expects:",model.input_shape)

    pr=model.predict(inp)[0][0]
    sc=round(pr*100,2)
    fb=generate_feedback(sc,exercise,angle)
    return {"exercise":exercise,"angle":angle,"accuracy":sc,"feedback":fb}

def generate_feedback(score,exercise,angle):
    if score>=90:
        return "Excellent form!"
    elif score>=75:
        return "Good form, minor improvements possible."
    elif score>=50:
        return "Fair form. Work on alignment."
    else:
        return "Needs improvement. Focus on posture."

def visualize_keypoints(video_path, keyjson): 
    import cv2
    if not os.path.exists(keyjson):
        print("No keypoint json found.")
        return
    with open(keyjson, "r") as f:
        data = json.load(f)
    cap = cv2.VideoCapture(video_path)
    fc = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        fc += 1
        fk = f"frame_{fc}"
        if fk in data:
            for lbl, v in data[fk].items():
                if v.get("x") is not None and v.get("y") is not None:
                    x = int(v["x"] * frame.shape[1])
                    y = int(v["y"] * frame.shape[0])
                    cv2.circle(frame, (x, y), 5, (0, 255, 0), -1)
                    cv2.putText(frame, lbl, (x + 5, y - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
        
        # Resize the frame for a smaller display window
        frame_resized = cv2.resize(frame, (800, 500)) 

        cv2.imshow("Labeled", frame_resized)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
    cap.release()
    cv2.destroyAllWindows()

if __name__=="__main__":
    vid=r"C:\Users\User\exercise-dataset\data\squats\behind\newtest\newtest.mp4"
    ex="squat"
    ang="behind"
    out=predict_accuracy(vid,ex,ang,True)
    print(out)
    kj=vid.replace(".mp4","_keypoints.json")
    if os.path.exists(kj):
        visualize_keypoints(vid,kj)
