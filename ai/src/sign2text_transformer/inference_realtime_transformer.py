import cv2
import torch
import numpy as np
import mediapipe as mp
from model import KSLTransformer
import json
from PIL import Image, ImageDraw, ImageFont
from collections import deque

# collect_data.py와 동일한 설정
INPUT_DIM = 141
MAX_LEN = 30
POSE_IDXS = [0, 11, 12, 13, 14] # 코, 양어깨, 양팔꿈치

def normalize_landmarks(features):
    """dataset.py와 동일한 141차원 정규화 로직"""
    pts = features.reshape(-1, 3)
    # 141차원 기준 어깨 인덱스 (43, 44)
    l_shoulder = pts[43]
    r_shoulder = pts[44]
    
    shoulder_center = (l_shoulder + r_shoulder) / 2.0
    pts_translated = pts - shoulder_center
    
    shoulder_width = np.linalg.norm(l_shoulder - r_shoulder)
    if shoulder_width < 1e-6:
        shoulder_width = 1.0
        
    pts_scaled = pts_translated / shoulder_width
    return pts_scaled.flatten()

def extract_keypoints(results, last_pose):
    """collect_data.py와 100% 동일한 순서와 규격으로 추출 (Forward Fill 적용)"""
    features = []
    
    # 1. Left Hand (63)
    if results.left_hand_landmarks:
        for lm in results.left_hand_landmarks.landmark:
            features.extend([lm.x, lm.y, lm.z])
    else:
        features.extend([0.0] * 21 * 3)

    # 2. Right Hand (63)
    if results.right_hand_landmarks:
        for lm in results.right_hand_landmarks.landmark:
            features.extend([lm.x, lm.y, lm.z])
    else:
        features.extend([0.0] * 21 * 3)

    # 3. Pose (15) - Forward Fill
    if results.pose_landmarks:
        pose_landmarks = results.pose_landmarks.landmark
        current_pose = []
        for idx in POSE_IDXS:
            lm = pose_landmarks[idx]
            current_pose.extend([lm.x, lm.y, lm.z])
        features.extend(current_pose)
        last_pose = current_pose # 업데이트
    else:
        if last_pose is not None:
            features.extend(last_pose)
        else:
            features.extend([0.0] * len(POSE_IDXS) * 3)

    # 추출된 생좌표를 즉시 정규화해서 반환
    raw_features = np.array(features, dtype=np.float32)
    return normalize_landmarks(raw_features), last_pose

def main():
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    
    # 모델 및 라벨 로드
    try:
        with open('models/new_1_best_model_9873/label_map_1.json', 'r', encoding='utf-8') as f:
            label_map = {int(k): v for k, v in json.load(f).items()}
        
        model = KSLTransformer(input_dim=INPUT_DIM, num_classes=len(label_map), d_model=128, num_heads=8, num_layers=3).to(device)
        # 141차원 전용으로 새로 학습된 모델 파일을 로드해야 함
        model.load_state_dict(torch.load('models/new_1_best_model_9873/best_sign_model_1.pt', map_location=device))
        model.eval()
        print("✅ KSL Transformer V1 (141-dim) 모델 로드 성공!")
    except Exception as e:
        print(f"❌ 모델 로드 실패: {e}")
        return

    mp_holistic = mp.solutions.holistic
    mp_drawing = mp.solutions.drawing_utils
    cap = cv2.VideoCapture(0)
    
    frame_window = deque(maxlen=MAX_LEN)
    prediction_window = deque(maxlen=5)
    current_action = "none"
    last_pose = None # Forward Fill용 변수

    with mp_holistic.Holistic(min_detection_confidence=0.5, min_tracking_confidence=0.5) as holistic:
        while cap.isOpened():
            success, frame = cap.read()
            if not success: break
            
            # 1. AI 분석용 (원본)
            image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = holistic.process(image_rgb)
            
            # 141차원 키포인트 추출 (Forward Fill 적용)
            keypoints, last_pose = extract_keypoints(results, last_pose)
            frame_window.append(keypoints)
            
            if len(frame_window) == MAX_LEN:
                input_tensor = torch.FloatTensor(np.array(frame_window)).unsqueeze(0).to(device)
                with torch.no_grad():
                    logits = model(input_tensor)
                    probs = torch.softmax(logits, dim=1)
                    conf, idx = torch.max(probs, 1)
                    
                    # 신뢰도가 0.8 이상일 때만 투표 후보로 등록
                    if conf.item() > 0.8:
                        prediction_window.append(label_map[idx.item()])
                    else:
                        prediction_window.append("none")
                
                # [투표 로직] 윈도우 내에서 가장 빈도가 높은 단어를 현재 액션으로 결정
                if len(prediction_window) > 0:
                    current_action = max(set(prediction_window), key=list(prediction_window).count)
                else:
                    current_action = "none"

            # --- 시각화 (사용자를 위한 거울 모드) ---
            # 뼈대 그리기
            mp_drawing.draw_landmarks(frame, results.pose_landmarks, mp_holistic.POSE_CONNECTIONS)
            mp_drawing.draw_landmarks(frame, results.left_hand_landmarks, mp_holistic.HAND_CONNECTIONS)
            mp_drawing.draw_landmarks(frame, results.right_hand_landmarks, mp_holistic.HAND_CONNECTIONS)
            
            # 화면 뒤집기
            display_frame = cv2.flip(frame, 1)
            
            # 글씨 쓰기
            img_pil = Image.fromarray(cv2.cvtColor(display_frame, cv2.COLOR_BGR2RGB))
            draw = ImageDraw.Draw(img_pil)
            try:
                font = ImageFont.truetype("malgun.ttf", 35)
                draw.text((30, 30), f"인식 결과: {current_action}", font=font, fill=(0, 255, 0))
            except: pass
            
            final_frame = cv2.cvtColor(np.array(img_pil), cv2.COLOR_RGB2BGR)
            cv2.imshow('KSL V1 141-dim Real-time', final_frame)
            
            if cv2.waitKey(1) & 0xFF == ord('q'): break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()
