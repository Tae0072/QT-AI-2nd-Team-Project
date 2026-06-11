# 2026-06-05 TTS 클라우드 서비스 (팀 공유용)

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다. 제품 반영 시 별도 검토 필요.

## 목표
TTS가 Lead PC(`bible-tts`, 8090)에만 있어서 팀원이 못 쓰는 문제를 해결한다.
GPU 없이 클라우드에서 항상 켜져 동작하는 경량 TTS 서비스를 만들어,
팀 전원이 같은 서버 주소로 QT 읽기 기능을 쓸 수 있게 한다.

## 방향 결정 (사용자 선택)
- 후보: ①터널로 내 PC 공개 ②클라우드 배포 ③음성 사전 생성
- 선택: **②클라우드 배포** — 내 PC를 안 켜도 됨. 대신 무료/저가 서버는 GPU가
  없어 Edge TTS(기본 3목소리)만 가능, 커스텀 학습 목소리는 제외(수용).

## 작업 브랜치
`feature/tts-cloud-deploy` (dev 기반)

## 단계
- [x] 1단계: 앱 API 규격 확인 (`/`, `/voices`, `/qt/read`, Bearer, [N초])
- [x] 2단계: 경량 서비스 `tts-service/app.py` — FastAPI + edge-tts
  - 목소리 표시 이름 기존과 동일(선희/인준/현수) → 저장된 설정 호환
  - `[N초]` 묵음 태그 동일 적용, pydub로 무음 합성
- [x] 3단계: 컨테이너화 — Dockerfile(python-slim+ffmpeg), docker-compose, .env.example
- [x] 4단계: 로컬 검증 (샌드박스) — 기동/인증/목소리/음성생성/[N초]
- [x] 5단계: 배포 가이드 + 앱 연결 문서 (`tts-service/README.md`)
- [ ] 6단계 (사용자): 클라우드 플랫폼 선택해 실제 배포 + `API_TOKEN` 설정
- [ ] 7단계 (사용자): 배포 URL로 앱 빌드(`--dart-define=TTS_BASE_URL/TTS_TOKEN`)

## 산출물
| 파일 | 설명 |
|------|------|
| `tts-service/app.py` | Edge TTS 기반 경량 TTS 서비스 |
| `tts-service/requirements.txt` | fastapi/uvicorn/edge-tts/pydub |
| `tts-service/Dockerfile` | CPU 전용 컨테이너 (ffmpeg 포함) |
| `tts-service/docker-compose.yml` | 로컬/VM 실행용 |
| `tts-service/.env.example` | API_TOKEN/PORT 예시 |
| `tts-service/README.md` | 배포 가이드 + 앱 연결법 |

## 다음
- 사용자가 플랫폼(Render 등) 선택 후 배포 → URL 확정 시 app_config dev 기본값
  교체 여부 결정(팀원 편의)
