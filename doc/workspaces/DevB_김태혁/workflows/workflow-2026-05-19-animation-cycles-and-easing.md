# Workflow — 2026-05-19 animation-cycles-and-easing

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/animation-cycles` |
| 작업 패널 | 사이클 애니메이션 + Newton-Raphson 큐빅 베지어 이징 |
| 기능 ID | F-03 |
| 기준 문서 | Disney 12 principles, CSS cubic-bezier 참고 |

## 작업 목표

- 정지된 11개 액션에 "사이클" 흔들림을 얹어 살아있는 느낌을 만든다.
- walk/run은 좌우 흔들림 + 위아래 바운스, speak는 입 사이클, idle은 호흡 sway.
- CSS의 `cubic-bezier(x1,y1,x2,y2)`와 호환되는 Newton-Raphson 풀이로 임의의 이징 곡선 지원.
- Easing이 Pose 보간뿐 아니라 카메라 워크·전환에도 재사용 가능하도록 분리.

## 수정 예정 경로

- `src/animation/cycles.ts` (applyWalkCycle, applyRunCycle, applyMouthCycle, applyIdleSway)
- `src/animation/easing.ts` (Newton-Raphson cubic-bezier solver 추가)
- `src/animation/getCurrentPose.ts` 사이클 적용 통합

## 검증 계획

- 임시 컴포지션에서 idle 캐릭터 호흡 sway 확인 (1.5초 주기)
- walk 액션이 좌우 흔들리며 약간 위아래 바운스
- speak 액션 시 mouthOpen 사이클 (1초 주기)
- easing 곡선 4종 (linear / easeIn / easeOut / easeInOut) 시각 비교

## 막힌 점

- run의 속도감을 walk와 어떻게 시각적으로 구분할지는 시행착오 필요.
- 캐릭터별로 호흡 주기 다르게 하면 더 자연스럽겠지만 일단 통일.
