import os
import boto3
import concurrent.futures
from dotenv import load_dotenv

load_dotenv()
AWS_ACCESS_KEY = os.getenv('AWS_ACCESS_KEY')
AWS_SECRET_KEY = os.getenv('AWS_SECRET_KEY')
BUCKET_NAME = os.getenv('S3_BUCKET_NAME')

TARGET_DIR = './s3_downloaded_data'

def download_single_file(s3_key):
    s3_client = boto3.client(
        's3',
        aws_access_key_id=AWS_ACCESS_KEY,
        aws_secret_access_key=AWS_SECRET_KEY
    )
    
    relative_path = s3_key.replace('processed_npy/', '')
    local_path = os.path.join(TARGET_DIR, relative_path)
    
    os.makedirs(os.path.dirname(local_path), exist_ok=True)
    
    try:
        s3_client.download_file(BUCKET_NAME, s3_key, local_path)
        return f"📥 다운로드 완료: {local_path}"
    except Exception as e:
        return f"❌ 실패 ({s3_key}): {e}"

def download_all_processed_npy():
    s3_client = boto3.client(
        's3',
        aws_access_key_id=AWS_ACCESS_KEY,
        aws_secret_access_key=AWS_SECRET_KEY
    )
    
    print(f"📂 S3 '{BUCKET_NAME}'에서 141차원 추출 데이터를 찾는 중...")
    target_keys = []
    paginator = s3_client.get_paginator('list_objects_v2')
    
    for page in paginator.paginate(Bucket=BUCKET_NAME, Prefix='processed_npy/'):
        if 'Contents' in page:
            for obj in page['Contents']:
                if obj['Key'].endswith('.npy'):
                    target_keys.append(obj['Key'])
    
    if not target_keys:
        print("⚠️ 다운로드할 .npy 파일이 없습니다. 추출기가 아직 실행 중인지 확인하세요.")
        return

    print(f"🚀 총 {len(target_keys)}개 파일 병렬 다운로드 시작...")
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        results = list(executor.map(download_single_file, target_keys))
    
    for res in results[:10]: print(res)
    print(f"✅ 다운로드 완료! 총 {len(target_keys)}개 파일을 {TARGET_DIR}에 저장했습니다.")

if __name__ == "__main__":
    download_all_processed_npy()
