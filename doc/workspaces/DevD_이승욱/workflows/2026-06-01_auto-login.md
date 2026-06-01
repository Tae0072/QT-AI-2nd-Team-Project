# 2026-06-01 자동 로그인 + 로그아웃 구현 (F-04)

## 목표
앱 재시작 시 저장된 토큰으로 자동 로그인하여 바로 홈 화면으로 진입하고, 로그아웃 시 토큰 삭제 후 로그인 화면으로 이동한다.

## 작업 내용
1. **main.dart authStatus 분기** — `unknown`(로딩 스피너), `authenticated`(`/home`), `unauthenticated`(`/login`). `key: ValueKey(initialRoute)`로 상태 변경 시 Navigator 재생성
2. **login_screen 신규 회원 setAuthenticated 지연** — 먼저 호출하면 main.dart 재빌드로 홈 강제 전환되는 문제 방지. 닉네임 설정 화면으로 이동 후 완료 시 호출
3. **nickname_setup_screen setAuthenticated** — 닉네임 설정 완료 후 호출 → 홈 자동 전환
4. **profile_edit_screen 로그아웃 버튼** — SecureStorage 우선 삭제 → `POST /auth/logout`(서버 Refresh Token 폐기) → 카카오 SDK 로그아웃 → `setUnauthenticated()` → `/login` 이동
5. **auth_providers withInitial** — 테스트용 생성자 추가, `_repository` nullable
6. **widget_test authStatus override** — 자동 로그인 분기 테스트 추가 (95건)

## 범위
- 브랜치: `feature/auto-login`
- PR: (Open, 리뷰 대기)
- 커밋: `feat(auth): 자동 로그인 + 로그아웃 구현 (F-04)`
- 변경: 7파일 119줄
- 관련: F-04 회원 인증

## 미해결
- PR 머지 대기

## 담당
- Lead 강태오 (T)
