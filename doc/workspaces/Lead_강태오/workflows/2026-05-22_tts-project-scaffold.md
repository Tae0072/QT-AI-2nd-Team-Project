# 2026-05-22 성경 TTS 데스크톱 앱 — 프로젝트 초기 구축

## 목표
QT-AI 프로젝트의 TTS(텍스트 음성 변환) 기능을 개발하기 위한
데스크톱 앱을 처음부터 만든다. 성경 본문을 자연스러운 목소리로
읽어주는 것이 최종 목표이며, 첫 단계로 Bark TTS 엔진 기반
프로토타입을 완성한다.

## 범위
- **프로젝트 폴더 생성**: `C:\Users\G\Downloads\bible-tts\` 작업 환경 구축
- **Bark TTS 엔진 통합**: Suno AI의 Bark 모델을 사용한 한국어 음성 합성
- **GUI 앱 개발**: CustomTkinter 기반 데스크톱 앱 (`tts_app.py`)
- **목소리 녹음/등록**: 사용자 목소리를 녹음하고 TTS 참조 음성으로 등록하는 기능
- **Edge TTS 폴백**: Bark 실행 실패 시 Microsoft Edge TTS를 대체 엔진으로 사용

## 단계

1. **프로젝트 환경 구성**
   - `bible-tts/` 디렉토리 생성
   - Python 가상환경 구축, 필요 패키지 설치
   - `voices/` (녹음 저장), `output/` (생성 결과) 디렉토리 구조 확립

2. **Bark TTS 엔진 래퍼 작성**
   - `bark_engine.py`: Bark 모델 로드, GPU/CPU 자동 감지
   - 한국어 텍스트 → 음성 WAV 생성 함수 구현
   - 긴 텍스트 분할 처리 (Bark는 ~15초 제한)
   - 생성된 청크 WAV를 하나로 이어붙이기

3. **Edge TTS 폴백 엔진 작성**
   - `edge_engine.py`: Microsoft Edge TTS API 래퍼
   - 한국어 여성/남성 음성 선택 (`ko-KR-SunHiNeural` 등)
   - Bark 실패 시 자동 전환 로직

4. **데스크톱 GUI 앱 개발 (`tts_app.py`)**
   - CustomTkinter 기반 메인 윈도우
   - 텍스트 입력 영역 (성경 본문 붙여넣기)
   - 목소리 선택 드롭다운 (등록된 목소리 목록)
   - TTS 엔진 선택 (Bark / Edge TTS)
   - "음성 생성" 버튼 → 백그라운드 스레드에서 생성
   - 진행 상태 로그 표시
   - 생성된 WAV 파일 재생 기능

5. **목소리 녹음/등록 기능**
   - 마이크 녹음 UI (녹음 시작/중지 버튼)
   - `sounddevice` + `scipy`로 WAV 저장
   - 녹음 파일을 `voices/이름_recording.wav`로 저장
   - 등록된 목소리 목록 자동 갱신

## 기술 스택
| 역할 | 라이브러리 | 비고 |
|------|-----------|------|
| TTS 엔진 (1차) | Bark (suno-ai/bark) | GPU 사용, 한국어 지원 |
| TTS 엔진 (폴백) | Edge TTS | Microsoft 클라우드 API, 무료 |
| GUI | CustomTkinter | tkinter 기반 모던 UI |
| 오디오 녹음 | sounddevice + scipy | WAV 포맷 저장 |
| 오디오 재생 | playsound / pygame | 생성된 음성 재생 |

## 게이트 / 검증
- [x] `tts_app.py` 실행 시 GUI 정상 표시
- [x] 텍스트 입력 → Bark TTS 음성 생성 동작
- [x] Edge TTS 폴백 정상 동작
- [x] 마이크 녹음 → WAV 저장 → 목소리 등록
- [x] `voices/`, `output/` 디렉토리 자동 생성
