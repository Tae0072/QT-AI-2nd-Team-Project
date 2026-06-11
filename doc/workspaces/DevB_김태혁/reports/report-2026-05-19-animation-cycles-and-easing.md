# Report — 2026-05-19 animation-cycles-and-easing

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/animation-cycles` |
| 작업 패널 | 사이클 애니메이션 + Newton-Raphson 큐빅 베지어 이징 |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- `src/animation/cycles.ts` 작성:
  - `applyIdleSway(pose, timeSec)` — 1.5Hz sin 흔들림, `headYOffset` ±4px, `bodyTilt` ±0.005rad
  - `applyWalkCycle(pose, timeSec, intensity)` — 6Hz, 팔다리 반대 흔들림 + bodyBend 바운스
  - `applyRunCycle(pose, timeSec, intensity)` — 9Hz, 흔들림 크기 walk × 1.5
  - `applyMouthCycle(pose, timeSec)` — 4Hz mouthOpen 사이클
- `src/animation/easing.ts` 확장:
  - `cubicBezier(x1, y1, x2, y2)` factory — Newton-Raphson 5회 반복으로 t→y 해.
  - 표준 곡선: `easeIn`, `easeOut`, `easeInOut`, `easeInOutCubic`.
- `src/animation/getCurrentPose.ts`에 사이클 적용 통합:
  - 액션이 walk/run/speak/idle인 경우 보간된 포즈에 사이클 함수 호출.
  - sit/kneel 등 정적 액션은 idleSway 약하게(0.3배) 적용.

## 검증 결과

- idle 캐릭터: 자연스러운 호흡 sway 확인
- walk: 좌우 흔들림 + 0.27초 주기 위아래 바운스
- run: walk보다 빠르고 큰 흔들림
- speak: 입이 1초 주기로 열렸다 닫힘
- 4종 easing 곡선 시각 비교: 모두 (0,0)→(1,1) 단조증가 확인
- `npx tsc --noEmit`: PASS

## 확인한 금지선

- 추가 라이브러리 의존 없음 (순수 수학)
- 사이클 함수는 모두 Pose를 받아서 Pose를 반환 — 불변성 유지
- Remotion 결정론(deterministic) 호환: `Math.random` 미사용

## 남은 리스크

- 캐릭터별 호흡 주기/세기 차이는 추후 `CharacterStyle`에 추가 가능.
- walk 사이클의 좌우 흔들림이 너무 크면 어색 — `intensity` 파라미터로 추후 조정.

## 회고

오늘은 "엔진이 생명을 얻는 날". 정지 포즈가 사이클을 만나니 캐릭터가 살아 움직이는 느낌이 났다. 4Hz 입 사이클이 speak 액션에 붙으니 사람이 말하는 것처럼 보이는 게 신기. easing 곡선을 코드로 정의해두면 카메라 워크, 전환, 카운트다운까지 다 같은 인프라를 쓸 수 있다.
