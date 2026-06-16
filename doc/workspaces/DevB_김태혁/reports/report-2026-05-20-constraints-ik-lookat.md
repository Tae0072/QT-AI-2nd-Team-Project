# Report — 2026-05-20 constraints-ik-lookat

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/constraints` |
| 작업 패널 | IK + Look-At + pose blending |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- `src/animation/constraints/ik.ts` — 2-bone IK 구현
  - 코사인법칙으로 어깨/팔꿈치 회전각 도출
  - 입력: target(x,y), bendSign(-1=안쪽, +1=바깥쪽)
  - 도달 불가 시 직선 자세 + 부드러운 외삽 (`bendSign` 무시)
  - 캐릭터 비율을 `CHARACTER_SCALES` 레지스트리에서 조회해 팔 길이 계산
- `src/animation/constraints/lookAt.ts` — Look-At 구현
  - `eyeGazeX/Y` (눈동자 오프셋, 최대 ±0.7)
  - `headTilt` (머리 추가 회전, 최대 ±π/6 = 30°)
  - intensity 파라미터로 강도 조절 (0=무시, 1=완전 추적)
- `src/animation/blendPoses.ts` — 가중치 합성
  - 모든 16관절을 가중평균
  - 각도는 sin/cos 변환 후 보간 (wraparound 안전)
- `src/types.ts`에 `LookAtKeyframe`, `IKKeyframe` 추가:
  - startFrame, endFrame, target(Vec2), intensity?, bendSign?

## 검증 결과

- IK 테스트: 화면 4모서리 + 중앙 → 손이 정확히 도달
- 도달 불가 영역 (캐릭터 좌측 1500px): 팔 직선 + 어색하지 않음
- Look-At 테스트: 카메라 0→1080 이동 시 캐릭터 시선이 따라감
- blendPoses 50/50: idle + raiseHands의 자연스러운 중간 자세
- `npx tsc --noEmit`: PASS

## 확인한 금지선

- 외부 IK 라이브러리 의존 없음 (직접 구현)
- 머리 회전 ±30°, 눈동자 ±0.7로 제한 (목 부러지지 않게)
- Pose 불변성 유지 — 모든 constraint는 새 Pose 반환

## 남은 리스크

- 양손 IK 동시 적용 시 어깨 위치 충돌 가능 — 추후 어깨 forward kinematics 우선 적용 후 IK.
- bendSign이 매 키프레임마다 명시 안 되면 갑자기 팔꿈치 방향 바뀜 — 기본값 +1로 통일.

## 회고

코사인법칙 한 줄이 캐릭터가 "가리키는" 동작을 자연스럽게 만들어준다. IK 없이는 매 자세마다 팔 각도를 손으로 계산해야 했을 것. Look-At은 시나리오 작성자가 "여기 봐"라고 좌표만 찍으면 카메라/캐릭터가 따라가는 마법. 큐티 영상에서 시선 처리는 몰입의 핵심.
