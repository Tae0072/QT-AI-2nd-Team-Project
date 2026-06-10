# Workflow — 2026-05-18 canvas-character-rendering

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/canvas-characters` |
| 작업 패널 | Canvas 도형 기반 캐릭터 렌더링 시스템 구축 |
| 기능 ID | F-02 |
| 기준 문서 | `STYLE_GUIDE.md`, 첫 요나 디자인 스케치 |

## 작업 목표

- Canvas 2D API로 16관절 Pose 값을 받아 캐릭터를 그리는 `drawCharacter` 함수를 작성한다.
- 공유 `Skeleton.tsx` 컴포넌트를 만들고 각 캐릭터(Jonah/Moses/David/Peter/Jesus)는 `CharacterStyle`만 다르게 주입하는 thin wrapper로 둔다.
- 머리/몸/팔/다리를 도형(arc, rect)으로 그리되 Disney의 squash & stretch 원칙 반영 가능하도록 `scaleX/Y` 분리.
- 4개 art style (simple / outlined / iconic / storybook) 출력 차이 검토.

## 수정 예정 경로

- `src/characters/drawing.ts` (~700줄 예정 — 모든 도형 그리기 primitives)
- `src/characters/base/Skeleton.tsx`
- `src/characters/Jonah.tsx`
- `src/characters/Moses.tsx`
- `src/characters/David.tsx`
- `src/characters/Peter.tsx`
- `src/characters/Jesus.tsx`
- `src/characters/registry.ts` (캐릭터별 스케일 lookup)

## 검증 계획

- `npm run preview` → 임시 컴포지션에서 각 캐릭터가 idle 자세로 보임.
- 같은 Pose를 4개 art style로 렌더해 시각 비교.
- `npx tsc --noEmit` 클린.

## 막힌 점

- 머리카락/수염 종류 다양화는 다음 단계로 미루고, 일단 `HairStyle` enum만 정의.
- iconic 스타일의 halo는 머리 뒤에 그려야 해서 렌더 순서 신중히.
