<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,18,20&height=200&section=header&text=SUDA&fontSize=64&fontColor=ffffff&animation=fadeIn&fontAlignY=34&desc=On-Device%20Sign%20Language%20Communication%20Platform&descSize=18&descAlignY=56" width="100%"/>

### 농인 부모와 청인 자녀의 대화를 잇는 온디바이스 수어 소통 서비스

[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)](#기술-스택)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](#기술-스택)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](#기술-스택)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white)](#기술-스택)
[![LiteRT](https://img.shields.io/badge/LiteRT-FF6F00?style=flat-square&logo=tensorflow&logoColor=white)](#기술-스택)

</div>

---

## 프로젝트 소개

**SUDA(수다)** 는 농인 부모와 청인 자녀(CODA)가 일상에서 더 자연스럽게 대화할 수 있도록 돕는 Android 기반 양방향 소통 서비스입니다.

- 부모의 수어를 인식해 **자막과 음성**으로 전달합니다.
- 자녀의 음성을 인식해 **원문과 문맥 보정문**으로 제공합니다.
- 수어 영상은 서버로 계속 전송하지 않고 기기에서 처리해 프라이버시와 응답성을 고려했습니다.
- 네트워크가 불안정해도 대화를 이어갈 수 있도록 온디바이스 번역과 시스템 TTS 폴백을 제공합니다.

> SSAFY 14기 A404팀 · 6인 팀 프로젝트
>
> Android · Spring Boot · On-Device AI · FastAPI

---

## 핵심 사용자 흐름

### 수어 → 음성

```text
카메라 프레임
  → MediaPipe 랜드마크 추출
  → LiteRT/TFLite 수어 추론
  → 온디바이스 또는 서버 문장 변환
  → 화면 자막과 TTS 음성 출력
```

### 음성 → 텍스트

```text
마이크 입력
  → Android WAV 녹음
  → 로컬 STT 또는 CLOVA STT
  → Gemini 문맥·발음 보정
  → 원문과 보정문 자막 표시
```

---

## 주요 기능

| 기능 | 설명 |
|:--|:--|
| **온디바이스 수어 인식** | CameraX 프레임에서 MediaPipe 랜드마크를 추출하고 LiteRT/TFLite 모델로 수어를 추론합니다. |
| **양방향 대화** | 수어는 음성으로, 자녀의 음성은 텍스트로 변환해 하나의 대화 화면에 표시합니다. |
| **온·오프라인 전환** | 네트워크 상태와 사용자 설정에 따라 서버 또는 온디바이스 번역 경로를 선택합니다. |
| **보호자·자녀 관리** | 로그인, 네이버 OAuth, 자녀 프로필 생성·수정·선택 기능을 제공합니다. |
| **학습·음성 퀴즈** | 카테고리별 수어 학습과 WAV 음성 답변 기반 퀴즈를 제공합니다. |
| **학습·소통 리포트** | 취약 단어, 학습 진도, 퀴즈 기록과 자녀 발화 분석을 제공합니다. |

---

## 김휘수 담당 영역

팀에서 **Android 개발**을 담당했습니다. Git 이력 기준 165개 커밋과 GitLab MR 아카이브 기준 71개 MR에 기여했습니다.

### Android 기반 구조

- Hilt 기반 의존성 주입과 앱 진입점 구성
- Retrofit·OkHttp 네트워크 모듈
- Jetpack Compose Navigation
- 카메라·마이크 권한과 대화 세션 제어
- 공통 UI 컴포넌트와 Ktlint·Detekt 환경

### 인증과 사용자 흐름

- Android Keystore 기반 Access·Refresh Token 저장
- Interceptor 인증 헤더와 Authenticator 토큰 갱신
- 동시 401 응답의 중복 Refresh를 막는 `Mutex` 적용
- 로그인·회원가입·세션 복원·로그아웃
- 네이버 Android SDK 로그인
- 자녀 프로필 생성·조회·수정·선택

### 음성·대화 통합

- Android STT와 CLOVA STT 연동
- TTS 재생 중 STT를 중단하는 에코 방지
- WAV 녹음, 헤더 검증과 multipart 업로드
- 네트워크 장애 시 온디바이스 번역·TTS 폴백
- 대화 세션 생성·메시지 저장·종료 API 연동

### 학습·퀴즈·리포트

- 학습 카테고리와 단어 API 연동
- 서버 기반 퀴즈 세션·채점 흐름
- 음성 답변 multipart 제출
- 취약 단어·진행도·퀴즈 기록 화면
- 소통 발화 분석 리포트 연동

> Backend와 AI 모델은 팀원의 주요 담당 영역입니다. Android에서 각 시스템의 API 계약을 연결하고 실제 사용자 흐름으로 통합했습니다.

---

## 핵심 문제 해결

### 1. 동시 토큰 갱신 제어

여러 API가 동시에 401을 반환하면 Refresh 요청이 중복되거나 서로 다른 토큰이 저장될 수 있습니다.

```text
401 응답
  → Mutex로 Refresh 구간 직렬화
  → 다른 요청이 갱신한 토큰이 있으면 재사용
  → 직접 갱신이 필요하면 Refresh API 호출
  → 성공 시 기존 요청 재시도
  → 실패 시 인증·활성 자녀 세션 정리
```

`priorResponse` 체인으로 재시도 횟수를 제한해 인증 요청의 무한 반복도 방지했습니다.

관련 코드: [TokenAuthenticator.kt](mobile/app/src/main/java/com/ssafy/mobile/core/network/TokenAuthenticator.kt)

### 2. TTS 음성의 STT 재인식 방지

앱이 출력한 TTS를 마이크가 사용자 발화로 다시 인식하면 대화가 반복됩니다. TTS 재생 전에 STT를 일시 중지하고, 재생 완료 후 현재 세션 상태를 확인해 다시 시작하도록 구성했습니다.

관련 코드: [ConversationViewModel.kt](mobile/app/src/main/java/com/ssafy/mobile/feature/conversation/presentation/ConversationViewModel.kt)

### 3. 오프라인에서도 유지되는 대화

서버 장애를 오류 화면으로만 처리하지 않고 번역 모드별 정책을 적용했습니다.

| 모드 | 동작 |
|:--|:--|
| `AUTO` | 서버를 우선 사용하고 실패하거나 오프라인이면 온디바이스로 전환 |
| `SERVER` | 서버만 사용하며 연결할 수 없으면 명확한 오류 안내 |
| `ON_DEVICE` | 네트워크와 관계없이 기기 내부에서 처리 |

네트워크 전환 시 진행 중인 STT를 정리하고 새 경로로 다시 시작해 세션 상태가 섞이지 않도록 했습니다.

### 4. 오디오 API 계약 안정화

파일 존재 여부만 확인하지 않고 RIFF·WAVE magic header와 payload 크기를 검사했습니다. 잘못된 녹음 파일을 서버 전송 전에 차단하고, 실제 백엔드 계약에 맞춰 WAV 파일을 `multipart/form-data`로 전달했습니다.

관련 코드: [DefaultTranslateRepository.kt](mobile/app/src/main/java/com/ssafy/mobile/feature/conversation/data/repository/DefaultTranslateRepository.kt)

### 5. 모바일·백엔드 계약 불일치 해결

- JSON 배열과 wrapper 객체 형태 불일치
- Base URL과 endpoint의 `api/` 경로 중복
- 퀴즈 JSON 제출과 서버 multipart 계약 차이
- 네이버 OAuth 요청 방식과 필드 차이

Swagger만 보지 않고 최신 Controller·Response DTO와 MR 이력을 함께 확인하며 계약을 조율했습니다. 문제와 해결 과정은 [개인 트러블슈팅 문서](docs/troubleshooting/hwisu/troubleshooting.md)에 기록했습니다.

---

## 시스템 아키텍처

<img width="1920" alt="SUDA system architecture" src="https://github.com/user-attachments/assets/b1e45dd0-6c55-4ee0-8d8e-0111b959207a" />

| 계층 | 기술 | 역할 |
|:--|:--|:--|
| **Mobile** | Android, Kotlin, Jetpack Compose, Hilt | 화면, 카메라·마이크, 세션과 상태 관리 |
| **On-Device AI** | MediaPipe, TFLite, LiteRT, Qwen LiteRT-LM | 랜드마크 추출, 수어 추론, 로컬 문장 변환 |
| **Backend** | Java 21, Spring Boot 4, Security, JPA | 인증, 학습·퀴즈·리포트, 외부 API Gateway |
| **AI Server** | Python 3.12, FastAPI, PyTorch | 서버 측 수어 추론 |
| **Storage** | PostgreSQL 16, Redis 7, Flyway | 영속 데이터, 토큰·캐시, 마이그레이션 |
| **External API** | Gemini, NAVER CLOVA | 문맥 보정, TTS, STT |
| **Infra** | Docker, Docker Compose | 서비스 실행과 배포 구성 |

---

## 기술 스택

| 영역 | 기술 |
|:--|:--|
| **Android** | Kotlin, Jetpack Compose, Coroutines, StateFlow, Hilt |
| **Network** | Retrofit, OkHttp, REST, WebSocket, multipart |
| **Media** | CameraX, MediaPipe, AudioRecord, Android STT/TTS |
| **On-Device AI** | TensorFlow Lite, LiteRT, LiteRT-LM |
| **Backend** | Java 21, Spring Boot 4, Spring Security, JPA |
| **AI Server** | Python 3.12, FastAPI, PyTorch |
| **Data** | PostgreSQL, Redis, Flyway |
| **Quality** | JUnit, Ktlint, Detekt, Spotless |
| **Infra** | Docker, Docker Compose, GitLab CI |

---

## 프로젝트 구조

```text
.
├── mobile/               # Android 앱과 온디바이스 추론
├── backend/              # Spring Boot API 서버
├── ai/                   # 수어 모델 학습·추론과 FastAPI 서버
├── docs/                 # 아키텍처, API, 요구사항, MR 아카이브
├── exec/                 # 제출용 포팅 문서
├── docker-compose.yml    # DB, Redis, Backend, AI 통합 실행
└── docker-compose.ai.yml # GPU AI 서버 단독 실행
```

---

## 주요 API

| Method | Endpoint | 설명 |
|:--|:--|:--|
| `POST` | `/api/v1/auth/login` | 로그인 |
| `POST` | `/api/v1/auth/refresh` | 토큰 재발급 |
| `POST` | `/api/v1/auth/oauth/naver` | 네이버 OAuth |
| `GET` | `/api/v1/children` | 자녀 프로필 목록 |
| `POST` | `/api/v1/translation/sign-to-speech` | 수어 문장 음성 변환 |
| `POST` | `/api/v1/translation/speech-to-text` | 음성 텍스트 변환 |
| `POST` | `/api/v1/comms/sessions` | 대화 세션 생성 |
| `PATCH` | `/api/v1/comms/sessions/{sessionId}/end` | 대화 세션 종료 |
| `POST` | `/api/v1/learn/quizzes/sessions` | 퀴즈 세션 생성 |
| `GET` | `/api/v1/children/{childId}/reports/summary` | 학습 리포트 |

전체 명세: [docs/api/api-spec.md](docs/api/api-spec.md)

---

## 실행 방법

### 사전 요구사항

- JDK 21
- Android Studio와 Android SDK 35
- Docker Compose V2
- Python 3.12: AI 서버를 로컬에서 실행할 경우

### 환경 변수

```bash
cp .env.example .env
```

실제 Secret과 비밀번호를 입력한 `.env`는 커밋하지 않습니다.

### Backend

```bash
cd backend
./gradlew bootRun
```

### Mobile

```bash
cd mobile
./gradlew assembleDebug
```

### Docker Compose

```bash
docker compose up -d --build
```

AI 서버를 함께 실행하려면 다음 profile을 사용합니다.

```bash
docker compose --profile local-ai up -d --build
```

환경별 상세 설정과 외부 모델 준비 방법은 [포팅 매뉴얼](docs/porting-manual.md)을 참고해 주세요.

---

## 외부 모델 파일

용량과 라이선스 문제로 다음 파일은 저장소에 포함하지 않습니다.

| 파일 | 용도 |
|:--|:--|
| `holistic_landmarker.task` | MediaPipe Holistic landmark |
| `best_sign_model_v6.pt` | FastAPI 서버 수어 추론 |
| `train_config_v6.json` | 서버 모델 설정 |
| `label_map_v6.json` | 수어 label map |
| `Qwen2.5 *.litertlm` | 온디바이스 문장 변환 |

파일 배치와 다운로드 방법: [docs/porting-manual.md](docs/porting-manual.md)

---

## 검증과 현재 한계

### 저장소 품질 구성

- Android 단위 테스트 18개 파일
- Backend 테스트 17개 파일
- Ktlint, Detekt, Android Lint
- Spotless와 Spring 테스트
- GitLab CI 품질 게이트

### 로컬 재검증 환경

2026-06-15 기준 별도 공개본에서 다음을 확인했습니다.

- Secret 패턴 검사에서 실제 API Key나 Private Key가 발견되지 않았습니다.
- `.env`, 인증서, 모델 artifact, `local.properties`를 Git에서 제외합니다.
- Mobile 검증은 Android SDK가 없는 환경에서 실행 전 중단되었습니다.
- Backend 검증은 JDK 21이 없는 환경에서 실행 전 중단되었습니다.

따라서 현재 README는 새 환경에서 전체 테스트 통과를 주장하지 않습니다.

### 개선 과제

- 큰 `ConversationViewModel`을 상태 머신과 UseCase로 분리
- 시연용 문장 규칙을 별도 build variant 또는 feature flag로 격리
- 모델 누락 시 Fake adapter를 사용하는 개발 경로와 운영 빌드 분리
- API Base URL을 CI와 환경별 설정으로 완전 분리
- MockWebServer, Compose UI Test와 실제 기기 회귀 테스트 확대
- 외부 AI·음성 API의 timeout, 장애 지표와 fallback 정책 강화

---

## 주요 문서

| 문서 | 링크 |
|:--|:--|
| 시스템 흐름 | [docs/architecture/system-flow.md](docs/architecture/system-flow.md) |
| ERD | [docs/architecture/erd.md](docs/architecture/erd.md) |
| API 명세 | [docs/api/api-spec.md](docs/api/api-spec.md) |
| 기능 명세 | [docs/specs/features.md](docs/specs/features.md) |
| 포팅 매뉴얼 | [docs/porting-manual.md](docs/porting-manual.md) |
| GitLab MR 아카이브 | [docs/gitlab-mr-archive/index.md](docs/gitlab-mr-archive/index.md) |
| 김휘수 트러블슈팅 | [docs/troubleshooting/hwisu/troubleshooting.md](docs/troubleshooting/hwisu/troubleshooting.md) |

---

## 팀원

| 이름 | 담당 |
|:--|:--|
| [김휘수](https://github.com/HWISU96) | Android |
| [손홍헌](https://github.com/skywalkbee300) | Android |
| [김민선](https://github.com/minseond) | Backend |
| [나예지](https://github.com/yezi720) | Backend |
| [김순우](https://github.com/soontofu12) | AI |
| [설현원](https://github.com/seolsa1014) | AI, Infra |

---

## 보안 및 커밋 제외 항목

```text
.env
*.pem
*.key
*.pt
*.pth
ai/model-artifacts/
local.properties
keystore.properties
secrets.xml
```

Secret, API Key, 비밀번호와 개인 로컬 경로는 커밋하지 않습니다.

---

<div align="center">

**SUDA**  
농인 부모와 청인 자녀의 일상 대화를 더 자연스럽게.

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,18,20&height=100&section=footer" width="100%"/>

</div>
