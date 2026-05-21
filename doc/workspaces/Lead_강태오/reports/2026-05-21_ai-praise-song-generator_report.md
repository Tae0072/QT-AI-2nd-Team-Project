# 2026-05-21 AI 찬양가 생성기 개발 — 결과 보고

## 요약
웹 API 없이 로컬 AI 모델(Ollama + MusicGen + Bark)만으로 찬양가를 생성하는 4단계 Gradio 앱을 완성했다.
가사 생성 → 배경음악 생성 → 보컬 합성 → 음향 믹싱 전 과정이 하나의 UI에서 자동 연동되며,
NVIDIA GPU 환경(CUDA 12.1)에서 `setup.bat` + `실행.bat`으로 실행 가능한 프로토타입을 완성했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `찬양가생성기/app.py` | Gradio 4단계 메인 앱 (가사/음악/보컬/믹싱) |
| `찬양가생성기/setup.bat` | Python 3.12 venv 의존성 설치 스크립트 |
| `찬양가생성기/실행.bat` | 앱 구동 배치 파일 |
| `output/music.wav` | MusicGen 생성 배경음악 |
| `output/vocal.wav` | Bark 생성 보컬 |
| `output/final_song.wav` | 최종 믹싱 결과물 |

## 구현 상세

### 1단계 — 가사 생성
`ollama` Python 라이브러리로 로컬에서 실행 중인 `qwen2.5:7b` 모델을 호출한다.
주제·분위기·절 수·추가 요청을 입력받아 한국어 찬양 가사를 생성하며,
결과는 1단계 텍스트박스와 3단계 입력창에 동시에 자동 채워진다.

### 2단계 — 배경음악 생성
`transformers.MusicgenForConditionalGeneration` (`facebook/musicgen-small`)을 GPU로 실행한다.
음악 스타일·분위기·길이(초)를 입력받아 `max_new_tokens = int(duration)*51+3` 으로 길이를 제어,
결과를 `output/music.wav`(int16)로 저장한다.
`audiocraft` 라이브러리는 C++ Build Tools 없이 설치 불가여서 `transformers` MusicGen으로 대체했다.

### 3단계 — 보컬 생성
`suno-bark`의 `generate_audio()`로 한국어 TTS를 합성한다.
가사 각 행을 `♪ 행1 ♪ 행2 ♪` 형태로 감싸 노래 뉘앙스를 유도한다.
화자 프리셋은 `v2/ko_speaker_4~7`을 사용한다:
- 여성 부드러운: `v2/ko_speaker_4`
- 여성 밝은: `v2/ko_speaker_5`
- 남성 따뜻한: `v2/ko_speaker_6`
- 남성 웅장한: `v2/ko_speaker_7`

결과는 `output/vocal.wav`(float32, 24000 Hz)로 저장된다.

### 4단계 — 음향 믹싱
`to_float32()` 함수가 오디오 dtype에 따라 정규화 방식을 분기한다:
- int16 → `/32767.0`
- int32 → `/2147483647.0`
- float32 → 그대로

Bark(24000 Hz)와 MusicGen(≈32000 Hz)의 샘플레이트 불일치는 `scipy.signal.resample_poly`로 해결한다.
볼륨 슬라이더로 보컬·음악 각각 조절하고, 옵션 리버브(scipy 컨볼루션)를 적용한 뒤
최종 `output/final_song.wav`로 저장한다.

## 발생한 문제와 해결

### 환경 문제
| 문제 | 원인 | 해결 |
|------|------|------|
| `torch` 설치 불가 | 기본 Python 3.14가 PyTorch 미지원 | winget으로 Python 3.12 설치 → `venv312` 생성 |
| `audiocraft` 설치 실패 | C++ Build Tools 미설치 | `transformers` MusicGen으로 대체 |
| 배치파일 한국어 깨짐 | bash heredoc이 UTF-8로 저장 | PowerShell `WriteAllText` + CP949 인코딩 사용 |
| 포트 7860 충돌 | 기존 Python 프로세스 잔존 | `Stop-Process -Id` 로 종료 |

### 코드 버그
| 문제 | 원인 | 해결 |
|------|------|------|
| 보컬이 최종 파일에서 무음 | float32를 int16으로 오인해 `/32767` 나눔 | dtype 분기 `to_float32()` 함수 신설 |
| 샘플레이트 불일치로 믹싱 왜곡 | Bark=24000Hz, MusicGen≈32000Hz | `resample_poly`로 보컬을 음악 샘플레이트에 맞춤 |
| `set_generation_params` 오류 | `transformers` MusicGen에 없는 메서드 | 제거 후 `model.generate(max_new_tokens=…)` 직접 전달 |
| 들여쓰기 오류 line 93 | 정규식 치환 후 들여쓰기 추가됨 | PowerShell 직접 라인 인덱스 교체로 수정 |
| 화자 성별 오매핑 | `ko_speaker_0~3`이 기대 성별과 불일치 | `ko_speaker_4~7`로 교체 |
| `show_copy_button` 오류 | Gradio 6.x API 변경 | 해당 파라미터 제거 |

## 검증
- [x] 가사 생성 정상 동작 (qwen2.5:7b 한국어 출력)
- [x] 배경음악 생성 정상 동작 (music.wav 재생 확인)
- [x] 보컬 생성 정상 동작 (vocal.wav Gradio 플레이어 재생 확인)
- [x] 믹싱 정상 동작 (final_song.wav 보컬+음악 합산 확인)
- [x] 1단계 → 3단계 자동 연동 동작
- [x] `setup.bat` / `실행.bat` 정상 구동

## 한계 및 향후 과제
- **보컬 음질**: Bark는 TTS 기반이라 실제 노래 창법과 차이가 있음. 향후 로컬 SVC(Singing Voice Conversion) 모델(예: So-VITS-SVC, RVC) 도입 검토 필요
- **음악 다양성**: `musicgen-small` 모델의 한계로 생성 음악의 장르·표현 범위가 제한적. `musicgen-medium` 이상 사용 시 개선 가능하나 VRAM 요구량 증가
- **Bible TTS 프로그램**: 원래 목표 중 두 번째인 성경 TTS 리더는 이번 세션에서 미착수. 다음 작업 대상

## 다음 단계
1. **보컬 음질 개선** — 로컬 SVC 모델(RVC 또는 So-VITS-SVC) 파이프라인 연구 및 통합
2. **성경 TTS 리더 개발** — 두 번째 목표 프로그램 착수 (로컬 TTS 모델 선정 필요)
3. **UI 개선** — 생성된 오디오 파형 시각화, 실시간 진행 상태 표시

## 관련 파일 경로
```
C:\Users\G\Downloads\찬양가생성기\
├── app.py           # Gradio 메인 앱
├── setup.bat        # 의존성 설치
├── 실행.bat          # 앱 구동
└── output\
    ├── music.wav    # MusicGen 배경음악
    ├── vocal.wav    # Bark 보컬
    └── final_song.wav  # 최종 믹싱
```