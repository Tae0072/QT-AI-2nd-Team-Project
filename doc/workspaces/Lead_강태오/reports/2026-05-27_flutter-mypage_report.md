# 2026-05-27 Flutter 마이페이지 화면 구현 — 결과 보고

## 요약
Flutter 앱의 마이페이지(F-16) 화면을 구현했다.
대시보드, 프로필 편집, 닉네임 변경(7일 잠금), 회원 탈퇴 흐름을 포함하며,
Widget 테스트 4건 + Repository 테스트 1건을 작성했다.

## 산출물

| 파일 | 설명 |
|------|------|
| `flutter-app/lib/features/mypage/models/dashboard_response.dart` | 대시보드 API 응답 DTO |
| `flutter-app/lib/features/mypage/models/member_response.dart` | 회원 정보 API 응답 DTO |
| `flutter-app/lib/features/mypage/providers/mypage_providers.dart` | Riverpod 상태 관리 |
| `flutter-app/lib/features/mypage/screens/mypage_screen.dart` | 마이페이지 대시보드 화면 |
| `flutter-app/lib/features/mypage/screens/profile_edit_screen.dart` | 프로필 편집 화면 |
| `flutter-app/lib/features/mypage/services/mypage_repository.dart` | 마이페이지 API 호출 |
| `flutter-app/lib/features/mypage/widgets/profile_card.dart` | 프로필 카드 위젯 |
| `flutter-app/lib/features/mypage/widgets/quick_menu_card.dart` | 빠른 메뉴 카드 위젯 |
| `flutter-app/lib/features/mypage/widgets/stats_card.dart` | 통계 카드 위젯 |
| `flutter-app/lib/features/mypage/widgets/withdraw_dialog.dart` | 탈퇴 확인 다이얼로그 |
| `flutter-app/lib/core/router/app_router.dart` | 라우터에 마이페이지 경로 추가 |
| `flutter-app/test/features/mypage/models/dashboard_response_test.dart` | 대시보드 DTO 테스트 |
| `flutter-app/test/features/mypage/services/mypage_repository_test.dart` | Repository 테스트 |
| `flutter-app/test/features/mypage/widgets/profile_card_test.dart` | 프로필 카드 위젯 테스트 |
| `flutter-app/test/features/mypage/widgets/quick_menu_card_test.dart` | 빠른 메뉴 위젯 테스트 |

## 구현 상세

### API 모델
`DashboardResponse`와 `MemberResponse`를 불변 클래스로 구현했다.
`fromJson`/`toJson` 직렬화를 포함하며, 서버 `ApiResponse<T>` envelope에 맞춰 파싱한다.

### 닉네임 7일 잠금
서버의 `lastNicknameChangedAt` 필드를 확인하여 7일 미경과 시 변경 버튼을 비활성화한다.
`checkNicknameAvailable()` API로 실시간 중복 검사를 수행한다.

### 회원 탈퇴
`WithdrawDialog`에서 사유 입력(선택) 후 확인 시 `DELETE /api/v1/me`를 호출한다.
탈퇴 완료 후 로컬 토큰 삭제 및 로그인 화면으로 이동한다.

### 테스트
- **dashboard_response_test**: JSON 파싱 정상/빈값/null 케이스 검증
- **mypage_repository_test**: Dio Mock으로 API 호출 검증
- **profile_card_test**: 닉네임, 프로필 이미지 렌더링 검증
- **quick_menu_card_test**: 메뉴 항목 탭 콜백 검증

## PR 크기 사유
15 files, 1,409 lines (XL). Flutter 마이페이지는 화면(2) + 위젯(4) + 모델(2) +
Provider(1) + Repository(1) + Router(1) + 테스트(4)로 구성되며,
기능 단위로 더 분리하면 오히려 리뷰 맥락이 끊어져 단일 PR로 제출한다.

## 남은 리스크
- 백엔드 MyPage 대시보드 API(`GET /api/v1/me/dashboard`) 구현 대기 중 — 현재 개별 API 조합으로 대체
- 프로필 이미지 업로드 기능은 후속 PR에서 구현 예정
