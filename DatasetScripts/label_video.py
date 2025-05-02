import cv2
import matplotlib.pyplot as plt
import json
from tkinter import Tk, simpledialog

# label point when chosen
def prompt_for_label(x, y):
    root = Tk()
    root.withdraw()
    label = simpledialog.askstring("Input", f"Label for point at ({x:.2f}, {y:.2f}):") # dialog box
    root.destroy() # clean up
    return label

#display and label frame function
def label_frame(video_path, frame_num=0):
    cap = cv2.VideoCapture(video_path) # open video
    cap.set(cv2.CAP_PROP_POS_FRAMES, frame_num) #go to frame
    ret, frame = cap.read()
    if not ret:
        print("Error reading frame.")
        return

#display frame 
    plt.imshow(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
    plt.title(f"Label Frame {frame_num}")
    points = []

#onclick to handle labels
    def onclick(event):
        if event.xdata and event.ydata:
            label = prompt_for_label(event.xdata, event.ydata)
            if label:
                #add point to list and display on matplotlib
                points.append({"x": event.xdata, "y": event.ydata, "label": label})
                plt.scatter(event.xdata, event.ydata, color='red')
                plt.text(event.xdata, event.ydata, label, color='blue')
                plt.draw()

    cid = plt.gcf().canvas.mpl_connect("button_press_event", onclick)
    plt.show()

    cap.release()
    return points

# Save the labeled points
video_path = r"C:\Users\User\exercise-dataset\data\pushups\incorrect\badpushup_example18.mp4"
output_path = r"C:\Users\User\exercise-dataset\data\pushups\incorrect\badpushup_example18_labelled_points.json"
points = label_frame(video_path)

# Format output
points_format = {p["label"]: [p["x"], p["y"]] for p in points}

with open(output_path, "w") as f:
    json.dump(points_format, f)
print(f"Saved points to {output_path}")