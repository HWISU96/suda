import os
import glob
import re
import numpy as np
import torch
from torch.utils.data import Dataset, DataLoader
from sklearn.model_selection import train_test_split

class KSLDataset(Dataset):
    def __init__(self, data_list, labels, max_len=30, is_train=False, none_idx=None):
        """
        data_list: .npy 파일 경로들의 리스트
        labels: 파일 경로에 대응하는 정수형 라벨(클래스 인덱스) 리스트
        max_len: 고정할 프레임 수 (기본 30)
        is_train: 데이터 증강을 적용할지 여부
        none_idx: 'none' 클래스의 인덱스 (정지 상태 증강에 사용)
        """
        self.data_list = data_list
        self.labels = labels
        self.max_len = max_len
        self.is_train = is_train
        self.none_idx = none_idx

    def __len__(self):
        return len(self.data_list)

    def __getitem__(self, idx):
        # 1. npy 파일 로드 (형태: [frames, 141])
        npy_path = self.data_list[idx]
        data = np.load(npy_path)
        
        # 2. 전처리 (정규화 - 모바일 스펙과 동일하게 적용)
        data = self.normalize_landmarks(data)

        # 3. 프레임 길이 맞추기 (Uniform Sampling / Padding)
        data = self.adjust_sequence_length(data)
        
        label = self.labels[idx]
        
        # 4. 일반적인 공간/시간 데이터 증강 (Train 데이터에만 적용)
        if self.is_train:
            data = self.augment_data(data)
        
        # FloatTensor로 변환 시, NumPy와 PyTorch의 메모리 공유로 인한 Multiprocessing 충돌 원천 차단 (강제 copy)
        data = np.array(data, dtype=np.float32).copy()
        
        # shape가 (30, 141)이 아니면 0으로 채움 (배치 사이즈 불일치 원천 차단)
        if data.shape != (self.max_len, 141):
            data = np.zeros((self.max_len, 141), dtype=np.float32)
            
        return torch.from_numpy(data).float(), torch.tensor(label, dtype=torch.long)

    def normalize_landmarks(self, data):
        """
        모바일 스펙 2-1에 명시된 정규화(Translation & Scale Invariance) 적용
        data shape: (frames, 141)
        - 141 = LH(63) + RH(63) + Pose(15)
        - 어깨 좌표 인덱스: Pose의 11, 12번 (141차원 벡터 내 위치: 126+3, 126+6 ...)
        """
        normalized_data = np.zeros_like(data)
        
        for i in range(data.shape[0]): # 매 프레임마다 반복
            frame = data[i]
            
            # x, y, z 좌표를 (47, 3) 형태로 변환하여 계산하기 쉽게 함 (LH 21 + RH 21 + Pose 5 = 47)
            pts = frame.reshape(-1, 3)
            
            # 141차원 벡터 내 어깨 위치: LH(21) + RH(21) + Nose(1) 다음이 어깨
            # pts[43] = Left Shoulder, pts[44] = Right Shoulder
            l_shoulder = pts[43]
            r_shoulder = pts[44]
            
            # 1단계: 위치 통일 (어깨 중심점을 0,0,0으로)
            shoulder_center = (l_shoulder + r_shoulder) / 2.0
            pts_translated = pts - shoulder_center
            
            # 2단계: 거리/크기 통일 (어깨 너비를 1.0으로)
            shoulder_width = np.linalg.norm(l_shoulder - r_shoulder)
            
            # 0으로 나누는 에러 방지
            if shoulder_width < 1e-6:
                shoulder_width = 1.0
                
            pts_scaled = pts_translated / shoulder_width
            
            # 다시 (141,) 1차원 배열로 복구
            normalized_data[i] = pts_scaled.flatten()
            
        return normalized_data

    def adjust_sequence_length(self, data):
        """
        프레임 수를 무조건 self.max_len(30)으로 맞춤
        길면 Uniform Sampling (간격 두고 추출), 짧으면 마지막 프레임으로 Forward Fill 패딩
        """
        frames = data.shape[0] if len(data.shape) > 0 else 0
        
        # 1. 추출 실패로 인해 비어 있거나 형태가 깨진 데이터 방어
        if frames == 0 or len(data.shape) < 2 or data.shape[1] != 141:
            return np.zeros((self.max_len, 141), dtype=np.float32)
            
        if frames == self.max_len:
            pass
        elif frames > self.max_len:
            # 길면 균등하게 30개를 뽑아냄 (방식 A)
            indices = np.linspace(0, frames - 1, self.max_len, dtype=int)
            data = data[indices]
        else:
            # 짧으면 마지막 프레임을 복사해서 채워 넣음
            pad_len = self.max_len - frames
            last_frame = data[-1:]
            padding = np.repeat(last_frame, pad_len, axis=0)
            data = np.vstack((data, padding))
            
        # 2. PyTorch DataLoader 호환성을 위해 새 배열로 래핑하여 리턴
        return np.array(data, dtype=np.float32)
            
    def augment_data(self, data):
        """
        3D 좌표 데이터의 본질을 훼손하지 않는 3가지 안전한 증강 기법
        """
        augmented = data.copy()
        
        # 1. Spatial Jittering (50% 확률로 랜드마크 추출 오차 및 손떨림 모방)
        if np.random.rand() < 0.5:
            # 숄더 너비 정규화 기준값에서 0.02 정도의 미세 노이즈
            noise = np.random.normal(loc=0.0, scale=0.02, size=augmented.shape)
            augmented += noise
            
        # 2. Random Scaling (50% 확률로 체형, 팔 길이 다양성 모방)
        if np.random.rand() < 0.5:
            scale_factor = np.random.uniform(0.9, 1.1) # 0.9배 ~ 1.1배
            augmented *= scale_factor
            
        # 3. Temporal Masking (50% 확률로 순간적인 화면 이탈 모방)
        if np.random.rand() < 0.5:
            num_masks = np.random.randint(1, 3) # 1개 또는 2개 프레임 무작위 삭제
            mask_indices = np.random.choice(augmented.shape[0], num_masks, replace=False)
            augmented[mask_indices] = 0.0 # 프레임의 모든 좌표를 0으로
            
        return augmented

