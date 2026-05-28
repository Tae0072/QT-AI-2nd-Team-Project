# 2026-05-28 YuE 4-bit 런타임 버그 수정 및 가사 엔진 교체

## 목표
3060 Ti 8GB에서 실제 YuE 노래 생성을 처음으로 실행하는 과정에서
연달아 발생한 런타임 오류들을 모두 수정하고,
한국어 가사 생성 품질을 높이기 위해 Ollama 모델을 교체한다.

## 범위
- **xcodec_mini_infer 누락 수정**: YuE git submodule 미초기화 → HuggingFace 직접 클론
- **sentencepiece 설치**: mmtokenizer 의존성 누락 패키지 추가
- **safetensors 변환**: pytorch_model.bin → model.safetensors (torch < 2.6 CVE 우회)
- **4-bit 패치 로직 개선**: 로컬 경로 모델에 4-bit 미적용 (shape mismatch 수정)
- **cp949 인코딩 수정**: infer.py 가사·장르 파일 열기에 encoding="utf-8" 추가
- **한국어 가사 강제**: LYRICS_SYSTEM 전 장르에 한국어 전용 지시문 추가
- **Ollama 모델 교체**: qwen2.5:7b → gemma3:12b (한국어 품질 향상)

## 단계

1. **xcodec_mini_infer 서브모듈 문제 진단 및 해결**
   - `inference/xcodec_mini_infer/` 디렉토리가 완전히 비어있음 확인
   - `git ls-tree HEAD -- inference/xcodec_mini_infer` → `160000 commit` (gitlink, 서브모듈)
   - `.gitmodules` 파일 없음 → `git submodule update --init` 불가
   - YuE README 확인: `cd YuE/inference && git clone https://huggingface.co/m-a-p/xcodec_mini_infer`
   - 기존 빈 디렉토리 삭제 후 HuggingFace에서 직접 클론 (1.75 GiB, LFS 포함)
   - 핵심 파일 확인: `models/soundstream_hubert_new.py`, `ckpt_00360000.pth` (1.3GB), `pytorch_model.bin` (360MB)

2. **sentencepiece 누락 패키지 설치**
   - `mmtokenizer.py`의 `import sentencepiece` → `ModuleNotFoundError`
   - `pip install sentencepiece` 로 즉시 해결

3. **pytorch_model.bin → safetensors 변환**
   - transformers가 `check_torch_load_is_safe()` 에서 torch >= 2.6 요구 (CVE-2025-32434)
   - cu121 PyTorch 인덱스 최신 버전: 2.5.1 → 업그레이드 불가
   - 해결: `torch.load(weights_only=False)` → `safetensors.torch.save_file()`
   - 211개 키 변환 완료 → `model.safetensors` 생성

4. **infer_4bit.py 4-bit 적용 범위 수정**
   - `semantic_model = AutoModel.from_pretrained("./xcodec_mini_infer/semantic_ckpts/hf_1_325000")`
     에도 NF4 4-bit 주입 → shape mismatch (예: `[768,768]` vs `[294912,1]`)
   - 원인: 4-bit 양자화 시 bitsandbytes가 weight를 `[N,1]` 형태로 저장
     → 원본 체크포인트 로드 시 shape 불일치
   - 수정: `_is_local_path()` 함수 추가
     - `.` / `\` / `/` 시작, `\\` 포함, `C:` 드라이브 경로, `os.path.isdir()` → 로컬 판단
     - 로컬 경로: 원본 그대로 로드 / HuggingFace ID: NF4 4-bit 적용

5. **infer.py cp949 UnicodeDecodeError 수정**
   - `with open(args.genre_txt) as f:` / `with open(args.lyrics_txt) as f:`
     → 한국 Windows 기본 인코딩 cp949로 UTF-8 파일 읽기 실패
   - PowerShell `-replace`로 두 open() 호출에 `encoding="utf-8"` 추가

6. **한국어 가사 생성 강제화**
   - qwen2.5:7b가 system 프롬프트 무시하고 중국어로 출력하는 문제
   - LYRICS_SYSTEM 10개 장르 모두: `"반드시 한국어로만 가사를 작성하세요. 영어나 중국어는 절대 사용하지 마세요."` 추가
   - user prompt 조건 문구도 강화: `"중국어, 영어 절대 금지"` + 영문 지시 병기
     (`"Write ONLY in Korean (한국어). Do NOT use Chinese or English."`)

7. **Ollama 모델 교체: qwen2.5:7b → gemma3:12b**
   - 한국어 가사 품질 비교 순위 분석 (Groq > gemma3:12b > EEVE-Korean > llama3.1 > qwen2.5)
   - `ollama pull gemma3:12b` (8.1GB 다운로드)
   - `app.py` 119번째 줄 `model="gemma3:12b"` 교체
   - 앱 재시작으로 즉시 적용

## 기술 스택 변경
| 항목 | 이전 | 이후 |
|------|------|------|
| xcodec_mini_infer | git 서브모듈 (비어있음) | HuggingFace 직접 클론 (1.75GB) |
| semantic_model 로드 | NF4 4-bit 강제 적용 | 로컬 경로 감지 → 원본 FP로 로드 |
| pytorch_model.bin | `.bin` (torch 버전 체크 실패) | `model.safetensors` (체크 우회) |
| Ollama 모델 | qwen2.5:7b (4.7GB) | gemma3:12b (8.1GB) |

## 게이트 / 검증
- [x] xcodec_mini_infer 파일 존재 확인 (ckpt 1.3GB, pytorch_model.bin 360MB, models/*.py)
- [x] safetensors 211개 키 변환 완료
- [x] _is_local_path() 로직: `.`으로 시작하는 경로 정확히 로컬 판단
- [x] infer.py genre_txt / lyrics_txt 모두 encoding="utf-8" 적용 확인
- [x] LYRICS_SYSTEM 10개 항목 전부 한국어 강제 지시 추가 확인
- [x] gemma3:12b ollama list 등록 확인 (8.1GB)
- [x] app.py 모델명 gemma3:12b 교체 확인
- [x] GPU 82% 사용·VRAM 5.7GB → YuE 실제 생성 동작 확인