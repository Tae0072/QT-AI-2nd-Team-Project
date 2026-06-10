# Workflow — 2026-05-15 animation-engine-core

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/animation-core` |
| 작업 패널 | 애니메이션 엔진 코어 — Pose 타입 + 보간 + 11개 액션 정의 |
| 기능 ID | F-01 |
| 기준 문서 | `STYLE_GUIDE.md`, Disney 12 animation principles |

## 작업 목표

- 16관절 Pose 타입을 확정하고 모든 캐릭터가 공유하도록 한다.
- 11개 기본 액션(idle / walk / run / point / speak / pray / kneel / sit / sit-edge / raiseHands / bow)의 정적 포즈 데이터를 정의한다.
- 타임라인(액션 + 시작/끝 프레임) → 현재 프레임의 포즈를 계산하는 `getCurrentPose` 함수를 작성한다.
- 액션 전환 시 12프레임(0.4초 @ 30fps) 동안 cubic 이징으로 부드럽게 보간한다.

## 수정 예정 경로

- `src/types.ts` (Pose, ActionName, TimelineEvent, CharacterPlacement, Scene)
- `src/poses/basicPoses.ts`
- `src/animation/interpolate.ts`
- `src/animation/easing.ts`
- `src/animation/getCurrentPose.ts`

## 검증 계획

- 단위 검증: 임의 프레임에서 idle ↔ pray 전환이 12프레임 동안 부드럽게 진행되는지 console.log로 시각화.
- `npx tsc --noEmit` 클린 빌드.
- 후속 통합: 1주차 후반 첫 요나 캐릭터로 시각 확인.

## 막힌 점

- raiseHands 자세는 회전 각도 ±2.6rad이 직관적이지 않음 — 코드 주석에 "0=down, ±π=up" 명시 필요.
- 추후 walk target 시스템 추가 시 `getCurrentPose` 시그니처가 바뀔 수 있음 — 다음 단계에서 검토.
