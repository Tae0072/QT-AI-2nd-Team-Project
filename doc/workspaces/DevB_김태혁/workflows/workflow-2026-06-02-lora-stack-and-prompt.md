# Workflow — 2026-06-02 lora-stack-and-prompt

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/lora-stack` |
| 작업 패널 | LoRA 8개 다운로드 + 워크플로우 3종 작성 + 한국 QT 톤 프롬프트 |
| 기능 ID | F-12 |
| 기준 문서 | CivitAI LoRA 페이지, 한국 기독교 일러스트 레퍼런스 |

## 작업 목표

- CivitAI에서 한국 QT 일러스트 톤에 적합한 LoRA 후보 8개 다운로드.
- LoRA 조합 워크플로우 3종 작성 (premium / animation / img2img).
- 한국 어린이 성경 일러스트 톤 프롬프트 작성.
- 베스트 조합 + 베스트 프롬프트 발견을 위한 8장 배치 생성.

## 수정 예정 경로

- `workflows/workflow_premium.json` (Whimsical Storybook + Pastel Reverie + HandDawing)
- `workflows/workflow_animation.json` (Juaner_illustration + Line Warm + Children Story Book)
- `workflows/workflow_img2img.json` (자세 변형용 베이스 이미지 변형)
- LoRA 파일들 (ComfyUI/models/loras/):
  - illustration003.safetensors (Whimsical)
  - Pastel_Reverie.safetensors
  - HandDawing.safetensors ⚠️ 상업 X
  - J_flat_illustration.safetensors (Juaner)
  - Line_Warm_Illustration_Cartoon_Flux_LoRA.safetensors
  - Illustration_story book.safetensors (Children Story Book)
  - Storybook-v2-3250.safetensors
  - pp-storybook_rank2_bf16.safetensors (Vivid Impressions)

## 검증 계획

- 두 가지 워크플로우 (premium/animation)로 같은 프롬프트 8장씩 비교.
- 4가지 평가 기준 (스타일·리깅·일관성·어린이 적합) 25점 만점.
- 16점 이상 통과한 이미지가 4장 이상인지 확인.

## 막힌 점

- HandDawing은 작가 본인 작품 학습 — 상업 사용 금지 명시. 출시 전 자체 LoRA로 대체 필요.
- LoRA 조합 강도 조절 (0.5~1.0) 시행착오 필요.
- FluxGuidance 3.5가 기본 — 5.0 이상 가면 이미지 깨짐 확인.
