import cv2
import os

if __name__ == "__main__":

    # -------- SETTINGS -------- #
    image_folder = r"C:\Users\justi\Documents\omr images\raw images\testingOutput"  # folder with frames
    output_video = r"C:\Users\justi\Documents\omr images\raw images\video.mp4"  # output video file
    fps = 24  # frames per second
    # -------------------------- #

    # Get all image files sorted by name
    images = [img for img in os.listdir(image_folder)
              if img.lower().endswith(".png")]

    images.sort()  # IMPORTANT: keeps frame order correct

    if not images:
        raise RuntimeError("No images found in folder.")

    # Read first image to get size
    first_frame = cv2.imread(os.path.join(image_folder, images[0]))
    height, width, _ = first_frame.shape

    # Create video writer
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")  # for .mp4
    video = cv2.VideoWriter(output_video, fourcc, fps, (width, height))

    # Write each frame
    for image in images:
        path = os.path.join(image_folder, image)
        frame = cv2.imread(path)

        # Safety check (all frames must be same size)
        if frame.shape[:2] != (height, width):
            frame = cv2.resize(frame, (width, height))

        video.write(frame)

    # Release file
    video.release()
    print("Video saved as:", output_video)