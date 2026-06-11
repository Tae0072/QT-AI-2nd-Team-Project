# Workflow — 2026-05-25 scenery-system

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/scenery` |
| 작업 패널 | 배경 시스템 — Sky / Mountain / Sea / Ship / City / Pier / DistantIsland |
| 기능 ID | F-07 |
| 기준 문서 | `STYLE_GUIDE.md` 색감 가이드, 요나서 1장 텍스트 분석 |

## 작업 목표

- 4종 하늘 변형 (dawn / day / storm / night)을 그라데이션 + 구름으로 구현한다.
- 산/바다/배/도시/부두/원경 섬을 배경 컴포넌트로 모듈화 — 시나리오 JSON에서 `scenery.extras`로 조합 가능.
- 배(Ship)는 파도에 따라 살짝 흔들리는 옵션(`rocking`) 지원.
- 도시(City)는 야간일 때 따뜻한 등불(`warmLights`) 옵션.

## 수정 예정 경로

- `src/scenery/Sky.tsx`
- `src/scenery/Mountain.tsx`
- `src/scenery/Sea.tsx`
- `src/scenery/Ship.tsx`
- `src/scenery/City.tsx`
- `src/scenery/Pier.tsx`
- `src/scenery/Clouds.tsx`
- `src/scenery/DistantIsland.tsx`
- `src/types.ts`에 `SceneryExtra`, `SkyVariant` 추가

## 검증 계획

- 4종 하늘 각각 idle 캐릭터와 함께 시각 확인.
- 폭풍(storm) 하늘 + Sea(stormy=true) + Ship(rocking=true) 조합으로 요나 폭풍 씬 프리뷰.
- 도시 야경 + warmLights = 따뜻한 분위기 확인.

## 막힌 점

- Sea의 파도 사이클(아래위 흔들림) 주기는 storm일 때 더 빠르게 — 분기 처리 필요.
- 산 그림자는 시간대(sky variant)에 따라 색이 달라야 자연스러움.
