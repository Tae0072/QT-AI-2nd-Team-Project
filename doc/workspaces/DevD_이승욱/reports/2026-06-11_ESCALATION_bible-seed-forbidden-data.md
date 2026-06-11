# [에스컬레이션 → Lead] 성경 본문 seed가 개역(KorRV)·KJV 원문 — CLAUDE.md §8 위반

작성: 2026-06-11 / DevD 이승욱 / 대상: Lead 강태오 (+ DevA 이지윤)

## 무엇
- `data/bible-json/generate_seed_sql.py` 가 `KorRV.json`(개역) + `KJV.json` 을 읽어
  `bible_verses` seed SQL을 생성한다. `korean_text = KorRV(개역)`, `english_text = KJV`.
- 이 스크립트·데이터는 **이미 origin/dev에 머지됨**: `6339a684 feat(bible): 성경 66권 전체 본문 seed 적재 (V23, 31104절) (#231)`.
- `data/bible-json/{KorRV.json, KJV.json}` 는 작업 트리에 untracked로도 존재.

## 왜 문제
CLAUDE.md §8 "금지 데이터": **개역개정/ESV/NIV seed·test·fixture·response 데이터 금지**,
성서유니온·두란노 본문 저장 금지. 개역(KorRV)·KJV 전문 seed 적재는 이에 정면 위배다.
(저작권 리스크를 낮추는 정책 위반)

## 범위 밖
- 본건은 이번 PR(`feature/flutter-bible-passage-page-explanation`)과 **무관**하며,
  해당 PR에는 금지 데이터/스크립트를 일절 포함하지 않았다(스테이징 제외).
- #231로 들어온 사안이라 임의 삭제하지 않고 에스컬레이션한다(요구사항·기존 결정 변경은 Lead 판단).

## 요청
1. 머지 전 확인: 허용 가능한 본문 소스(저작권 리스크 낮은 번역본/자체 제작)로 교체 또는 seed 제거.
2. 결정 시 V23 마이그레이션·`data/bible-json/*` 정리 주체/일정 합의.
3. 필요 시 `07_요구사항_정의서.md`/data 정책에 'BIBLE_SOURCE' 명시.

근거: CLAUDE.md §8, `06`/`23` 본문 소스 정책.
