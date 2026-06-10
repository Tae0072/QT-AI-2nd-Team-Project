# Workflow — 2026-05-22 particles-and-transitions

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/particles-transitions` |
| 작업 패널 | 파티클 시스템(7종) + 씬 전환 효과(6종) |
| 기능 ID | F-06 |
| 기준 문서 | Remotion 결정론 문서, 파티클 시스템 패턴 |

## 작업 목표

- 7종 파티클 (rain / snow / dust / sparkle / embers / lightRays / fire) 시스템을 구현한다.
- 모든 파티클은 결정론적(deterministic) — 같은 프레임에서 같은 위치, Math.random 미사용.
- 6종 전환 효과 (fade / slide-left / slide-right / iris-in / iris-out / dissolve) 구현.
- 씬 진입/퇴장 시 자동 적용 가능하도록 `TransitionConfig` 타입 정의.

## 수정 예정 경로

- `src/scenery/Particles.tsx`
- `src/scenery/transitions.tsx` (씬 전환 컴포넌트)
- `src/types.ts`에 `ParticleType`, `ParticleEffect`, `TransitionType`, `TransitionConfig` 추가

## 검증 계획

- 7종 파티클 각각 임시 씬에 띄워 시각 확인.
- 같은 프레임을 두 번 렌더 → 동일한 결과 (결정론 확인).
- 6종 전환 효과 각각 idle 씬 → 다른 idle 씬 전환에 적용.

## 막힌 점

- fire 파티클의 색상 그라데이션 (red → orange → yellow → transparent)이 까다로움.
- iris-in/out은 SVG mask로 처리 — 큰 캔버스에서 성능 확인 필요.
