# 2026-06-05 TTS 클라우드 서비스 결과 보고

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다.

## 요약
TTS를 Lead PC에만 의존하던 구조를, 팀 전원이 쓸 수 있는 **클라우드 배포형 경량
TTS 서비스**로 분리했다. Edge TTS 기반이라 GPU 없이 CPU 클라우드에서 동작하며,
앱의 기존 API 규격·목소리 이름·`[N초]` 태그를 그대로 따른다.

## 근본 원인
앱은 dev에서 TTS 주소를 `10.0.2.2:8090`(에뮬레이터→호스트 localhost)으로 본다.
이 주소는 각자 자기 PC를 가리켜, 팀원 기기에서는 TTS 서버가 없어 실패했다.
→ TTS 엔진을 공용 주소(클라우드)로 옮기는 것이 해결책.

## 구현 내용
- `tts-service/` 신규: FastAPI + edge-tts 단일 서비스
  - `GET /` 헬스, `GET /voices`(Bearer), `POST /qt/read`(Bearer)
  - 목소리: 선희/인준/현수 — 기존 표시 이름 유지로 저장된 설정 그대로 작동
  - `[N초]` 파싱 + pydub 무음 합성(문장 사이 0.4초, 태그는 정확히 N초)
  - `API_TOKEN` 환경변수로 Bearer 검증(미설정 시 개발용으로 인증 생략)
- Docker화: python-slim + ffmpeg, docker-compose, .env.example, .dockerignore
- 배포 가이드(README): Render/Railway/Fly/VM + 앱 `--dart-define` 연결법

## 검증 (샌드박스, end-to-end)
- [x] app.py 문법 + 기동 OK
- [x] `GET /` 200, `GET /voices` 토큰 없으면 401 / 있으면 3목소리 반환
- [x] 단위: `[N초]` 파싱 정확, 무음 합성 길이 1+3+1=5.0초 정확, 목소리 매핑+폴백
- [x] E2E 음성 생성: 일반 4.13초 vs `[3초]` 포함 8.88초 — 무음 정확히 삽입
- [x] edge-tts 네트워크 호출 정상 (한국어 합성 bytes 수신)

## 남은 작업 (사용자 몫 — 클라우드 계정 필요)
1. 플랫폼 선택(추천: Render 무료 티어, Docker, Root=`tts-service`)
2. `API_TOKEN` 환경변수 설정(강한 임의 문자열)
3. 배포 URL 확보 후 앱 빌드: `--dart-define=TTS_BASE_URL=<URL> --dart-define=TTS_TOKEN=<토큰>`
   - 또는 `app_config.dart` dev 기본 ttsBaseUrl을 배포 URL로 한 줄 교체(팀원 편의)

## 참고/제약
- GPU 커스텀 학습 목소리는 미포함(클라우드 CPU 한계) — 기존 로컬 Voice Studio 전용
- Render 무료 티어는 미사용 시 슬립 → 첫 요청 지연. 앱의 사전 생성으로 체감 완화
- 서버 코드(qtai-server) 변경 없음, Flutter 코드 변경 없음(주소 override만)
