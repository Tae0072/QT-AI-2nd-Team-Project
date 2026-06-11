# Report — 2026-06-02 lora-stack-and-prompt

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/lora-stack` |
| 작업 패널 | LoRA 8개 + 워크플로우 3종 + 한국 QT 프롬프트 |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- **LoRA 8개 다운로드 완료** (`ComfyUI/models/loras/`):
  - illustration003 (Whimsical Storybook, rank 2 bf16)
  - Pastel_Reverie
  - HandDawing ⚠️ 상업 X
  - J_flat_illustration (Juaner)
  - Line_Warm_Illustration_Cartoon_Flux_LoRA
  - Illustration_story book
  - Storybook-v2-3250
  - pp-storybook_rank2_bf16 (Vivid Impressions)
- **워크플로우 3종 작성** (`workflows/`):
  - `workflow_premium.json` — Whimsical 0.9 + Pastel 0.5 + HandDawing 0.4 (고퀄 톤)
  - `workflow_animation.json` — Juaner 0.8 + LineWarm 0.5 + StoryBook 0.3 (구조 친화)
  - `workflow_img2img.json` — VAEEncode + LoadImage 추가, denoise 0.55~0.6 기본
- **한국 QT 톤 프롬프트** 작성:
  - 시그니처: `wearing white robe with diagonal red sash and mint green collar`
  - 톤: `Korean Christian devotional illustration, soft pastel palette, simple minimalist face with small dot eyes`
- 두 워크플로우로 8장 배치 생성, 비교.
- **Premium 조합**이 한국 어린이 책 톤에 가장 근접 — 베스트 조합 확정.

## 검증 결과

- premium 조합 8장 중 6장이 16점 이상 통과
- animation 조합 8장 중 3장만 통과 (캐릭터 일관성 약함)
- 같은 시드로 두 조합 비교 시 premium의 손그림 질감이 우세
- 첫 베스트 idle 예수 발견: 흰 로브 + 빨간 띠 + 갈색 머리 + 친근한 얼굴

## 확인한 금지선

- FluxGuidance 3.5 유지 (5.0 이상 금지 — 이미지 깨짐 검증됨)
- KSampler CFG 1.0 고정 (FLUX 규칙)
- HandDawing은 시제품만 사용, 출시 시 자체 LoRA로 교체

## 남은 리스크

- HandDawing 상업 금지 — 출시 전 fluxgym으로 자체 LoRA 학습 필요.
- LoRA 조합 강도가 캐릭터마다 다르게 작동할 수 있음 — 다른 캐릭터(모세 등) 추가 시 재조정 필요.

## 회고

오늘은 "우리 톤이 보인 날". 8장 중 1장이 한국 어린이 큐티 일러스트의 결을 정확히 잡았다. premium 조합 (Whimsical + Pastel + HandDawing)이 답. 다음 단계는 이 캐릭터를 엔진에 통합하는 일.
