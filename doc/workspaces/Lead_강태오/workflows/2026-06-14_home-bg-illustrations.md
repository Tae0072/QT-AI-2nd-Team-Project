# 2026-06-14 오늘의 말씀 배경 — 자작 기독교 일러스트

## 요청
- 저작권 없는 기독교 배경 그림을 직접 그려서 넣어 달라.

## 구현
- 직접 그린 **원본 일러스트 6장**(저작권 무부담)을 PIL로 생성해 자산으로 추가:
  - `sunrise_cross`(일출+언덕 십자가), `three_crosses`(세 십자가), `mountains_dawn`(산 여명),
    `valley_path`(골짜기 빛+길), `church_evening`(교회 실루엣), `stained_glass`(스테인드글라스).
  - 모두 그라데이션 하늘 + 빛/실루엣 구성. 흰 본문 글자 가독을 위해 어두운 막(0.30→0.52) 위에 표시.
- `assets/home_backgrounds/` 자산 등록(pubspec), `_kBackgroundAssets`에 6장 연결.
- 오늘의 말씀 카드는 매일 랜덤으로 한 장을 `BoxFit.cover`로 배경에 깔고, 그 위 정중앙에 본문(절·참조)을 표시. 로드 실패 시 그라데이션+십자가 드로잉으로 폴백.

## 검증
- `flutter analyze` 무이슈, `flutter test` 302개 통과.

## Git/PR
- 브랜치 `feature/home-bg-illustrations` → PR 대상 `dev`. PNG 6장 + pubspec + 화면 코드.
