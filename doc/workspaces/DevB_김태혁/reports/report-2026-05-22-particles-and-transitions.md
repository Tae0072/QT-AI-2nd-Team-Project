# Report — 2026-05-22 particles-and-transitions

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/particles-transitions` |
| 작업 패널 | 파티클 7종 + 전환 6종 |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- `src/scenery/Particles.tsx` 작성 — 7종 파티클 모두 한 컴포넌트로:
  - **rain**: 위→아래 빠른 흰선, intensity로 밀도 조절
  - **snow**: 부드러운 좌우 흔들림 + 천천히 하강
  - **dust**: 작은 점들이 천천히 부유 (Brownian-like)
  - **sparkle**: 깜빡이는 별점 (sin opacity)
  - **embers**: 위로 올라가는 작은 불꽃 (red→yellow)
  - **lightRays**: 위에서 아래로 비추는 빛줄기 (linear gradient)
  - **fire**: 아래 가운데에서 위로 타오르는 불꽃 모양
- 모든 파티클은 `(prng_state, index)` 기반 seed로 deterministic 위치 계산:
  - `seed = startFrame + index * 9301 + 49297` 식의 의사난수
  - Math.random 일절 사용 안 함
- `src/scenery/transitions.tsx` 작성 — 6종:
  - **fade**: opacity 0↔1
  - **slide-left/right**: translateX
  - **iris-in/out**: SVG circular mask로 화면 줄였다/넓혔다
  - **dissolve**: opacity + slight blur
- `TransitionConfig`: `type`, `durationFrames` — 진입 12프레임, 퇴장 12프레임 권장.

## 검증 결과

- 7종 파티클 임시 씬 시각 확인 PASS
- 같은 프레임 두 번 렌더: 동일한 픽셀 (deterministic 확인)
- 6종 전환 모두 자연스럽게 작동
- `npx tsc --noEmit`: PASS

## 확인한 금지선

- Math.random 미사용 (Remotion 결정론)
- 외부 파티클 라이브러리 의존 없음
- 파티클 개수는 intensity로 제한 (성능 가드)

## 남은 리스크

- iris-in/out은 SVG mask이라 1080×1920에서 60fps 시 살짝 부담 — 30fps 권장.
- fire 파티클의 색상 그라데이션은 GPU 압축에서 banding 가능 — 추후 dithering 추가 검토.

## 회고

파티클 7종이 다 한 컴포넌트에 있으니 시나리오 작성 시 `type: 'rain'`처럼 한 줄로 사용 가능. 결정론 확보가 핵심 — 매 렌더가 동일한 영상이어야 R2 캐싱이 의미 있음. 전환 효과는 영상의 흐름을 만드는 도구 — 큐티 씬 사이를 부드럽게 잇는 데 잘 쓰일 것.
