import os 
import json
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, BatchNormalization, Masking
from tensorflow.keras.callbacks import EarlyStopping
from sklearn.model_selection import train_test_split
from collections import Counter

# Configuration Constants 
KEYPOINT_NAMES_COMMON = {
    "squat": [
        "left_shoulder", "left_hip", "left_knee", "left_ankle", "left_heel",
        "right_shoulder", "right_hip", "right_knee", "right_ankle", "right_heel"
    ],
    "pushup": [
        "left_wrist", "left_elbow", "left_shoulder", "left_hip", "left_knee",
        "right_wrist", "right_elbow", "right_shoulder", "right_hip", "right_knee"
    ]
}

EXERCISE_LABELS = {
    "squat": 0,
    "pushup": 1
}

NUM_KEYPOINTS = 10
FEATURE_SIZE = NUM_KEYPOINTS * 3 + 1 + 1  # 3 values per keypoint + 1 angle + 1 exercise metadata
MAX_FRAMES = 100  # Sequence length for LSTM

SQUAT_DIR = r"C:\Users\User\exercise-dataset\data\squats"
PUSHUP_DIR = r"C:\Users\User\exercise-dataset\data\pushups"
MODEL_PATH = r"C:\Users\User\VitalMixFYP\Backend\vitalmix-vscode\ml\form_check_lstm_model_v3.h5"

# Pad or truncate each sequence to a fixed length for LSTM 
def pad_or_truncate(sequence_array, max_length, feature_size):
    if len(sequence_array) > max_length:
        # Keep the last max_length frames
        sequence_array = sequence_array[-max_length:]
    pad_len = max_length - len(sequence_array)
    if pad_len > 0:
        # Pad with zeros at the start if sequence is too short
        padding = np.zeros((pad_len, feature_size), dtype=np.float32)
        sequence_array = np.vstack((padding, sequence_array))
    return sequence_array

# Load and process data from all labeled JSON files 
def load_labeled_data(exercise_type, data_directory):
    input_sequences = []  # Hold all X sequences
    labels = []           # Hold corresponding Y labels

    keypoint_names = KEYPOINT_NAMES_COMMON[exercise_type]
    if exercise_type == "squat":
        angle_key = "knee_angle"
    elif exercise_type == "pushup":
        angle_key = "left_elbow_angle"

    exercise_label_value = EXERCISE_LABELS[exercise_type]

    for label_value, category in enumerate(["correct", "incorrect"]):
        category_path = os.path.join(data_directory, category)
        if not os.path.exists(category_path):
            continue

        for filename in os.listdir(category_path):
            if not filename.endswith(".json"):
                continue

            filepath = os.path.join(category_path, filename)
            with open(filepath, "r") as file:
                data = json.load(file)

            frames = data.get("frames", [])
            reps = data.get("reps", [])

            # Skip files with no reps
            if not reps:
                continue

            # Process each rep range
            for rep in reps:
                start_idx = rep["start_frame"]
                end_idx = rep["end_frame"]
                frame_vectors = []

                for i in range(start_idx, end_idx + 1):
                    if i >= len(frames):
                        continue  # Avoid index errors

                    frame = frames[i]
                    keypoints = frame.get("keypoints", {})
                    angle_value = frame.get(angle_key, 0)

                    frame_features = []

                    for key in keypoint_names:
                        point = keypoints.get(key, [0, 0, 0])  # x, y, visibility
                        if point is None or len(point) != 3:
                            frame_features.extend([0, 0, 0])
                        else:
                            frame_features.extend([
                                point[0] if point[0] is not None else 0,
                                point[1] if point[1] is not None else 0,
                                point[2] if point[2] is not None else 0
                            ])

                    # Add angle
                    if angle_value is None:
                        frame_features.append(0)
                    else:
                        frame_features.append(angle_value)

                    # Add exercise type label
                    frame_features.append(exercise_label_value)

                    frame_vectors.append(frame_features)

                # Skip if this rep has no valid frames
                if not frame_vectors:
                    continue

                # Ensure sequence is fixed length for LSTM
                sequence_array = np.array(frame_vectors, dtype=np.float32)
                sequence_array = pad_or_truncate(sequence_array, MAX_FRAMES, FEATURE_SIZE)

                input_sequences.append(sequence_array)
                labels.append(label_value)

    return np.array(input_sequences, dtype=np.float32), np.array(labels, dtype=np.float32)

#  Load the full dataset 
X_squat, y_squat = load_labeled_data("squat", SQUAT_DIR)
X_pushup, y_pushup = load_labeled_data("pushup", PUSHUP_DIR)

X = np.vstack((X_squat, X_pushup))
y = np.hstack((y_squat, y_pushup))

print("Loaded sequences:", X.shape, "| Labels:", y.shape)

#  Split into training and testing sets 
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.1, random_state=42)
print("Train size:", len(X_train), "| Test size:", len(X_test))

#  balance correct & incorrect examples 
label_counts = Counter(y_train)
total_samples = len(y_train)
class_weights = {cls: total_samples / (len(label_counts) * count) for cls, count in label_counts.items()}
print("Class Weights:", class_weights)

#  Define the LSTM model 
def create_lstm_model():

    #Returns a compiled LSTM model for binary classification of form correctness

    model = Sequential([
        # Ignore padded frames
        Masking(mask_value=0.0, input_shape=(MAX_FRAMES, FEATURE_SIZE)),

        # First LSTM layer to learn temporal patterns
        LSTM(128, return_sequences=True, activation='tanh'),
        BatchNormalization(),
        Dropout(0.3),

        # Second LSTM layer for deeper temporal learning
        LSTM(64, activation='tanh'),
        BatchNormalization(),
        Dropout(0.3),

        # Dense layer to compress learned representation
        Dense(32, activation='relu'),

        # 1 neuron for binary classification (correct vs incorrect)
        Dense(1, activation='sigmoid')
    ])

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.0005), # slow learning rate
        loss='binary_crossentropy',
        metrics=['accuracy']
    )

    return model

#  Create and train the model 
model = create_lstm_model()
print("Model ready. Input shape:", model.input_shape)

early_stopping = EarlyStopping(
    monitor='val_loss',      # Watch validation loss
    patience=50,              # Stop if no improvement for 5 epochs
    restore_best_weights=True  # Restore the best model
)

history = model.fit(
    X_train, y_train,
    epochs=85,
    batch_size=32,
    validation_data=(X_test, y_test),
    class_weight=class_weights,
    callbacks=[early_stopping]
)

#  Save the trained model 
os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
model.save(MODEL_PATH)
print("Model saved to:", MODEL_PATH)