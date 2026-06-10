# Report — 2026-06-01 comfyui-flux-setup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/comfyui-setup` |
| 작업 패널 | ComfyUI + FLUX.1 Dev FP8 + 텍스트 인코더 + VAE 셋업 |
| PR | 로컬 환경 — 레포 변경 없음 |
| 최종 상태 | DONE |

## 변경 내용

- **Stability Matrix** 설치 (Windows): https://lykos.ai
- **ComfyUI 패키지** 추가 (Stability Matrix의 "Packages" 메뉴).
  - Python 3.10.11 선택 (3.10.19보다 호환성 좋음)
  - pip 미러 Kakao로 변경 (`%APPDATA%/pip/pip.ini`)
- **다운로드 완료** (총 ~24GB):
  - `flux1-dev-fp8.safetensors` (12GB, Kijai 양자화) → `models/unet/`
  - `t5xxl_fp16.safetensors` (9.5GB) → `models/clip/`
  - `clip_l.safetensors` (240MB) → `models/clip/`
  - `ae.safetensors` (320MB, Flux VAE) → `models/vae/`
- Stability Matrix가 중앙 `Data/Models/` 폴더에 받은 후 ComfyUI로 심볼릭 링크 — 일부 미연결 시 수동 복사로 보완.
- **첫 이미지 생성**: FLUX 기본 워크플로우 (PNG 메타 드래그&드롭)로 1024×1024 이미지 1장 정상 출력 확인.

## 검증 결과

- ComfyUI 기동: PASS (`http://127.0.0.1:8188`)
- 첫 1장 생성 시간: 약 90초 @ 20 steps
- VRAM 피크: 7.8GB / 8GB (T5XXL 단계에서 OOM 직전)
- 두 번째 생성부터 캐싱으로 30~60초

## 확인한 금지선

- 모든 모델 파일은 로컬 보관 — 레포에 커밋하지 않음 (Git LFS 미사용)
- BFL 라이선스 명시 — 비상업 (개발 단계만)
- 외부 모델 사용 시 출처 기록

## 남은 리스크

- VRAM 8GB가 한계 — 큰 LoRA 여러 개 동시 사용 시 OOM 가능.
- T5XXL FP16은 무거움 — 추후 FP8 변환 검토 (품질 trade-off).
- ComfyUI Manager 미설치 — 커스텀 노드 설치 시 수동.

## 회고

오늘은 "AI 도구가 우리 손에 들어온 날". RTX 3060 Ti 8GB에서 FLUX가 돌아간다는 게 검증됐다. FP8 양자화 덕분에 가능. 다음에는 LoRA로 한국 어린이 톤을 입히는 작업.
