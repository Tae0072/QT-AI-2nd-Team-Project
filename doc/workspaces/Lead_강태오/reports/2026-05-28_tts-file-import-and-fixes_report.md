# 2026-05-28 TTS 파일 불러오기 기능 및 자동 파인튜닝 버그 수정 결과 보고

## 요약
TTS 앱에 외부 녹음 파일 불러오기 기능과 목소리 반영 비율(tau) 조절 슬라이더를 추가했다.
자동 파인튜닝 파이프라인에서 m4a 파일 미변환 버그와 ASR 오류 오탐 버그를 수정했다.
이지윤 파인튜닝 완료를 확인하여 현재 3명(이승욱, 김지민, 이지윤)의 파인튜닝 모델이
사용 가능한 상태이다. 수정 후 m4a 회의록 파일로 End-to-End 테스트를 통과했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `bible-tts/tts_app.py` | 파일 불러오기 + tau 슬라이더 UI 추가 |
| `bible-tts/auto_finetune.py` | m4a→WAV 변환 + ASR 오탐 수정 |
| `bible-tts/finetune_log.txt` | 이지윤 파인튜닝 완료 로그 |
| `bible-tts/gpt_sovits_engine.py` | 이지윤 모델 자동 등록 반영 |

## 아키텍처

### 파일 불러오기 흐름
```
사용자 → "📁 파일 불러오기" 버튼 클릭
       ↓
파일 선택 다이얼로그 (.wav/.mp3/.m4a/.aac/.ogg/.wma/.flac)
       ↓
목소리 이름 입력 (기본값: 파일명에서 추출)
       ↓
_convert_to_wav() — 비-WAV 파일 → 16kHz 모노 WAV 변환
  ├── ffmpeg 우선 (GPT-SoVITS runtime 또는 시스템 PATH)
  └── torchaudio 폴백
       ↓
register_voice() — 기존 목소리 등록 파이프라인 진입
  ├── SE(Speaker Embedding) 추출
  ├── 참조 음성 파일 생성
  └── voices.json 업데이트
       ↓
_start_auto_finetune() — 백그라운드 파인튜닝 시작
  └── auto_finetune.run_finetune(voice_name)
```

### Tau 슬라이더 동작
```
슬라이더 0% ←──────────→ 100%
           │                │
     Edge TTS 100%    내 목소리 100%
     내 목소리 0%     Edge TTS 0%
           │                │
      기본값: 70% (tau=0.7)

voice_converter.convert_voice(tau=slider_value)
  → OpenVoice tone_color_converter에 tau 전달
```

### 수정된 _run_step 오류 판별 로직
```
subprocess 실행 → stderr 캡처
       ↓
정규식 필터링:
  ① tqdm 진행 바 (100%|█████|) 제거
  ② UserWarning 제거
  ③ Fetching N files 제거
       ↓
실제 오류 키워드 검색:
  traceback, exception, runtimeerror, ...
       ↓
  키워드 있음 → 실패 보고
  키워드 없음 → "완료 (경고 무시)" 성공 처리
```

## 구현 상세

### 파일 불러오기 (`_on_import_file`)
기존에는 앱 내 녹음 버튼으로만 목소리를 등록할 수 있었다.
이번에 `📁 파일 불러오기` 버튼을 추가하여, 미리 녹음해 둔 파일을
직접 로드할 수 있게 했다. 지원 포맷은 WAV, MP3, M4A, AAC, OGG,
WMA, FLAC 7종이다.

파일 선택 후 목소리 이름 입력 다이얼로그가 나타나며,
기본값은 파일명에서 확장자를 뺀 이름이다.
비-WAV 포맷은 ffmpeg를 이용해 16kHz 모노 WAV로 자동 변환하고,
이후 기존의 `register_voice()` 파이프라인을 그대로 재사용한다.

### Tau 슬라이더 UI
OpenVoice의 `tau` 파라미터는 음색 변환 강도를 조절한다.
0.0이면 Edge TTS 원본, 1.0이면 사용자 목소리 100% 반영이다.
슬라이더를 움직이면 퍼센트와 비율 설명이 실시간 갱신된다.
커스텀 목소리(`내 목소리:` 접두사)가 선택된 경우에만 활성화되고,
기본 목소리 선택 시 자동으로 비활성화된다.

