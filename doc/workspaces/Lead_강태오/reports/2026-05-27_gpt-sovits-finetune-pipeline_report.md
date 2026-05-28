# 2026-05-27 GPT-SoVITS 파인튜닝 및 자동화 파이프라인 구축 결과 보고

## 요약
GPT-SoVITS v2pro의 파인튜닝을 팀원 2명(이승욱, 김지민)에 대해 완료했다.
pretrained 모델 대비 목소리 유사도가 크게 향상되었다.
녹음 후 자동으로 파인튜닝이 실행되는 698줄의 `auto_finetune.py` 모듈을 구축하고,
앱에 통합하여 녹음 → 전처리 → 학습 → 모델 등록까지 원클릭으로 처리되도록 했다.
녹음 가이드를 14문장(2~3분)으로 확장하고, UX를 개선했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `bible-tts/auto_finetune.py` | 자동 파인튜닝 파이프라인 (698줄) |
| `bible-tts/gpt_sovits_engine.py` | 모델 전환 기능 추가 (335줄) |
| `bible-tts/tts_app.py` | 자동 파인튜닝 통합 + 가이드 업데이트 |
| `GPT-SoVITS/SoVITS_weights_v2/leeseungwook_e8_s208.pth` | 이승욱 SoVITS 가중치 (81MB) |
| `GPT-SoVITS/GPT_weights_v2/leeseungwook-e15.ckpt` | 이승욱 GPT 가중치 (148MB) |
| `GPT-SoVITS/SoVITS_weights_v2/kimjimin_e8_s208.pth` | 김지민 SoVITS 가중치 (81MB) |
| `GPT-SoVITS/GPT_weights_v2/kimjimin-e15.ckpt` | 김지민 GPT 가중치 (148MB) |
| `GPT-SoVITS/logs/leeseungwook/` | 이승욱 전처리 아티팩트 |
| `GPT-SoVITS/logs/kimjimin/` | 김지민 전처리 아티팩트 |

## 아키텍처

### 파인튜닝 파이프라인 흐름
```
녹음 원본 (voices/{이름}_recording.wav)
       ↓
① 서버 중지 (GPU 확보)
       ↓
② 오디오 슬라이싱 (slicer2) → logs/{name}/raw_sliced/
       ↓
③ ASR (Faster Whisper large-v3) → 2-name2text.txt
       ↓
④ BERT 특징 추출 → 3-bert/ (중국어만, 한국어 skip)
       ↓
⑤ HuBERT 특징 추출 → 4-cnhubert/
       ↓
⑥ WAV 리샘플링 (32kHz) → 5-wav32k/
       ↓
⑦ Semantic 토큰 추출 → 6-name2semantic.tsv
       ↓
⑧ SoVITS 학습 (8 epochs) → SoVITS_weights_v2/{name}_e8_s*.pth
       ↓
⑨ GPT 학습 (15 epochs) → GPT_weights_v2/{name}-e15.ckpt
       ↓
⑩ 모델 등록 (gpt_sovits_engine.py에 엔트리 추가)
       ↓
⑪ 서버 재시작
```

### 음성 생성 흐름 (파인튜닝 후)
```
tts_app.py → gpt_sovits_engine.py
                ├── _switch_model(voice_name)
                │     ├── FINETUNED_MODELS에 있으면 → 전용 모델 로드
                │     └── 없으면 → pretrained 모델 로드
                ├── _prepare_ref_audio() → 3~8초 참조 음성
                └── generate_speech() → POST /tts → WAV 출력
```

## 구현 상세

### 자동 파인튜닝 모듈 (`auto_finetune.py`)
녹음 파일 하나만 있으면 전처리부터 학습, 모델 등록까지 자동으로 수행하는
독립 모듈이다. 11개 내부 함수로 구성되며, 각 단계에 skip 로직이 있어
중간에 실패해도 재실행 시 이미 완료된 단계를 건너뛴다.

주요 설계 결정:
- **환경 변수 기반**: GPT-SoVITS 전처리 스크립트가 `os.environ.get()`으로 설정을 받으므로, `subprocess.Popen`에 `env` 딕셔너리를 전달하는 방식 사용
- **bundled Python 사용**: GPT-SoVITS v2pro의 `runtime/python.exe` (Python 3.9.13)을 직접 호출하여 의존성 충돌 방지
- **서버 중지/재시작**: 파인튜닝 시 GPU VRAM이 필요하므로 서버를 중지하고, 완료 후 재시작
- **모델 자동 등록**: 정규식으로 `gpt_sovits_engine.py`의 `FINETUNED_MODELS` 딕셔너리에 새 엔트리를 삽입

