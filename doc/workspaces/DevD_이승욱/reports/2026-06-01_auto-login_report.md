# 2026-06-01 자동 로그인 + 로그아웃 구현 — 결과 보고

## 요약
앱 재시작 시 SecureStorage의 토큰 존재 여부로 자동 로그인 분기 구현. 최초 실행(온보딩→로그인→닉네임→홈), 재시작(자동 로그인→홈), 로그아웃(→로그인) E2E 검증 완료.

## 산출물

| 파일 | 설명 |
|------|------|
| `main.dart` | authStatus 3단계 분기 + ValueKey Navigator 재생성 |
| `login_screen.dart` | 신규 회원 setAuthenticated 지연 |
| `nickname_setup_screen.dart` | 닉네임 완료 후 setAuthenticated |
| `profile_edit_screen.dart` | 로그아웃 버튼 추가 |
| `auth_providers.dart` | withInitial 테스트 생성자 |
| `auth_repository.dart` | 서버 응답 파싱 + 로그아웃 서버 폐기 호출 + SecureStorage 우선 삭제 |
| `widget_test.dart` | authStatus override + 자동 로그인 테스트 |

## 검증
- `flutter analyze` — No issues found
- `flutter test` — 95건 전체 통과
- 에뮬레이터 E2E:
  - 최초 실행: 온보딩 → 카카오 로그인 → 닉네임 설정 → 홈 ✅
  - 앱 재시작: 자동 로그인 → 바로 홈 ✅
  - 로그아웃: 프로필 → 로그아웃 → 로그인 화면 ✅
- PR Guard — 7파일/119줄, 금지 항목 없음

## 해결한 이슈
- `setAuthenticated()` 호출 시점 문제: 로그인 직후 호출하면 main.dart 재빌드로 닉네임 설정 화면 건너뜀 → 신규 회원은 닉네임 완료 후 호출로 해결
- MaterialApp `initialRoute` 변경 미반영: `key: ValueKey(initialRoute)`로 Navigator 재생성하여 해결

## 리뷰 피드백 대응
- Refresh Token 서버 폐기 호출 누락 → `POST /auth/logout` 호출 추가 (Redis 폐기)
- SecureStorage 삭제 순서 → 로컬 토큰 우선 삭제 후 서버/카카오 호출 (부분 실패 보장)
- 토큰 유효성 검증 → `AuthInterceptor` 401 자동 refresh로 충족 (Phase 2)
- `use_super_parameters` CI 실패 → super parameter 문법으로 수정

## 미해결
- PR 머지 대기
