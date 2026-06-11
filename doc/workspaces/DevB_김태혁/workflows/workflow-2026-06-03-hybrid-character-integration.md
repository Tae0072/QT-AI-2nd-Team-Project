# Workflow — 2026-06-03 hybrid-character-integration

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/hybrid-character` |
| 작업 패널 | Canvas + Sprite 하이브리드 캐릭터 시스템 + 예수 sprite 통합 |
| 기능 ID | F-13 |
| 기준 문서 | report-2026-06-02-lora-stack-and-prompt, FLUX img2img 결과 |

## 작업 목표

- Canvas 도형 기반 캐릭터(요나·모세·다윗·베드로)와 Sprite PNG 기반 캐릭터(예수)를 같은 씬에서 혼용 가능한 하이브리드 디스패처를 구축한다.
- 어떤 캐릭터가 어느 모드로 렌더되는지 한 곳(`renderMode.ts`)에서 관리.
- 사용자가 자세 PNG를 추가하면 자동으로 SpriteCharacter가 로드 + 크로스페이드 전환.
- 자세 PNG 부재 시 idle.png로 자동 폴백.

## 수정 예정 경로

- `src/characters/sprite/SpriteCharacter.tsx` (범용 스프라이트 렌더러)
- `src/characters/renderMode.ts` (캐릭터별 canvas/sprite 모드 설정)
- `src/scenes/_renderer/CharacterByName.tsx` (하이브리드 디스패처)
- `src/types.ts`에 `positionY` 추가 (점프/비행 등 수직 이동)
- `src/animation/getCurrentPose.ts`에 walk target 시스템 추가
- `src/poses/basicPoses.ts`에 `sit-edge` 추가
- `src/characters/registry.ts`에 ChildBoy/ChildGirl 스케일 등록
- `src/scenes/jesus_peter_test.json` (하이브리드 테스트 씬)
- `public/characters/jesus/idle.png` (배경 제거된 베스트 예수)
- `package.json`에 `render:hybrid` 스크립트 추가

## 검증 계획

- jesus_peter_test 씬: Jesus(sprite) + Peter(canvas) 동시 렌더 — 깨짐 없이 같은 씬에 표시.
- 자세 PNG 부재 시 idle.png 폴백 확인.
- 자세 전환 시 8프레임 크로스페이드 정상.
- positionY 변경으로 점프 효과 확인.
- `npx tsc --noEmit` 클린 빌드.

## 막힌 점

- 자세 PNG 로딩이 비동기 — Remotion 결정론과 충돌 가능. preload 메커니즘 필수.
- 캔버스 캐릭터와 스프라이트 캐릭터의 baseY 다름 — 통일 필요.
- walk target은 이전→다음 이벤트 사이를 보간 — 시나리오 작성자가 헷갈리지 않게 명시 필요.