### 모델 전환 (`_switch_model`)
목소리별로 파인튜닝된 모델이 다르므로, 음성 생성 전에 올바른 모델을 로드해야 한다.
`_switch_model()`은 API 서버의 `/set_sovits_weights`와 `/set_gpt_weights`를 호출하여
모델을 전환한다. 이미 같은 모델이 로드되어 있으면 전환을 건너뛴다.

### 학습 설정
| 항목 | SoVITS | GPT |
|------|--------|-----|
| Epochs | 8 | 15 |
| Batch size | 4 | 4 |
| 정밀도 | fp16 | fp16-mixed |
| 저장 주기 | 매 epoch | 5 epoch |
| GPU | 3060 Ti 8GB | 3060 Ti 8GB |
| 학습 시간 | ~2분 | ~3분 |

### 녹음 가이드 (14문장, 2~3분)
다양한 어조를 포함하여 음성 특성을 최대한 포착하도록 설계했다:
- 서술문: 성경 내용 설명
- 감탄문: 감정 표현
- 기도문: 차분한 어조
- 대화체: 일상적 톤

## 발생한 문제와 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| ASR `UnicodeEncodeError` (cp949) | Faster Whisper가 중국어 로그 출력 | 오류 무시, 출력 파일 정상 확인 |
| BERT 스크립트 CLI 인수 안 됨 | `os.environ.get()` 방식 사용 | 환경 변수로 전환 |
| BERT `3-bert/` 비어있음 | 한국어는 BERT 특징 미사용 (정상) | skip 로직으로 처리 |
| HuBERT `inp_text` 누락 | 환경 변수 누락 | `inp_text` 환경 변수 추가 |
| SoVITS config UTF-8 BOM | PowerShell 기본 `UTF8` 인코딩이 BOM 포함 | `UTF8Encoding($false)` (BOM 없음) 사용 |
| `2-name2text-0.txt` 파일명 불일치 | 학습 스크립트가 `2-name2text.txt` 기대 | 파티션 접미사 없는 이름으로 복사 |
| `6-name2semantic-0.tsv` 동일 문제 | 학습 스크립트가 `6-name2semantic.tsv` 기대 | 파티션 접미사 없는 이름으로 복사 |
| 김지민 이름 더블스페이스 | 파일명에 공백 2개 (`김지민  ㅠ`) | FINETUNED_MODELS 키를 실제 파일명과 일치시킴 |
| PowerShell Start-Process 타임아웃 | GPT 학습 백그라운드 프로세스 관리 어려움 | `schtasks` 즉시 실행으로 대체 |

## 검증
- [x] 이승욱 파인튜닝 모델 → 음성 생성 성공, 유사도 높음
- [x] 김지민 파인튜닝 모델 → 음성 생성 성공, 유사도 높음
- [x] 모델 자동 전환 정상 (파인튜닝 → pretrained → 파인튜닝)
- [x] `auto_finetune.py` 코드 리뷰 완료
- [x] `_register_model()` 정규식 모델 등록 동작 확인
- [x] 녹음 후 자동 파인튜닝 트리거 동작 확인
- [x] 14문장 가이드 적용 및 읽기 시간 검증
- [x] 기본 텍스트 플레이스홀더 변경 확인
- [ ] 새 팀원 목소리로 자동 파인튜닝 End-to-End 실행 테스트 (예정)

## 한계 및 향후 과제
- **한국어 발음**: GPT-SoVITS pretrained 모델이 영어/중국어 위주라 한국어 발음이 가끔 부자연스러움
- **녹음 품질 의존**: 마이크 품질, 배경 소음에 따라 클로닝 결과 차이 큼
- **학습 시간**: 현재 ~5분이지만 녹음 길이 증가 시 비례 증가
- **VRAM 제약**: 3060 Ti 8GB에서 서버와 학습 동시 불가 → 서버 중지 필요
- **향후**: 한국어 특화 파인튜닝 데이터 확보, 녹음 품질 가이드라인 작성


> **[미구현 항목 안내]** 위 체크리스트에서 - [ ]로 표시된 항목은
> 현재 미구현 상태이며, 회의 확정 또는 일정에 따라 추후 구현될 예정입니다.
> 해당 항목이 구현되면 - [x]로 변경하고 별도 워크플로우·리포트를 작성합니다.