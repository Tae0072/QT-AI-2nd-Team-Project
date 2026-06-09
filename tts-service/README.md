# QT-AI TTS 서비스 (클라우드 배포용)

팀 전원이 QT 읽기(TTS) 기능을 쓸 수 있도록, 로컬 PC에 묶여 있던 음성 생성을
**항상 켜진 클라우드 서버**로 옮기는 경량 서비스입니다.

- 엔진: **Edge TTS** (Microsoft 무료 온라인 TTS) — GPU 불필요, CPU 클라우드에서 동작
- 목소리: 선희(여성)·인준(남성)·현수(남성, 다국어) 3종 — 앱에 저장된 설정과 호환
- `[N초]` 묵음 태그 지원 (예: `본문 [2초] 해설`)
- 앱(`flutter-app`)의 기존 API 규격 그대로: `GET /`, `GET /voices`, `POST /qt/read`

> 참고: GPU 커스텀 학습 목소리(GPT-SoVITS/OpenVoice)는 이 서비스에 포함되지
> 않습니다. 그건 기존 로컬 Voice Studio(`bible-tts`)에서만 됩니다.

## API 규격

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/` | 헬스체크 (200) |
| GET | `/voices` | 목소리 목록 (Bearer) |
| POST | `/qt/read` | `{text, voice, format}` → 음성 파일 (Bearer) |

인증: `API_TOKEN` 환경변수가 설정돼 있으면 `Authorization: Bearer <토큰>` 필요.
앱의 `TTS_TOKEN`과 같은 값으로 맞춥니다.

## 로컬 실행

```bash
# Docker (권장)
cd tts-service
API_TOKEN=devtoken docker compose up --build

# 또는 파이썬 직접 (ffmpeg 필요)
pip install -r requirements.txt
API_TOKEN=devtoken python app.py   # http://localhost:8090
```

확인:
```bash
curl http://localhost:8090/
curl -H "Authorization: Bearer devtoken" http://localhost:8090/voices
```

## 클라우드 배포

어느 플랫폼이든 이 폴더의 `Dockerfile` 하나면 됩니다. 공통 절차:

1. 저장소를 연결하고 **루트 디렉터리를 `tts-service`** 로 지정
2. 환경변수 **`API_TOKEN`** 에 강한 임의 문자열 설정 (`openssl rand -hex 24`)
3. 배포 후 받은 URL(예: `https://qt-ai-tts.onrender.com`)을 앱에 연결(아래)

### Render (무료 티어, 가장 쉬움)
- New → Web Service → 이 repo 선택
- Root Directory: `tts-service`, Runtime: Docker
- Environment에 `API_TOKEN` 추가 → Deploy
- 무료 티어는 미사용 시 슬립(첫 요청이 수십 초 느림). 앱은 본문 로드 시 미리
  생성하므로 체감은 덜함.

### Railway / Fly.io / 일반 VM
- Railway: New Project → Deploy from repo → Root `tts-service` → Variables에 `API_TOKEN`
- Fly.io: `cd tts-service && fly launch`(Dockerfile 자동 인식) → `fly secrets set API_TOKEN=...`
- 일반 VM(EC2/GCE 등): `docker compose up -d` 후 80/443 리버스 프록시(nginx)로 노출

## 앱(Flutter) 연결

배포 URL과 토큰을 빌드 시 주입합니다(팀원 공통):

```bash
flutter run \
  --dart-define=TTS_BASE_URL=https://<배포-URL> \
  --dart-define=TTS_TOKEN=<API_TOKEN과 동일>
```

릴리스 빌드도 동일하게 `--dart-define` 두 개를 넣습니다.
앱은 이미 `TTS_BASE_URL` override를 지원하므로 코드 변경이 필요 없습니다
(`flutter-app/lib/core/config/app_config.dart`).

> 매번 입력이 번거로우면, 배포 URL이 확정된 뒤 `app_config.dart`의 dev 기본
> `ttsBaseUrl`을 그 URL로 한 줄 바꾸면 팀원은 토큰만 넣으면 됩니다.

## 보안 메모
- `API_TOKEN`은 강한 임의 문자열로 설정하고, 평문으로 저장소에 커밋하지 않습니다
  (`.env`는 `.gitignore` 대상, `.env.example`만 커밋).
- 운영 URL은 HTTPS(플랫폼 기본 제공)를 사용합니다.
