# Report — 2026-05-18 canvas-character-rendering

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/canvas-characters` |
| 작업 패널 | Canvas 도형 기반 캐릭터 렌더링 시스템 |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- `src/characters/drawing.ts` 720여 줄 작성 — Canvas 도형 그리기 primitives 전부.
  - `drawCharacter(ctx, pose, style, secondaryMotion)` 진입점
  - `drawHead`, `drawBody`, `drawArm`, `drawLeg`, `drawHair`, `drawBeard` 등 부위별 함수
  - 4종 art style 분기: `simple` / `outlined` / `iconic` / `storybook`
  - `fillStroke`, `fillStrokeRect` 헬퍼로 외곽선 일관 처리
  - iconic 스타일은 머리 뒤에 halo 그림 (렌더 순서 조정)
- `src/characters/base/Skeleton.tsx`: `useEffect` 안에서 `drawCharacter` 호출, 1080×1920 캔버스 출력.
- 5개 캐릭터 thin wrapper 작성:
  - Jonah: 어부 의상, 짧은 갈색 머리, 갈색 수염
  - Moses: 흰 긴 머리 + 긴 흰 수염, 스태프
  - David: 어린이 비율 (scale 0.9), 짧은 빨강 머리, 양치기 의상
  - Peter: 어부, 긴 갈색 수염
  - Jesus: 긴 갈색 머리, 짧은 갈색 수염, 흰 로브
- `src/characters/registry.ts`에 `CHARACTER_SCALES` 추가 — IK/Look-At 계산 시 컴포넌트 인스턴스화 없이 캐릭터 비율 조회.

## 검증 결과

- 임시 컴포지션에서 5명 모두 idle 자세 정상 출력
- 같은 Pose로 4종 art style 비교 가능 (CharacterDesignShowcase 컴포지션 초안)
- `npx tsc --noEmit`: PASS

## 확인한 금지선

- AI 이미지 의존 없음 — 모두 코드로 그림
- 외부 sprite/atlas 파일 없음
- 캐릭터 추가 시 thin wrapper만 만들면 되도록 구조 유지

## 남은 리스크

- 700줄 단일 파일은 길지만 캐릭터 그리기는 한 곳에 모여있는 게 직관적. 추후 분할 필요 시 부위별로 나눌 것.
- iconic 스타일의 halo가 큰 머리 캐릭터에선 깨질 수 있음 — 캐릭터별 halo 반경 조정 추후.
- 사이클 애니메이션 없이 정지 자세만 가능 — 다음 단계 작업.

## 회고

긴 파일이지만 도형 기반 캐릭터 렌더링이 코드 한 곳에 모여있는 게 유지보수에 좋다. art style 분기를 `fillStroke` 같은 헬퍼로 통일해서 새 스타일 추가도 한 줄로 끝남. 다음 단계 사이클 애니메이션이 붙으면 정적 일러스트가 살아 움직이는 게 보일 것.
