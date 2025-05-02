import cv2
import json

def visualize_points(video_path, labeled_points_path):
    # Load the labeled points
    with open(labeled_points_path, "r") as f:
        labeled_data = json.load(f)

    # Open the video file
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print("Error: Unable to open video.")
        return

    # Loop through frames in the video
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # Get the current frame number
        frame_idx = int(cap.get(cv2.CAP_PROP_POS_FRAMES))

        # Overlay labeled points if they exist for this frame
        if frame_idx < len(labeled_data["frames"]):
            keypoints = labeled_data["frames"][frame_idx]["keypoints"]
            

            for label, (x_norm, y_norm, _) in keypoints.items():
                x = int(x_norm * frame.shape[1])
                y = int(y_norm * frame.shape[0])

                # draw point and label
                cv2.circle(frame, (x, y), 5, (0, 255, 0), -1)
                cv2.putText(frame, label, (x + 5, y - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
                cv2.putText(frame, f"Frame: {frame_idx}", (20, 40), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 255), 2)

        # Display the frame
        resized_frame = cv2.resize(frame, (700, 900))  # Adjust width and height as needed
        cv2.imshow("Labeled Video", resized_frame)


        # Quit if q is pressed
        if cv2.waitKey(30) & 0xFF == ord('q'):  # Adjust delay for speed
            break

    cap.release()
    cv2.destroyAllWindows()

# video and points paths
video_path = r"C:\Users\User\exercise-dataset\data\pushups\incorrect\badpushup_example18.mp4"
labeled_points_path = r"C:\Users\User\exercise-dataset\data\pushups\incorrect\badpushup_example18_labelled_points.json"

# Call the function
visualize_points(video_path, labeled_points_path)