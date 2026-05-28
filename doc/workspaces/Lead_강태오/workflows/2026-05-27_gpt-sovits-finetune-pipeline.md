# 2026-05-27 GPT-SoVITS 파인튜닝 및 자동화 파이프라인 구축

## 목표
GPT-SoVITS pretrained 모델의 목소리 복제 품질을 개인 맞춤형으로 끌어올리기 위해
팀원별 파인튜닝을 수행하고, 녹음 후 자동으로 파인튜닝이 실행되는
자동화 파이프라인을 구축한다. 또한 녹음 가이드 문장 업데이트와
앱 UX 개선을 진행한다.

## 범위
- **이승욱 파인튜닝**: GPT-SoVITS v2pro 수동 파인튜닝 (첫 번째 사례)
- **김지민 파인튜닝**: 스크립트 기반 파인튜닝 (두 번째 사례)
- **자동 파인튜닝 모듈**: `auto_finetune.py` 698줄 — 녹음 → 전처리 → 학습 → 모델 등록 자동화
- **앱 통합**: 녹음 완료 시 자동 파인튜닝 백그라운드 실행
- **모델 전환 구현**: 목소리별 파인튜닝 모델 자동 로드 (`_switch_model`)
- **녹음 가이드 업데이트**: 2~3분 분량 14문장 성경 테마 가이드
- **기본 텍스트 변경**: 음성 생성 입력창 플레이스홀더 변경

## 단계

1. **이승욱 수동 파인튜닝 (첫 사례)**
   - GPT-SoVITS v2pro WebUI에서 수동 전처리 및 학습
   - ASR → BERT → HuBERT → Semantic → SoVITS 학습 → GPT 학습
   - SoVITS: 8 epochs, batch 4 → `leeseungwook_e8_s208.pth` (81MB)
   - GPT: 15 epochs, batch 4 → `leeseungwook-e15.ckpt` (148MB)
   - 결과: pretrained 대비 목소리 유사도 대폭 향상

2. **모델 전환 기능 구현 (`gpt_sovits_engine.py`)**
   - `FINETUNED_MODELS` 딕셔너리: 목소리 이름 → (GPT 가중치, SoVITS 가중치) 매핑
   - `_switch_model()`: API `/set_gpt_weights`, `/set_sovits_weights` 호출로 모델 전환
   - `_current_model` 추적: 이미 로드된 모델이면 전환 건너뛰기
   - 파인튜닝 없는 목소리는 기본 pretrained 모델 사용

3. **GPT-SoVITS 전처리 스크립트 분석**
   - 각 스크립트가 CLI 인수가 아닌 `os.environ.get()`으로 설정을 받는 구조 파악
   - 필요 환경 변수: `inp_text`, `inp_wav_dir`, `opt_dir`, `bert_pretrained_dir` 등
   - 스크립트 간 의존 관계: slice → ASR → BERT → HuBERT → Semantic → Train
   - 파일명 규칙: `2-name2text.txt`, `6-name2semantic.tsv` (파티션 접미사 없음)

4. **김지민 스크립트 기반 파인튜닝 (두 번째 사례)**
   - PowerShell에서 환경 변수 설정 후 각 전처리 스크립트 순차 실행
   - ASR(Faster Whisper): 9개 슬라이스 → `2-name2text-0.txt` 생성
   - BERT: 한국어이므로 `3-bert/` 비어있음 (정상 — 중국어만 해당)
   - HuBERT: `4-cnhubert/` 특징 추출
   - Semantic: `6-name2semantic-0.tsv` 생성
   - SoVITS 학습: 8 epochs → `kimjimin_e8_s208.pth`
   - GPT 학습: 15 epochs → `kimjimin-e15.ckpt`

5. **자동 파인튜닝 모듈 작성 (`auto_finetune.py`)**
   - 698줄, 11개 내부 함수로 구성된 완전 자동화 파이프라인
   - `run_finetune(voice_name)` 엔트리 포인트
   - 파이프라인: 서버 중지 → 오디오 슬라이싱 → ASR → BERT → HuBERT
     → Semantic → 학습 파일 준비 → SoVITS 학습 → GPT 학습 → 모델 등록 → 서버 재시작
   - 각 단계에 skip 로직: 이미 출력 파일이 있으면 건너뛰기
   - `_voice_to_dirname()`: 파일시스템 안전한 디렉토리 이름 변환
   - `_register_model()`: 정규식으로 `gpt_sovits_engine.py`에 모델 엔트리 자동 추가
   - `_run_step()`: 인코딩 오류 처리, 타임아웃, stderr 캡처

6. **앱 통합 (`tts_app.py`)**
   - `_start_auto_finetune()` 함수 추가
   - `register_voice()` 완료 후 백그라운드 스레드에서 파인튜닝 자동 시작
   - 파인튜닝 완료 후 `importlib.reload(gpt_sovits_engine)`으로 모델 매핑 갱신
   - 로그에 진행 상태 실시간 표시

7. **녹음 가이드 문장 업데이트**
   - 기존: 짧은 예시 1~2문장
   - 변경: 14문장, 2~3분 분량, 성경 테마 콘텐츠
   - 다양한 어조(서술/감탄/기도/대화체)로 음성 특성 포착 극대화

8. **기본 텍스트 변경**
   - 기존: `"하나님이 세상을 이처럼 사랑하사 독생자를 주셨으니"`
   - 변경: `"내용을 입력해주세요"` (플레이스홀더)

## 기술 스택
| 역할 | 라이브러리 | 비고 |
|------|-----------|------|
| 음성 클로닝 | GPT-SoVITS v2pro | HTTP API 서버 + 파인튜닝 |
| ASR | Faster Whisper (large-v3) | 음성 → 텍스트 전사 |
| 특징 추출 | HuBERT (chinese-hubert-base) | 음성 특징 벡터 |
| Semantic 추출 | GPT-SoVITS 내장 | 의미 토큰 추출 |
| 학습 | PyTorch + CUDA | SoVITS (8 epochs) + GPT (15 epochs) |
| 자동화 | subprocess + 환경 변수 | `auto_finetune.py` |
| GUI | CustomTkinter | 녹음 → 자동 파인튜닝 통합 |

## 게이트 / 검증
- [x] 이승욱 파인튜닝 모델 음성 생성 테스트 통과
- [x] 김지민 파인튜닝 모델 음성 생성 테스트 통과
- [x] 모델 자동 전환 (`_switch_model`) 정상 동작
- [x] `auto_finetune.py` 전체 파이프라인 로직 검증
- [x] `_register_model()` 정규식 모델 등록 동작 확인
- [x] 녹음 → 자동 파인튜닝 백그라운드 실행 흐름 확인
- [x] 14문장 녹음 가이드 적용 확인
- [x] 기본 텍스트 플레이스홀더 변경 확인
- [ ] 새로운 팀원 목소리로 자동 파인튜닝 End-to-End 테스트 (예정)


> **[미구현 항목 안내]** 위 체크리스트에서 - [ ]로 표시된 항목은
> 현재 미구현 상태이며, 회의 확정 또는 일정에 따라 추후 구현될 예정입니다.
> 해당 항목이 구현되면 - [x]로 변경하고 별도 워크플로우·리포트를 작성합니다.