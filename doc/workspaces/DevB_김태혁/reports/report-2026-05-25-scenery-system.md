# Report — 2026-05-25 scenery-system

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/scenery` |
| 작업 패널 | 배경 시스템 (Sky/Mountain/Sea/Ship/City/Pier/Clouds/DistantIsland) |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- **Sky.tsx** — 4종 그라데이션:
  - dawn: 핑크 → 오렌지 → 푸르스름
  - day: 밝은 하늘색 + 흰 구름
  - storm: 어두운 회색 + 두꺼운 먹구름
  - night: 진한 남색 + 별 + 달
- **Mountain.tsx** — 다층 산 실루엣 (앞/중/뒤), sky variant에 따라 색 조정.
- **Sea.tsx** — 가로 파도 + 결정론적 흔들림.
  - `stormy=true`: 파도 2배 빠르게 + 흰 거품
- **Ship.tsx** — 옆모습 배 실루엣 + rocking 시 sin 흔들림 (±5°).
- **City.tsx** — 멀리 보이는 도시 실루엣 + warmLights로 창문 등.
- **Pier.tsx** — 항구 부두 (요나가 다시스 배 타는 씬용).
- **Clouds.tsx** — 다층 구름 (멀리/가까이).
- **DistantIsland.tsx** — 수평선 위 작은 섬.
- 모든 배경 컴포넌트는 `width={1080} height={1920}` 풀스크린.
- `SceneryExtra` union 타입: `{ type: 'Ship' | 'City' | ... , ...props }`.

## 검증 결과

- 4종 하늘 각각 시각 확인 — 시간대별 분위기 잘 표현
- 폭풍 씬 (storm + stormy sea + rocking ship): 요나 1장 분위기 재현
- 야경 + warmLights: 도시의 평화로운 밤 분위기
- `npx tsc --noEmit`: PASS

## 확인한 금지선

- 외부 SVG/이미지 사용 없음 (모두 코드로)
- 결정론 호환 (시간 입력은 항상 frame 기반)
- 1080×1920 세로 캔버스 가정

## 남은 리스크

- 도시 실루엣은 현재 단순한 사각형 합 — 추후 좀 더 다양한 건물 형태 필요.
- DistantIsland는 정적 — 파도에 따라 살짝 위아래 흔들리는 효과 추후 추가 가능.

## 회고

배경이 붙으니 씬이 진짜 "공간"이 된다. 요나서 1장의 폭풍 + 배 흔들림이 코드 3줄로 만들어진다는 게 엔진의 가치. SceneryExtra union 타입으로 시나리오 JSON에서 `{type: "Ship", rocking: true}` 한 줄로 끝나니 작가가 편함.
