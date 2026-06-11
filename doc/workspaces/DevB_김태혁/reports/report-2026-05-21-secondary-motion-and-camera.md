# Report — 2026-05-21 secondary-motion-and-camera

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/secondary-camera` |
| 작업 패널 | 2차 모션 + 카메라 시스템 |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- `src/animation/secondaryMotion.ts` 작성
  - `computeSecondaryMotion(prevPose, currentPose)` — 속도(velocityX, velocityY) 기반.
  - `SecondaryMotion` 타입: `robeLagX`, `robeWaveY`, `beardSwayX`, `hairShiftX`.
  - 강도 계수: 옷자락 ±8px, 머리·수염 ±4px.
  - 가속도가 클수록 lag 커짐 (1차 lowpass 필터).
- `src/camera/Camera.tsx` 작성
  - props로 받은 `CameraConfig`에 따라 컨테이너 transform 적용.
  - pan-left/right: 시간에 따라 ±200px 슬라이드 (easeInOut).
  - zoom-in/out: scale 1.0 ↔ 1.3 (easeInOut).
  - shake: `ShakeKeyframe[]` 적용 — intensity, frequency, decay.
- `ShakeKeyframe` 감쇠:
  - `linear`: 시간에 따라 선형 감소
  - `exponential`: 빠른 감쇠
  - `none`: 일정한 강도 유지
- `drawCharacter` 시그니처에 `secondaryMotion?` 인자 추가 (기본값 NO_SECONDARY).
- 모든 캐릭터 `Skeleton`에 자동으로 적용.

## 검증 결과

- 빠르게 walk → 즉시 stop: 옷자락이 0.3초 늦게 따라오는 visual 확인
- pan-left 카메라: 480 프레임 동안 자연스럽게 좌측 슬라이드
- shake (intensity 8, frequency 20Hz, exponential decay): 천둥 느낌
- `npx tsc --noEmit`: PASS

## 확인한 금지선

- Math.random 미사용 — shake는 deterministic sin/cos
- 영상 압축 호환: 너무 빠른 흔들림(>30Hz) 금지

## 남은 리스크

- 2차 모션이 모든 캐릭터에 동일 강도로 적용됨 — 캐릭터별 의상 무게에 따라 조정 가능하도록 추후 확장.
- 카메라 shake와 캐릭터 흔들림이 겹치면 멀미 — 같은 씬에서 동시 사용 자제 가이드 필요.

## 회고

오늘은 영상에 "무게감"이 생긴 날. 옷자락이 늦게 따라오는 follow-through 한 줄이 캐릭터를 종이 인형이 아닌 실재하는 사람으로 보이게 한다. 카메라 shake로 천둥 효과를 넣어보니 큐티 영상의 극적 순간 연출이 가능해 보임.
