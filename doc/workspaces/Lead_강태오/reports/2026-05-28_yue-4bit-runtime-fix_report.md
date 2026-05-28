# 2026-05-28 YuE 4-bit 런타임 버그 수정 및 가사 엔진 교체 결과 보고

## 요약
3060 Ti 8GB에서 YuE 노래 생성을 처음 실행하는 과정에서 6개의 연속 오류가 발생했다.
각 오류를 순서대로 수정했으며, GPU 82% 사용·VRAM 5.7GB로 YuE가 실제 생성을
시작하는 것을 확인했다. 추가로 가사 엔진을 qwen2.5:7b → gemma3:12b로 교체해
한국어 출력 품질을 개선했다.

## 산출물
| 파일 | 변경 내용 |
|------|-----------|
| `C:\SongGen\YuE\inference\xcodec_mini_infer\` | HuggingFace 신규 클론 (1.75GB, LFS 포함) |
| `C:\SongGen\YuE\inference\xcodec_mini_infer\semantic_ckpts\hf_1_325000\model.safetensors` | pytorch_model.bin → safetensors 변환 (211키) |
| `C:\SongGen\YuE\inference\infer_4bit.py` | _is_local_path() 추가, 로컬 모델 4-bit 제외 |
| `C:\SongGen\YuE\inference\infer.py` | genre_txt / lyrics_txt open() encoding="utf-8" 추가 |
| `C:\SongGen\app.py` | LYRICS_SYSTEM 한국어 강제, 모델 gemma3:12b 교체 |

## 발생한 문제와 해결

| 순서 | 오류 | 원인 | 해결 |
|------|------|------|------|
| 1 | `ModuleNotFoundError: No module named 'models'` | xcodec_mini_infer가 git submodule이었으나 .gitmodules 없어 초기화 불가 | `rm -rf xcodec_mini_infer` 후 `git clone https://huggingface.co/m-a-p/xcodec_mini_infer` |
| 2 | `ModuleNotFoundError: No module named 'sentencepiece'` | mmtokenizer 의존성 누락 | `pip install sentencepiece` |
| 3 | `ValueError: Due to a serious vulnerability issue in torch.load... upgrade torch to at least v2.6` | transformers CVE-2025-32434 패치가 torch<2.6 차단. cu121 인덱스는 2.5.1까지만 제공 | semantic_ckpts/pytorch_model.bin → model.safetensors 변환 |
| 4 | `size mismatch for semantic_model.encoder.layers.*.weight: [768,768] vs [294912,1]` | infer_4bit.py의 from_pretrained 패치가 로컬 경로 모델(semantic_model)에도 NF4 4-bit 적용 | `_is_local_path()` 함수로 로컬 경로 감지 → 원본 FP 로드 |
| 5 | `UnicodeDecodeError: 'cp949' codec can't decode byte 0xec` | infer.py가 lyrics.txt를 cp949(Windows 기본)로 열려다 UTF-8 파일 만남 | `open(args.genre_txt, encoding="utf-8")` / `open(args.lyrics_txt, encoding="utf-8")` |
| 6 | 가사 생성 결과가 중국어 | qwen2.5:7b가 system 프롬프트 무시하고 중국어 출력 | LYRICS_SYSTEM에 한국어 전용 지시 추가 + gemma3:12b 교체 |

## 핵심 코드 변경

### _is_local_path() — 4-bit 적용 범위 제한
```python
def _is_local_path(name_or_path):
    s = str(name_or_path)
    if s.startswith(".") or s.startswith("/") or s.startswith("\\"):
        return True
    if len(s) >= 2 and s[1] == ":":   # C:\... 형태
        return True
    if "\\" in s:                      # Windows 경로 구분자
        return True
    if os.path.isdir(s):               # 실제로 존재하는 디렉토리
        return True
    return False

# HuggingFace ID (m-a-p/YuE-*) → 4-bit 적용
# 로컬 경로 (./xcodec_mini_infer/...) → 원본 FP 로드
```

### pytorch_model.bin → safetensors 변환
```python
import torch
from safetensors.torch import save_file
sd = torch.load(bin_path, map_location="cpu", weights_only=False)
sd_clean = {k: v.contiguous() for k, v in sd.items() if isinstance(v, torch.Tensor)}
save_file(sd_clean, st_path)  # 211개 키, transformers가 .safetensors 우선 로드
```

## 검증 결과
- **GPU 상태**: nvidia-smi → 82% 사용률, 5,753 MiB / 8,192 MiB VRAM
- **프로세스**: Python PID 24704 CPU 누적 7077초, WorkingSet 394MB (GPU 오프로드)
- **gemma3:12b**: `ollama list` 확인 → 8.1GB 등록 완료

## 한계 및 향후 과제
- **생성 속도**: 3060 Ti 8GB + 4-bit NF4 + SDPA(flash_attention_2 대체)로
  RTX 4090 대비 3~5배 느림. 2세그먼트 기준 1~3시간 예상
- **torch 버전**: cu121 인덱스 최대 2.5.1 → safetensors 우회로 해결했으나
  장기적으로 cu124 기반 torch 2.6+ 업그레이드 권장
- **gemma3:12b VRAM**: 가사 생성 시 추가 ~6GB VRAM 사용 가능성 → YuE 생성 중
  Ollama가 동시 실행되지 않도록 순차 실행 구조 유지 필요