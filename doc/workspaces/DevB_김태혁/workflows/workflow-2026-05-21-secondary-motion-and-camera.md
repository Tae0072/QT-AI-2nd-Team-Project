# Workflow — 2026-05-21 secondary-motion-and-camera

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/secondary-camera` |
| 작업 패널 | 2차 모션 (옷자락·머리·수염 늦은 흔들림) + 카메라 시스템 |
| 기능 ID | F-05 |
| 기준 문서 | Spine/DragonBones 2차 모션 참고, Disney follow-through |

## 작업 목표

- 캐릭터가 움직일 때 옷자락/머리카락/수염이 약간 늦게 따라오는 follow-through 효과를 구현한다.
- `secondaryMotion`은 이전 프레임 포즈와 현재 프레임 포즈의 속도차로 계산.
- 카메라 시스템 — `static / pan-left / pan-right / zoom-in / zoom-out` 5종 + 흔들림(shake) 키프레임.
- 카메라 흔들림은 감쇠 곡선 3종 (`linear / exponential / none`) 선택 가능.

## 수정 예정 경로

- `src/animation/secondaryMotion.ts` (computeSecondaryMotion, applySecondaryMotion)
- `src/camera/Camera.tsx` (Pan/zoom transform + 흔들림)
- `src/types.ts`에 `CameraType`, `CameraConfig`, `ShakeKeyframe` 추가
- `src/characters/drawing.ts`에 `secondaryMotion` 인자 통합

## 검증 계획

- walk 사이클이 빠르게 좌우 움직일 때 옷자락이 늦게 따라오는지 시각 확인.
- 빠르게 정지 시 머리카락이 살짝 앞으로 쏠리는 follow-through.
- pan-left 카메라로 씬을 천천히 훑기.
- shake 키프레임으로 천둥/지진 효과.

## 막힌 점

- 2차 모션 강도(SecondaryMotion 값)를 너무 크게 하면 어색 — 최대 ±8px 정도로 제한.
- 카메라 흔들림이 영상 압축 시 모기 노이즈 발생 가능 — 낮은 frequency 권장.
