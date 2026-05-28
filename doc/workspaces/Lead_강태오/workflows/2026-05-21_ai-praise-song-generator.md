# 2026-05-21 AI 찬양가 생성기 개발

> ⚠️ **[비제품 범위]** 이 문서는 qtai-server 제품에 반영되지 않는 **Lead 개인 기술 실험·R&D 기록**입니다.
> AI 가사/음원 생성, TTS 합성, 비-DeepSeek LLM 등은 현재 제품 정책(CLAUDE.md §8) 적용 범위 밖이며,
> 제품 백로그·구현 예정으로 간주하지 않습니다.


## 목표
웹 API 없이 로컬 AI 모델만으로 찬양가를 생성하는 데스크톱 프로그램을 제작한다.
4단계 파이프라인(가사 생성 → 배경음악 생성 → 보컬 생성 → 음향 믹싱)을 Gradio UI로 구성해
비개발자도 쉽게 사용할 수 있는 실행 가능한 프로토타입을 완성한다.

## 범위
- **가사 생성**: Ollama + qwen2.5:7b 로컬 LLM으로 한국어 찬양 가사 생성
- **배경음악 생성**: HuggingFace `transformers` MusicGen(`facebook/musicgen-small`)으로 기악 생성
- **보컬 생성**: Bark(`suno/bark`) TTS 모델로 한국어 가사 음성 합성
- **음향 믹싱**: 보컬 + 배경음악 볼륨 조절 및 믹스, 옵션 리버브 처리
- **실행 환경**: Python 3.12 venv + CUDA 12.1 (NVIDIA GPU)
- **패키징**: `setup.bat` (의존성 설치), `실행.bat` (앱 구동)

## 단계

1. **환경 분석**
   - Python 버전 확인: 기본 설치된 Python 3.14가 PyTorch 미지원 확인
   - winget으로 Python 3.12 별도 설치 → `venv312` 가상환경 생성
   - Ollama 설치 확인 + `qwen2.5:7b` 모델 pull 완료 확인

2. **프로젝트 구조 설정**
   - 작업 폴더: `C:\Users\G\Downloads\찬양가생성기\`
   - `app.py` — Gradio 메인 앱
   - `setup.bat` — `venv312` pip 의존성 설치 스크립트
   - `실행.bat` — `venv312\Scripts\python.exe` 로 앱 구동
   - `output/` — 생성된 `music.wav`, `vocal.wav`, `final_song.wav` 저장

3. **Gradio UI 설계**
   - Gradio 6.x `Blocks` + `Tabs` 4단계 레이아웃
   - 각 단계 결과가 다음 단계 입력으로 자동 연동 (`lyrics_btn.click` → `outputs=[lyrics_output, vocal_lyrics]`)
   - `show_copy_button` 제거 (Gradio 6.x API 변경 대응)

4. **1단계 — 가사 생성 구현**
   - `ollama` Python 라이브러리로 `qwen2.5:7b` 호출
   - 입력: 주제, 분위기, 절 수, 추가 요청
   - 한국어 찬양 가사 프롬프트 엔지니어링

5. **2단계 — 배경음악 생성 구현**
   - `transformers.MusicgenForConditionalGeneration` + `AutoProcessor` 사용
   - `facebook/musicgen-small` 모델 (GPU 가속)
   - `max_new_tokens = int(duration) * 51 + 3` 으로 길이 제어
   - `audiocraft` 대신 `transformers` 채택 이유: `audiocraft`는 C++ Build Tools 요구

6. **3단계 — 보컬 생성 구현**
   - `suno-bark` 라이브러리의 `generate_audio()` 사용
   - 한국어 화자 프리셋: `v2/ko_speaker_4~7` (여성 부드러운/밝은, 남성 따뜻한/웅장한)
   - 가사 각 행을 `♪ 행1 ♪ 행2 ♪` 형식으로 감싸 노래 뉘앙스 부여

7. **4단계 — 음향 믹싱 구현**
   - `vocal.wav` + `music.wav` 로드 후 dtype 정규화
   - Bark 출력(float32 [-1,1])과 MusicGen 출력(int16) 분리 처리 → `to_float32()` 함수 신설
   - `scipy.signal.resample_poly`로 샘플레이트 통일 (Bark 24000 Hz → MusicGen 기준 맞춤)
   - 볼륨 슬라이더 + 선택적 리버브(scipy 컨볼루션) 적용
   - 최종 `final_song.wav` 저장

8. **버그 수정 및 검증**
   - 한국어 배치 파일 인코딩: PowerShell `WriteAllText` + CP949 인코딩으로 해결
   - 포트 충돌(7860): `Stop-Process` 로 기존 Python 프로세스 종료
   - 들여쓰기 오류(line 93): PowerShell 직접 라인 인덱스 교체로 수정
   - 보컬 무음 버그: dtype 미구분으로 float32를 int16처럼 나눠 발생 → `to_float32()` 로 해결
   - 성별 오매핑: ko_speaker 0~3 → 4~7 로 교체

## 기술 스택
| 역할 | 라이브러리 | 비고 |
|------|-----------|------|
| 로컬 LLM | `ollama` + `qwen2.5:7b` | 한국어 가사 생성 |
| 배경음악 | `transformers` MusicGen | `facebook/musicgen-small` |
| 보컬 합성 | `suno-bark` | 한국어 화자 v2/ko_speaker_4~7 |
| UI | `gradio` 6.x | Blocks + Tabs |
| 오디오 처리 | `scipy`, `soundfile` | 리샘플링, 믹싱, 저장 |
| GPU 가속 | `torch` cu121 | NVIDIA GPU |

## 관련 커밋
| SHA | 메시지 |
|-----|--------|
| (로컬 작업) | `feat: AI 찬양가 생성기 초기 구현 (4단계 파이프라인)` |

## 게이트 / 검증
- [x] **가사 생성** — Ollama qwen2.5:7b 한국어 가사 정상 출력
- [x] **배경음악 생성** — MusicGen GPU 가속 music.wav 생성 확인
- [x] **보컬 생성** — Bark 한국어 vocal.wav 생성 확인 (Gradio 오디오 플레이어 재생)
- [x] **믹싱** — dtype 정규화 + 리샘플링 후 final_song.wav 정상 생성
- [x] **자동 연동** — 1단계 가사가 3단계 입력에 자동 채워짐
- [ ] **보컬 음질** — Bark 한계로 TTS 수준. 실제 노래 합성은 로컬 SVC 모델로 추후 개선 예정

> **[미구현 항목 안내]** 위 체크리스트에서 - [ ]로 표시된 항목은
> 현재 미구현 상태이며, 회의 확정 또는 일정에 따라 추후 구현될 예정입니다.
> 해당 항목이 구현되면 - [x]로 변경하고 별도 워크플로우·리포트를 작성합니다.