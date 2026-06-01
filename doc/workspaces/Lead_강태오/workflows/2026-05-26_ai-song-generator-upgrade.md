# 2026-05-26 AI 노래 생성기 고도화

## 목표
초기 4단계 찬양가 생성기(05-21)의 구조적 한계를 극복하고,
실제 사용 가능한 수준의 AI 노래 생성 데스크톱 앱으로 고도화한다.
MusicGen GPU 가속 활성화, 실시간 진행률 표시, 목소리 복제(XTTS v2),
원클릭 자동 파이프라인, Bark CPU 강제 모드 등을 구현한다.

## 범위
- **MusicGen GPU 가속**: CUDA OOB 패치로 GPU 생성 활성화
- **진행률 표시**: 각 단계별 실시간 프로그레스바 (gr.Progress)
- **XTTS v2 목소리 복제**: coqui-tts 설치 + 참조 음성 10~30초로 클로닝
- **원클릭 자동 파이프라인**: 가사→음악→보컬→믹싱 완전 자동화 탭 신설
- **가사→음악 AI 연결**: Ollama가 가사를 분석해 MusicGen 프롬프트 자동 생성
- **Bark CPU 강제 모드**: CUBLAS 오류 해결을 위한 CPU 전용 실행
- **단계 간 자동 연결**: 생성 결과가 다음 단계 입력에 자동 채워짐

## 단계

1. **Gradio 6.x 호환성 수정**
   - `theme`, `css`를 `Blocks` 생성자에서 `launch()`로 이동
   - `show_copy_button` 파라미터 제거 (6.x에서 삭제됨)
   - `.then()` 체이닝으로 단계 간 자동 연결 구현

2. **MusicGen GPU 활성화**
   - `vocab_size=2048` OOB 오류 근본 원인 분석
   - `model.config.decoder.vocab_size = 2049` 패치를 `.to("cuda")` 이전에 적용
   - `torch.cuda.empty_cache()` + `torch.cuda.synchronize()` 선행 호출로 CUBLAS 방지
   - CUDA 연산을 메인 스레드에만 실행(스피너 스레드는 `progress()` 호출만)

3. **실시간 프로그레스바 구현**
   - `_make_spinner()` / `_stop_spinner()` 데몬 스레드 패턴
   - 각 단계(가사/음악/보컬/믹싱)별 0~100% 진행률 표시
   - `progress(pct, desc=...)` 호출로 UI 실시간 업데이트

4. **XTTS v2 목소리 복제 추가**
   - `coqui-tts` 패키지 설치 (Python 3.12 호환 버전)
   - `transformers.pytorch_utils.isin_mps_friendly` 누락 패치
   - `COQUI_TOS_AGREED=1` 환경변수로 라이선스 prompt 우회
   - 참조 음성(마이크 녹음 또는 업로드) 기반 다국어 음성 복제

5. **가사→음악 AI 연결 구현**
   - `_core_music_prompt_from_lyrics()`: Ollama가 생성된 가사를 분석
   - 장르 프리셋 + 가사 감정 분석 → 30단어 이내 MusicGen 영어 프롬프트 생성
   - 원클릭 파이프라인의 2단계(20~28%)에 자동 적용

6. **원클릭 자동 파이프라인 탭 신설**
   - 가사(0~20%) → 가사 분석→음악 프롬프트(20~28%) → 음악(28~65%)
     → 보컬(65~88%) → 믹싱(88~100%) 5단계 자동 실행
   - 각 단계 실패 시 중간 산출물 보존 후 에러 반환

7. **Bark CPU 강제 모드 구현 (CUBLAS 오류 수정)**
   - `torch.serialization.default_restore_location` 패치로 CUDA 텐서를 CPU로 강제 복원
   - `is_available` 미패치(True 유지) → PyTorch 역직렬화 실패 방지
   - `_bark_cpu_context()` / `_bark_restore()` 컨텍스트 매니저 패턴
   - `preload_models()`, `generate_audio()` 전 구간에 패치 적용

## 기술 스택
| 역할 | 라이브러리 | 비고 |
|------|-----------|------|
| 로컬 LLM | `ollama` + `qwen2.5:7b` | 가사 생성 + 가사→음악 프롬프트 분석 |
| 배경음악 | `transformers` MusicGen | GPU 가속, vocab_size=2049 패치 |
| 보컬 합성 (TTS) | `suno-bark` | CPU 강제, ko_speaker_0~9 |
| 보컬 합성 (클로닝) | `coqui-tts` XTTS v2 | 참조 음성 기반 목소리 복제 |
| UI | `gradio` 6.x | Blocks + Tabs + gr.Progress |
| 오디오 처리 | `scipy`, `numpy` | 리샘플링, 믹싱, 클리핑 방지 |
| GPU 가속 | `torch` cu121 | MusicGen GPU / Bark CPU |

## 게이트 / 검증
- [x] MusicGen GPU 생성 동작 (CUDA OOB 패치 적용)
- [x] 실시간 프로그레스바 각 단계 동작
- [x] XTTS v2 로드 성공 (`isin_mps_friendly` 패치 포함)
- [x] Bark CUBLAS 오류 해소 (`default_restore_location` 패치)
- [x] 원클릭 파이프라인 5단계 자동 실행
- [x] 단계 간 자동 연결 (`lyrics_btn → bark_lyrics`, `music_btn → mix_music` 등)
- [x] 영어 가사 자동 번역 후 Bark 입력 처리