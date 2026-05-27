# 2026-05-22 성경 TTS 데스크톱 앱 — 프로젝트 초기 구축 결과 보고

## 요약
QT-AI 프로젝트의 TTS 기능 개발을 위해 `bible-tts` 프로젝트를 새로 만들었다.
Bark TTS를 1차 엔진으로, Edge TTS를 폴백 엔진으로 사용하는 데스크톱 앱을 완성했다.
CustomTkinter GUI로 텍스트 입력, 목소리 녹음/등록, 음성 생성을 한 화면에서 처리할 수 있다.

## 산출물
| 파일 | 설명 |
|------|------|
| `bible-tts/tts_app.py` | CustomTkinter 기반 메인 GUI 앱 |
| `bible-tts/bark_engine.py` | Bark TTS 엔진 래퍼 (GPU/CPU 자동 감지) |
| `bible-tts/edge_engine.py` | Edge TTS 폴백 엔진 래퍼 |
| `bible-tts/voices/` | 녹음된 목소리 저장 디렉토리 |
| `bible-tts/output/` | 생성된 음성 파일 저장 디렉토리 |

## 아키텍처
```
사용자 GUI (tts_app.py)
    ├── 텍스트 입력 → TTS 엔진 선택
    │       ├── Bark TTS (bark_engine.py) ─── GPU 음성 합성
    │       └── Edge TTS (edge_engine.py) ─── 클라우드 API 폴백
    ├── 목소리 녹음 → voices/ 저장
    └── 음성 재생 → output/ 에서 WAV 재생
```

## 구현 상세

### Bark TTS 엔진 (`bark_engine.py`)
Bark는 Suno AI의 오픈소스 TTS 모델로, 다양한 언어와 감정 표현이 가능하다.
한국어 텍스트를 입력하면 약 15초 단위로 분할하여 각각 음성을 생성한 뒤
하나의 WAV로 이어붙인다. GPU(CUDA)가 있으면 자동으로 사용하고,
없으면 CPU로 폴백한다.

### Edge TTS 폴백 (`edge_engine.py`)
Microsoft Edge의 TTS API를 사용하는 무료 클라우드 엔진이다.
Bark 모델 로드 실패나 GPU 메모리 부족 시 자동 전환된다.
한국어 음성은 `ko-KR-SunHiNeural`(여성), `ko-KR-InJoonNeural`(남성)을 지원한다.

### GUI 앱 (`tts_app.py`)
CustomTkinter를 사용한 데스크톱 앱으로, 하나의 화면에서 모든 기능을 제공한다.
음성 생성은 백그라운드 스레드에서 실행되어 UI가 멈추지 않는다.
진행 상태는 로그 영역에 실시간 표시된다.

## 발생한 문제와 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| Bark 모델 다운로드 오래 걸림 | 첫 실행 시 ~2GB 모델 다운로드 필요 | 진행 상태 로그로 사용자에게 안내 |
| GPU 메모리 부족 (3060 Ti 8GB) | Bark가 ~4GB VRAM 사용 | Edge TTS 자동 폴백 구현 |
| 긴 텍스트 잘림 현상 | Bark 최대 ~15초 제한 | 문장 단위 분할 후 청크별 생성 → 합치기 |

## 검증
- [x] GUI 앱 실행 및 기본 동작 확인
- [x] Bark TTS 한국어 음성 생성 정상
- [x] Edge TTS 폴백 동작 정상
- [x] 목소리 녹음 및 등록 기능 정상
- [x] 출력 WAV 파일 재생 정상

## 한계 및 향후 과제
- **Bark 음질**: 한국어 발음이 부자연스러운 경우가 있음
- **목소리 복제 한계**: Bark의 목소리 복제는 참조 음성 유사도가 낮음
- **실시간 미지원**: 음성 생성에 수 초~수십 초 소요
- **향후**: 더 높은 음질의 목소리 복제 엔진 탐색 필요 (GPT-SoVITS 등)
