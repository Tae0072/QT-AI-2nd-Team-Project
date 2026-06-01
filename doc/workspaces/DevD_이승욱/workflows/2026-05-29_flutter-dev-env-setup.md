# 2026-05-29 Flutter 개발 환경 설정 + 카카오 로그인 연동 수정

## 목표
Flutter 앱 실행 시 카카오 앱 키 등 환경변수를 자동 주입하고, 카카오 로그인 → 서버 JWT 교환 흐름의 연동 버그를 수정한다.

## 작업 내용
1. **launch.json 추가** — `.vscode/launch.json`에 `--dart-define-from-file=.env` 설정. Cursor/VS Code F5로 환경변수 자동 주입
2. **AppConfig 카카오 키 기본값** — dev 환경에서 `--dart-define` 없이도 카카오 SDK 초기화 가능하도록 기본값 추가 (네이티브 앱 키는 APK 공개 키)
3. **AuthRepository API 경로 수정** — baseUrl(`/api/v1`) + 요청 경로(`/api/v1/auth/kakao`) 이중 경로 문제 → `/auth/kakao`로 수정
4. **AuthRepository 필드명 수정** — Flutter `accessToken` → 서버 `kakaoAccessToken` 불일치 해결
5. **AuthRepository 응답 파싱 수정** — `data['isNewMember']` → `data['member']['onboardingRequired']` 서버 LoginResponse 구조 반영
6. **NicknameSetupScreen 경로 수정** — `/members/me/nickname` → `/me/nickname` 서버 경로 일치

## 범위
- 브랜치: `chore/flutter-dev-env-setup`
- PR: (Open, 리뷰 대기)
- 커밋: `chore(flutter): launch.json + .env 기반 dart-define 자동 로딩 설정`
- 변경: 4파일 (`.gitignore`, `launch.json`, `app_config.dart`, `auth_repository.dart`, `nickname_setup_screen.dart`)
- 관련: F-04 카카오 소셜 로그인

## 미해결
- PR 머지 대기

## 담당
- Lead 강태오 (T)
