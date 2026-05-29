# 2026-05-27 마이페이지 Flutter 화면 구현

## 목표
Phase 4에서 구현한 마이페이지 백엔드 API를 Flutter 앱에서 소비하는 화면을 구현한다. 대시보드, 프로필 상세, 닉네임 변경, 회원 탈퇴 UI를 포함하며, 위젯별 부분 실패 처리와 Pull-to-refresh를 지원한다.

## 작업 내용
1. **모델 2파일** — DashboardResponse(profile/stats/unreadNotificationCount/praiseSummary/widgetErrors), MemberResponse(닉네임잠금 판단 getter 포함)
2. **Repository 1파일** — MyPageRepository(Dio 기반 6개 메서드: dashboard/profile/updateProfile/changeNickname/checkNickname/withdraw)
3. **Provider 5개** — myPageRepositoryProvider, dashboardProvider, profileProvider, nicknameQueryProvider, nicknameAvailableProvider(300ms 디바운스)
4. **화면 2파일** — MypageScreen(Pull-to-refresh + widgetErrors SnackBar), ProfileEditScreen(닉네임 변경 + 7일 잠금 + 회원 탈퇴)
5. **위젯 4파일** — ProfileCard(CircleAvatar+닉네임), StatsCard(3열 통계), QuickMenuCard(알림/찬양/설정 + Badge 99+), WithdrawDialog
6. **라우터** — /my-page, /my-page/profile 추가

## 범위
- 브랜치: `feature/flutter-mypage`
- 변경 규모: 프로덕션 11파일 + 테스트 4파일
- `flutter analyze` No issues found
- 테스트: 4파일 18건 전체 통과 (dashboard_response 2, mypage_repository 8, profile_card 3, quick_menu_card 5)

## 미해결
- 알림 목록/찬양 목록 화면 연동 — 각 도메인 Flutter 화면 구현 시
- 프로필 이미지 업로드 — 파일 업로드 API 구현 후
- 통계 데이터 실제 연동 — notes 도메인 구현 후 (현재 0 표시)
- cached_network_image 도입 — 프로필 이미지 캐싱 필요 시

## 담당
- DevD 이승욱
