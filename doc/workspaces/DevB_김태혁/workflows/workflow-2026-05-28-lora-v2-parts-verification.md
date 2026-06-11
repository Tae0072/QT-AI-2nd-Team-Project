# Workflow — 2026-05-28 lora-v2-parts-verification

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/lora-v2-parts` |
| 작업 패널 | 자체 LoRA v2 학습 결과 검증 + 부위별 PNG 생성 가능성 확인 |
| 기능 ID | F-10 |
| 기준 문서 | report-2025-05-28-lora-v2-parts-verification, FLUX LoRA 학습 가이드 |

## 작업 목표

- 자체 학습한 LoRA v2가 캐릭터 일관성을 유지하면서 다양한 자세를 출력하는지 검증한다.
- 베스트 idle 이미지를 베이스로 img2img에서 신체 부위만 다르게 추출 가능한지 실험한다.
- 부위 자동 컷팅 알고리즘 (자세 비례 기반)을 1차 시도해본다.
- 결과를 통해 "AI 부위 분리 + 엔진 리깅" 경로의 실현 가능성 판단.

## 수정 예정 경로

- `workflows/workflow_img2img.json` — denoise/guidance 값 변경 실험
- 자체 LoRA 학습 결과 (외부 fluxgym 출력) → `Data/Packages/ComfyUI/models/loras/qt_v2.safetensors`
- 검증 결과 정리: `report-2025-05-28-lora-v2-parts-verification.md`

## 검증 계획

- 같은 시드로 LoRA v1 / v2 비교 8장.
- 부위 자동 컷팅 스크립트로 15부위 PNG 추출 시도.
- 추출된 부위가 의미 단위 (머리/팔/다리)로 분리되는지 시각 확인.

## 막힌 점

- 학습 데이터 30장 중 일부 톤이 흔들려서 LoRA v2가 약간 흐릿함.
- 부위 자동 컷팅은 T-pose 가정 — 우리 캐릭터는 자연 자세라 어려움.
