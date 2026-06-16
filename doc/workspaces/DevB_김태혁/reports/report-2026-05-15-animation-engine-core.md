# Report — 2026-05-15 animation-engine-core

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/animation-core` |
| 작업 패널 | 애니메이션 엔진 코어 완성 |
| PR | 로컬 커밋 (1인 개발) |
| 최종 상태 | DONE |

## 변경 내용

- `src/types.ts`에 16관절 `Pose` 타입을 확정했다.
  - 머리: `headTilt`, `headYOffset`, `mouthOpen`, `eyesClosed`, `eyeGazeX`, `eyeGazeY`
  - 몸통: `bodyTilt`, `bodyBend`
  - 팔: `leftArmShoulder/Elbow`, `rightArmShoulder/Elbow`
  - 다리: `leftLegHip/Knee`, `rightLegHip/Knee`
  - 전체: `scaleX/Y`, `positionX`
- `src/poses/basicPoses.ts`에 11개 액션 정적 포즈 정의 완료.
  - idle/walk/run: 거의 동일한 base pose (사이클은 추후 추가)
  - point: 한 팔 들어 가리키는 자세
  - speak: 두 팔 가볍게 벌리기
  - pray: 팔 모으고 머리 숙임, `bodyBend=25`
  - kneel: 무릎 꿇음, `scaleY=0.95` (squash)
  - sit: 다리 굽혀 앉기
  - raiseHands: 양팔 ±2.6rad로 위로
  - bow: `bodyTilt=0.5` 인사
- `src/animation/interpolate.ts`: 모든 관절 값을 t∈[0,1]로 선형 보간.
- `src/animation/easing.ts`: `easeInOutCubic` 작성.
- `src/animation/getCurrentPose.ts`: 타임라인 + 프레임 → 현재 포즈 계산.
  - 액션 전환 시 12프레임 동안 cubic ease.
  - 타임라인 끝 이후엔 마지막 액션 유지.

## 검증 결과

- `npx tsc --noEmit`: PASS
- idle → pray 전환 console 시각화: 12프레임에 걸쳐 부드럽게 변화 확인
- 11개 액션 모두 base pose로부터 spread 연산자로 안전하게 파생됨

## 확인한 금지선

- 외부 애니메이션 라이브러리 의존 없음 (순수 TypeScript + Math)
- 캐릭터별 특수 자세 추가 시에도 16관절 구조 유지 (확장 X)

## 남은 리스크

- 사이클 애니메이션(walk/run/speak 흔들림)은 다음 단계 작업.
- 캐릭터별로 다른 비율을 가지면 같은 포즈가 어색해질 수 있음 — 추후 `CHARACTER_SCALES` 레지스트리 필요.

## 회고

오늘은 "엔진의 IP가 무엇인가"를 코드로 옮긴 날. 16관절 Pose 타입 하나가 향후 모든 캐릭터·모든 액션·모든 씬의 공통 인터페이스가 된다. 이게 단단하면 다음 작업이 모두 빠르게 붙는다. 11개 액션을 미리 정의해둔 것도 시나리오 작성자가 "어떤 자세가 가능한가"를 코드 검색 없이 알 수 있게 하는 장치.
