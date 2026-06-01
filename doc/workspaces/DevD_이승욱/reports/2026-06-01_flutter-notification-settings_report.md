# 2026-06-01 알림 목록 + 설정 Flutter 화면 — 결과 보고

## 요약
마이페이지 퀵메뉴 알림/설정 탭에서 진입하는 M-02 알림 목록 화면(미읽음 필터, 개별/전체 읽음)과 M-06 설정 화면(알림 Switch, 폰트 Dropdown) 구현.

## 산출물

| 파일 | 설명 |
|------|------|
| `notification_response.dart` | NotificationItem, NotificationListResponse 모델 |
| `settings_response.dart` | SettingsData 모델 |
| `mypage_repository.dart` | 알림/설정 API 메서드 추가 |
| `mypage_providers.dart` | notificationsProvider, settingsProvider 추가 |
| `notification_list_screen.dart` | M-02 알림 목록 화면 |
| `settings_screen.dart` | M-06 설정 화면 |
| `app_router.dart` | /notifications, /settings 라우트 |
| `mypage_screen.dart` | 퀵메뉴 알림/설정 와이어링 |

## 검증
- `flutter analyze` — No issues found
- `flutter test` — 95건 전체 통과
- PR Guard — 8파일/332줄, 금지 항목 없음

## 미해결
- PR 머지 대기
