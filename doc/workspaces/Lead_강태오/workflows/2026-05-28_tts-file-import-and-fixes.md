# 2026-05-28 TTS 파일 불러오기 기능 및 자동 파인튜닝 버그 수정

## 목표
TTS 앱에 외부 녹음 파일을 불러와 목소리를 등록·학습할 수 있는 기능을 추가하고,
자동 파인튜닝 파이프라인(`auto_finetune.py`)에서 발견된 m4a 변환 및
오류 오탐(false-positive) 버그를 수정한다. 또한 이지윤 파인튜닝 완료를 확인하고,
목소리 반영 비율(tau) 조절 슬라이더 UI를 추가한다.

## 범위
- **이지윤 파인튜닝 완료 확인**: 전일 실행한 자동 파인튜닝 결과 검증
- **Tau 슬라이더 UI**: OpenVoice 목소리 반영 비율(tau) 실시간 조절 슬라이더 추가
- **파일 불러오기 기능**: 외부 녹음 파일(.wav, .mp3, .m4a 등)을 불러와 목소리 등록·학습
- **auto_finetune.py 버그 수정**: m4a→WAV 변환 누락, ASR 오류 오탐 문제 해결
- **End-to-End 테스트**: 다양한 포맷의 녹음 파일로 전체 파이프라인 검증

## 단계

1. **이지윤 파인튜닝 완료 확인**
   - `finetune_log.txt` 확인: 4분 14초에 완료
   - SoVITS 학습 (8 에폭) + GPT 학습 (15 에폭) 모두 성공
   - `gpt_sovits_engine.py`에 이지윤 모델 자동 등록 확인

2. **Tau 슬라이더 UI 구현 (`tts_app.py`)**
   - 음성 생성 탭에 `🎚️ 내 목소리 반영:` 슬라이더 추가
   - 0%~100% 범위, 기본값 70% (tau=0.7)
   - 슬라이더 변경 시 실시간으로 퍼센트와 설명 텍스트 업데이트
   - "내 목소리:" 계열 선택 시만 활성화, 기본 목소리 선택 시 비활성화
   - `convert_voice()` 호출에 슬라이더 값 전달

3. **파일 불러오기 기능 구현 (`tts_app.py`)**
   - 목소리 등록 탭에 `📁 파일 불러오기` 버튼 추가
   - 지원 포맷: `.wav`, `.mp3`, `.m4a`, `.aac`, `.ogg`, `.wma`, `.flac`
   - `_on_import_file()`: 파일 선택 → 이름 입력 → WAV 변환 → 등록 → 자동 학습
   - `_convert_to_wav()`: ffmpeg 기반 16kHz 모노 WAV 변환 (torchaudio 폴백)
   - 파일명에서 목소리 이름 자동 추출 (확장자 제거)
   - 기존 `register_voice()` 파이프라인과 통합

4. **auto_finetune.py 버그 수정 #1: m4a→WAV 변환**
   - 기존 문제: `_slice_audio()`가 m4a 파일을 그대로 `raw/` 디렉토리에 복사
   - `slicer2.py`는 WAV만 처리 가능하여 슬라이싱 실패
   - 수정: 비-WAV 파일 감지 시 ffmpeg로 16kHz 모노 WAV 변환 후 복사
   - ffmpeg 경로: GPT-SoVITS `runtime/ffmpeg.exe` 우선, 없으면 시스템 PATH
   - 슬라이싱 타임아웃 120초 → 600초로 증가

5. **auto_finetune.py 버그 수정 #2: ASR 오류 오탐**
   - 기존 문제: `_run_step()`이 stderr의 tqdm 진행 바를 오류로 오인
   - ASR 100% 완료 후에도 "실패"로 표시되는 현상
   - 수정: 정규식 기반 stderr 필터링 도입
     - tqdm 진행 바 (`100%|█████|`) 제거
     - `UserWarning`, `warnings.warn()` 제거
     - `Fetching N files` 메시지 제거
   - 실제 오류 키워드 목록으로만 판단:
     `traceback`, `exception`, `modulenotfounderror`, `runtimeerror`,
     `filenotfounderror`, `valueerror`, `typeerror`, `keyerror`,
     `importerror`, `oserror`, `keyboardinterrupt`, `memoryerror`

6. **End-to-End 테스트**
   - 테스트 1: `260522 회의록.m4a` → 슬라이싱 실패 (WinError 2, ffmpeg 미발견)
     → 코드 수정 후 재테스트
   - 테스트 2: `260526 회의록.m4a` → m4a→WAV 변환 성공, ASR 오탐 발견
     → 코드 수정 후 재테스트
   - 테스트 3: `260526 회의록.m4a` 재실행 → 전체 파이프라인 완료 (10분 36초)
   - 테스트 4: `녹음 (4).m4a` → SE 추출 실패 + SoVITS AssertionError
     (유효 학습 샘플 부족 — 녹음 품질 문제, 코드 버그 아님)

## 기술 스택
| 역할 | 라이브러리 | 비고 |
|------|-----------|------|
| 음성 클로닝 | GPT-SoVITS v2pro | 파인튜닝 파이프라인 |
| 포맷 변환 | ffmpeg | m4a/mp3/aac/ogg → WAV |
| 폴백 변환 | torchaudio | ffmpeg 없을 때 WAV 변환 |
| GUI | CustomTkinter | 슬라이더 + 파일 불러오기 UI |
| 음성 변환 | OpenVoice v2 | tau 파라미터 조절 |
| ASR | Faster Whisper (large-v3) | 음성 → 텍스트 전사 |

## 게이트 / 검증
- [x] 이지윤 파인튜닝 완료 확인 (4분 14초)
- [x] Tau 슬라이더 UI 동작 확인 (커스텀 목소리 활성화/기본 목소리 비활성화)
- [x] 파일 불러오기 → 포맷 변환 → 목소리 등록 흐름 확인
- [x] m4a→WAV 변환 정상 동작 (ffmpeg 경로 탐지 포함)
- [x] ASR stderr 필터링으로 오탐 해소 확인
- [x] 260526 회의록.m4a End-to-End 파인튜닝 성공 (10분 36초)
- [x] 녹음 품질 부족 시 적절한 에러 메시지 출력 확인
- [ ] 다양한 오디오 포맷(.ogg, .flac 등) 변환 테스트 (향후)


> **[미구현 항목 안내]** 위 체크리스트에서 - [ ]로 표시된 항목은
> 현재 미구현 상태이며, 회의 확정 또는 일정에 따라 추후 구현될 예정입니다.
> 해당 항목이 구현되면 - [x]로 변경하고 별도 워크플로우·리포트를 작성합니다.