### m4a→WAV 변환 수정 (`_slice_audio`)
기존에는 `_slice_audio()`가 녹음 파일의 확장자를 검사하지 않고
그대로 `raw/` 디렉토리에 복사했다. `slicer2.py`는 WAV만 읽을 수
있어서 m4a 파일은 슬라이싱에 실패했다.

수정 후에는 확장자가 `.wav`가 아닌 경우 ffmpeg로 변환한 뒤 복사한다.
ffmpeg 경로는 GPT-SoVITS의 `runtime/ffmpeg.exe`를 우선 탐지하고,
없으면 시스템 PATH에서 찾는다.

### ASR 오류 오탐 수정 (`_run_step`)
GPT-SoVITS의 전처리 스크립트들은 exit code 1을 반환하면서도
실제로는 정상 완료되는 경우가 많다. 특히 ASR 단계에서 tqdm
진행 바가 stderr에 출력되고, 이를 오류로 오인하는 문제가 있었다.

수정 후에는 stderr 텍스트에서 tqdm 바, UserWarning, Fetching 메시지를
정규식으로 제거한 뒤, 남은 텍스트에서 실제 Python 예외 키워드
(traceback, exception 등 13종)가 있는지만 확인한다.

## 발생한 문제와 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| m4a 파일 슬라이싱 실패 | `_slice_audio`가 m4a를 WAV로 변환하지 않음 | ffmpeg 변환 단계 추가 |
| ASR 100% 완료인데 "실패" 표시 | tqdm stderr 출력을 오류로 오인 | 정규식 필터링 + 키워드 기반 판별 |
| 첫 m4a 테스트 WinError 2 | ffmpeg 경로를 찾지 못함 | runtime/ffmpeg.exe 우선 탐지 로직 추가 |
| 녹음(4).m4a SoVITS AssertionError | 유효 학습 샘플 < 2개 (녹음 품질 부족) | 코드 버그 아님, 사용자에게 녹음 품질 안내 |
| 녹음(4).m4a SE 추출 실패 | 8초 참조 음성이 OpenVoice에 "too short" | CosyVoice 폴백으로 처리 (GPT-SoVITS는 SE 불필요) |
| 파일 동기화 문제 | Edit 도구 변경이 bash VM에 즉시 반영 안 됨 | bash에서 직접 파일 쓰기로 우회 |

## 검증
- [x] 이지윤 파인튜닝 로그 확인 — 4분 14초 완료, 모델 등록 정상
- [x] Tau 슬라이더 — 커스텀 목소리 활성화, 기본 목소리 비활성화 정상
- [x] 파일 불러오기 — mp3/m4a 파일 로드 → WAV 변환 → 등록 정상
- [x] m4a→WAV 변환 — ffmpeg 경로 탐지 및 변환 정상
- [x] ASR 오탐 수정 — tqdm 100% 출력에도 성공 판정 정상
- [x] 260526 회의록.m4a — 파인튜닝 전체 파이프라인 성공 (10분 36초)
- [x] 녹음 품질 부족 시 — AssertionError 발생 후 적절한 로그 출력 확인

## 파인튜닝 현황 (누적)
| 팀원 | 방식 | SoVITS 가중치 | GPT 가중치 | 소요 시간 |
|------|------|--------------|-----------|----------|
| 이승욱 | 수동 (WebUI) | `leeseungwook_e8_s208.pth` | `leeseungwook-e15.ckpt` | ~5분 |
| 김지민 | 스크립트 | `kimjimin_e8_s208.pth` | `kimjimin-e15.ckpt` | ~5분 |
| 이지윤 | 자동 (auto_finetune) | `ijiyun_e8_s*.pth` | `ijiyun-e15.ckpt` | 4분 14초 |

## 한계 및 향후 과제
- **녹음 품질 가이드라인 필요**: 품질 낮은 녹음(배경 소음, 다중 화자 등)은 학습 실패 — 사용자 안내 UI 추가 필요
- **다양한 포맷 검증**: OGG, WMA, FLAC 등 비주류 포맷 테스트 미완
- **슬라이더 세분화**: 현재 단순 tau 비율만 조절, 음성 스타일(부드럽게/강하게 등) 제어는 모델 변경 필요하여 보류
- **파일 크기 제한**: 대용량 파일(30분+) 변환 시 메모리/시간 소요 관련 가드 미구현
