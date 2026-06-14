# 2026-06-14 홈(랜딩) 화면 신설

## 요청
- QT 페이지 대신 홈 화면을 첫 탭으로, '묵상 시작하기'로 QT 본문 진입.
- 홈에 개인 닉네임 인사, 오늘의 말씀(오늘 QT 절 중 매일 랜덤 1절) + 매일 랜덤 배경, 최근 묵상 기록(기록 탭 연동).

## 결정(사용자)
- 하단 탭: 첫 탭만 '홈'으로 교체(성경/나눔/기록/마이 유지, 5탭). 배경: 그라데이션 세트(매일 랜덤).

## 구현(프론트)
- 신규 `home/providers/home_providers.dart`: `homeTabIndexProvider`(탭 전환 공유), `homeRecentNotesProvider`(노트 최근 3개 — 기록 탭과 같은 repo).
- 신규 `home/screens/home_dashboard_screen.dart`:
  - 닉네임 인사(`profileProvider`).
  - 오늘의 말씀 카드: `todayQtPassageProvider`의 절 중 **날짜 시드 랜덤 1절** + 6종 어스톤 그라데이션 중 **날짜 시드 랜덤**(흰 글자). 참조 라벨 "책 장:절".
  - '묵상 시작하기' → 오늘 QT 본문(`TodayQtScreen`) push.
  - 최근 묵상 기록 + '모두 보기' → 기록 탭(index 3)으로 전환.
- `home/screens/home_screen.dart`: `StatefulWidget`→`ConsumerWidget`. 첫 탭을 `HomeDashboardScreen`으로 교체, 라벨 '홈'·home 아이콘. 탭 인덱스를 `homeTabIndexProvider`로 관리.
- 오늘 QT는 더 이상 첫 탭이 아니며 '묵상 시작하기'로 진입.

## 검증
- `flutter analyze` 무이슈, `flutter test` 302개 통과(앱 전체 진입 테스트를 홈→묵상시작하기 흐름으로 갱신, 홈 최근기록 provider 오버라이드).

## Git/PR
- 브랜치 `feature/home-dashboard-screen` → PR 대상 `dev`.
