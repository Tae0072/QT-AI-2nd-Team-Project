# Workflow — 2026-05-20 constraints-ik-lookat

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/constraints` |
| 작업 패널 | IK (2-bone) + Look-At constraint + pose blending |
| 기능 ID | F-04 |
| 기준 문서 | Rive constraint 문서, 코사인법칙 wiki |

## 작업 목표

- 캐릭터가 화면 내 특정 좌표를 손으로 가리키도록 하는 2-bone IK를 구현한다.
- 캐릭터가 다른 캐릭터/대상의 좌표를 눈·머리로 추적하는 Look-At constraint를 구현한다.
- 여러 포즈를 가중치 합산으로 블렌딩하는 `blendPoses` 헬퍼를 작성한다.
- 모든 constraint는 Pose를 입력받아 Pose를 반환 — 함수형 합성 가능.

## 수정 예정 경로

- `src/animation/constraints/ik.ts` — 2-bone IK (어깨↔팔꿈치↔손)
- `src/animation/constraints/lookAt.ts` — 눈/머리 추적
- `src/animation/blendPoses.ts` — 포즈 가중치 합성
- `src/types.ts`에 `LookAtKeyframe`, `IKKeyframe` 타입 추가

## 검증 계획

- IK 테스트: 화면 4모서리에 손이 닿도록 IK 적용 → 시각 확인
- Look-At 테스트: 카메라가 좌→우로 이동할 때 캐릭터 시선 따라감
- blendPoses: idle ↔ raiseHands 50/50 블렌드 → 중간 자세 출력

## 막힌 점

- IK 도달 불가 영역(팔 최대 길이 초과)에서 부드럽게 처리 필요 — 클램프 + 외삽.
- 머리 회전 한계 설정 (목 부러지지 않게) — `clamp(±π/4)`.
