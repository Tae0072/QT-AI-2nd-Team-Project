# 2026-06-01 알림 목록(M-02) + 설정(M-06) Flutter 화면 구현

## 목표
마이페이지 퀵메뉴에서 진입하는 알림 목록 화면과 설정 화면을 구현한다.

## 작업 내용
1. **NotificationItem/NotificationListResponse 모델** — 알림 응답 파싱
2. **SettingsData 모델** — 설정 응답 파싱
3. **MyPageRepository** — 알림(GET/PATCH) + 설정(GET/PATCH) API 메서드 추가
4. **notificationsProvider / settingsProvider** — Riverpod FutureProvider
5. **NotificationListScreen** — 알림 목록 + 미읽음 필터 + 개별/전체 읽음 + Pull-to-refresh
6. **SettingsScreen** — 알림 Switch + 폰트 크기 Dropdown
7. **AppRouter** — `/notifications`, `/settings` 라우트 추가
8. **MyPageScreen** — 퀵메뉴 알림/설정 탭 와이어링

## 범위
- 브랜치: `feature/flutter-notification-settings`
- PR: (Open, 리뷰 대기)
- 커밋: `feat(flutter): 알림 목록(M-02) + 설정(M-06) 화면 구현`
- 변경: 8파일 332줄
- 관련: F-05 인앱 알림, F-13 마이페이지

## 미해결
- PR 머지 대기

## 담당
- Lead 강태오 (T)