def clean_class_name(cls_name):
    """
    폴더명에서 숫자를 제거하여 통합된 클래스 이름 반환 ('걷다1', '걷다2' -> '걷다')
    """
    return re.sub(r'\d+', '', cls_name).strip()

def create_dataloaders(data_dir, batch_size=32, max_len=30, max_classes=None):
    """
    폴더 구조(단어별 폴더)를 읽어서 Train/Val 80:20 분리 후 DataLoader 반환
    """
    raw_classes = sorted([d for d in os.listdir(data_dir) if os.path.isdir(os.path.join(data_dir, d)) and not d.startswith('.')])
    
    # 숫자 꼬리표 떼고 순수 단어들만 모으기
    unique_classes = set()
    for rc in raw_classes:
        unique_classes.add(clean_class_name(rc))
        
    sorted_unique_classes = sorted(list(unique_classes))
    
    # 30개 등 특정 개수만 먼저 뽑고 싶을 경우
    if max_classes is not None:
        sorted_unique_classes = sorted_unique_classes[:max_classes]
        
    class_to_idx = {cls_name: i for i, cls_name in enumerate(sorted_unique_classes)}
    
    all_files = []
    all_labels = []
    
    # 데이터 수집 (걷다1, 걷다2 폴더의 영상들이 모두 '걷다' 인덱스로 뭉쳐짐)
    for rc in raw_classes:
        clean_name = clean_class_name(rc)
        
        # 제한된 클래스 목록에 없으면 스킵
        if clean_name not in class_to_idx:
            continue
            
        cls_idx = class_to_idx[clean_name]
        cls_dir = os.path.join(data_dir, rc)
        
        files = glob.glob(os.path.join(cls_dir, '*.npy'))
        
        # [데이터 불균형 해결] none 클래스의 파일이 너무 적다면, 다른 단어 평균 개수(약 100개)에 맞게 강제 복제(Oversampling)
        if clean_name == 'none' and 0 < len(files) < 100:
            multiplier = max(1, 100 // len(files))
            files = files * multiplier
            
        all_files.extend(files)
        all_labels.extend([cls_idx] * len(files))
        
    print(f"📊 정제된 총 클래스 수: {len(sorted_unique_classes)}개 (원본 폴더 수: {len(raw_classes)}개)")
    
    # Class Weight 계산 (데이터 불균형 극복)
    label_counts = np.bincount(all_labels)
    total_samples = len(all_labels)
    num_classes = len(class_to_idx)
    # 데이터가 적은 클래스(예: none)에 엄청난 가중치가 부여됨
    class_weights = total_samples / (num_classes * (label_counts + 1e-6))
    class_weights_tensor = torch.FloatTensor(class_weights)
    
    # none 클래스 식별 (증강용)
    none_idx = class_to_idx.get('none', None)
    
    # Stratified Split (단어별 비율을 유지하면서 80:20으로 나눔)
    X_train, X_val, y_train, y_val = train_test_split(
        all_files, all_labels, 
        test_size=0.2, 
        random_state=42, 
        stratify=all_labels # 핵심: 모든 단어가 고르게 80:20으로 찢어지도록 보장
    )
    
    train_dataset = KSLDataset(X_train, y_train, max_len, is_train=True, none_idx=none_idx)
    val_dataset = KSLDataset(X_val, y_val, max_len, is_train=False, none_idx=none_idx)
    
    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True, num_workers=4)
    val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False, num_workers=4)
    
    return train_loader, val_loader, class_to_idx, class_weights_tensor
