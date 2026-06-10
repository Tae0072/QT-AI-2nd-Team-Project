# Workflow — 2026-06-01 comfyui-flux-setup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/comfyui-setup` |
| 작업 패널 | Stability Matrix + ComfyUI + FLUX.1 Dev FP8 셋업 |
| 기능 ID | F-11 |
| 기준 문서 | ComfyUI 공식 문서, FLUX 모델 카드 |

## 작업 목표

- 로컬에 ComfyUI 환경을 구축하고 FLUX.1 Dev FP8 양자화 모델을 동작시킨다.
- RTX 3060 Ti 8GB VRAM에서 FP8로 메모리 한계 회피.
- 텍스트 인코더 (T5XXL FP16 + CLIP-L) + VAE (ae.safetensors) 셋업.
- 첫 텍스트 → 이미지 생성 테스트.

## 수정 예정 경로

- 로컬 (이 레포 밖):
  - `StabilityMatrix-win-x64/Data/Packages/ComfyUI/models/unet/flux1-dev-fp8.safetensors`
  - `StabilityMatrix-win-x64/Data/Packages/ComfyUI/models/clip/t5xxl_fp16.safetensors`
  - `StabilityMatrix-win-x64/Data/Packages/ComfyUI/models/clip/clip_l.safetensors`
  - `StabilityMatrix-win-x64/Data/Packages/ComfyUI/models/vae/ae.safetensors`
- 레포에는 변경 없음 (외부 도구 셋업)

## 검증 계획

- ComfyUI 기동 → 브라우저 `http://127.0.0.1:8188` 접근.
- 기본 FLUX 워크플로우로 텍스트 → 1024×1024 이미지 생성.
- 생성 시간 측정 (RTX 3060 Ti 기준).
- VRAM 사용량 모니터링 (8GB 초과 시 OOM).

## 막힌 점

- 모델 파일 크기 합 약 24GB — 다운로드 시간 1~2시간 예상.
- T5XXL은 9.5GB로 8GB VRAM 초과 — CPU offload 또는 FP8 변환 필요.
- pip 미러를 Kakao로 변경해야 다운로드 안정성 확보.
