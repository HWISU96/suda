import boto3
import cv2
import mediapipe as mp
import numpy as np
import os
import tempfile
import time
from concurrent.futures import ProcessPoolExecutor, as_completed

AWS_ACCESS_KEY = os.environ.get('AWS_ACCESS_KEY')
AWS_SECRET_KEY = os.environ.get('AWS_SECRET_KEY')
BUCKET_NAME = os.environ.get('S3_BUCKET_NAME')
S3_SOURCE_PREFIX = 'raw_videos/'
S3_TARGET_PREFIX = 'processed_npy_v2/'

POSE_IDXS = [0, 11, 12, 13, 14]

def extract_keypoints(results, last_pose):
    if results.left_hand_landmarks:
        lh = np.array([[lm.x, lm.y, lm.z] for lm in results.left_hand_landmarks.landmark]).flatten()
    else:
        lh = np.zeros(21 * 3)

    if results.right_hand_landmarks:
        rh = np.array([[lm.x, lm.y, lm.z] for lm in results.right_hand_landmarks.landmark]).flatten()
    else:
        rh = np.zeros(21 * 3)

    if results.pose_landmarks:
        pose_landmarks = results.pose_landmarks.landmark
        pose = np.array([[pose_landmarks[idx].x, pose_landmarks[idx].y, pose_landmarks[idx].z] for idx in POSE_IDXS]).flatten()
        last_pose = pose
    else:
        pose = last_pose if last_pose is not None else np.zeros(len(POSE_IDXS) * 3)

    return np.concatenate([lh, rh, pose]), last_pose

def process_single_video(s3_key):
    try:
        s3_client = boto3.client('s3', aws_access_key_id=AWS_ACCESS_KEY, aws_secret_access_key=AWS_SECRET_KEY)
        
        relative_path = s3_key.replace(S3_SOURCE_PREFIX, '')
        target_key = os.path.join(S3_TARGET_PREFIX, os.path.splitext(relative_path)[0] + '.npy').replace('\\', '/')
        
        try:
            s3_client.head_object(Bucket=BUCKET_NAME, Key=target_key)
            return f"⏭️ 스킵 (이미 존재): {target_key}"
        except:
            pass
            
        with tempfile.NamedTemporaryFile(suffix='.mp4', delete=False) as tmp_file:
            tmp_path = tmp_file.name
            s3_client.download_file(Bucket=BUCKET_NAME, Key=s3_key, Filename=tmp_path)

        video_data = []
        cap = cv2.VideoCapture(tmp_path)
        last_pose = None
        
        mp_holistic = mp.solutions.holistic
        with mp_holistic.Holistic(static_image_mode=False, min_detection_confidence=0.5) as holistic:
            while cap.isOpened():
                success, frame = cap.read()
                if not success: break
                image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                results = holistic.process(image_rgb)
                keypoints, last_pose = extract_keypoints(results, last_pose)
                video_data.append(keypoints)
        
        cap.release()
        os.remove(tmp_path)

        if len(video_data) > 0 and last_pose is not None:
            npy_data = np.array(video_data, dtype=np.float32)
            with tempfile.NamedTemporaryFile(suffix='.npy', delete=False) as tmp_npy:
                np.save(tmp_npy.name, npy_data)
                s3_client.upload_file(tmp_npy.name, BUCKET_NAME, target_key)
            os.remove(tmp_npy.name)
            return f"✅ 완료: {target_key}"
        return f"⚠️ 실패 (데이터 없음): {s3_key}"
    
    except Exception as e:
        return f"❌ 에러 ({s3_key}): {str(e)}"

def main():
    s3_client = boto3.client('s3', aws_access_key_id=AWS_ACCESS_KEY, aws_secret_access_key=AWS_SECRET_KEY)
    
    paginator = s3_client.get_paginator('list_objects_v2')
    video_keys = []
    for page in paginator.paginate(Bucket=BUCKET_NAME, Prefix=S3_SOURCE_PREFIX):
        if 'Contents' in page:
            video_keys.extend([obj['Key'] for obj in page['Contents'] if obj['Key'].endswith('.mp4')])

    print(f"🚀 총 {len(video_keys)}개의 영상 작업을 시작합니다. (안정화 병렬 모드)")

    with ProcessPoolExecutor(max_workers=4) as executor:
        future_to_video = {executor.submit(process_single_video, key): key for key in video_keys}
        
        for future in as_completed(future_to_video):
            result = future.result()
            print(result)
            time.sleep(0.05)

if __name__ == "__main__":
    main()
