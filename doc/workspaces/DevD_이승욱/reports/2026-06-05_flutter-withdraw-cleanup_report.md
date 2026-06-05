# 2026-06-05 탈퇴 시 토큰·카카오 세션 정리 — Flutter 결과 보고

## 요약
회원 탈퇴 후 stale JWT·카카오 세션·인증 상태가 남아 재로그인이 로그인 화면에
갇히던 루프를 해소했다. 탈퇴 시 카카오 unlink로 연결을 해제하고, 다음 로그인
1회는 카카오 계정 재인증(이메일/비번 입력)을 강제하며, 탈퇴 다이얼로그에
개인정보 2년 보관·자동 삭제 고지를 반영했다 (2026-06-05 결정).

## 완료된 작업

### 1. 탈퇴 핸들러 정리 (`profile_edit_screen.dart`)
- 기존: 서버 탈퇴 후 라우팅만 변경 → SecureStorage 토큰·카카오 세션·
  `authStatusProvider`(authenticated) 그대로 잔존
- 루프 원인: 재로그인 성공 시 `setAuthenticated()`가 동일 상태값 →
  `main.dart`의 `ValueKey(initialRoute)` 미변경 → 화면 전환 불발
- 수정: 서버 탈퇴 → `cleanupAfterWithdraw()`(unlink + 토큰 삭제 + 재인증 플래그)
  → `setUnauthenticated()` 순서로 정리

### 2. logout 순서 버그 수정 (`auth_repository.dart`)
- 기존: 로컬 토큰 먼저 삭제 → `/auth/logout`이 무인증 호출 → Redis refresh 폐기 실패
- 수정: 서버 폐기 먼저 → 카카오 logout → 로컬 토큰 삭제(finally 보장)

### 3. 탈퇴 후 첫 로그인 재인증 강제 (2026-06-05 결정)
- 탈퇴 시 `force_kakao_relogin` 플래그 저장(SecureStorage)
- 다음 로그인 시 `loginWithKakaoAccount(prompts: [Prompt.login])`로
  이메일/비번 입력부터 강제 — "완전히 새로 가입" 경험
- 로그인 성공 시 플래그 해제 → 이후 평소 간편로그인 유지

### 4. 탈퇴 고지 문구 (`withdraw_dialog.dart`)
- "모든 데이터가 삭제되며 복구할 수 없습니다" → 2년 보존 정책 문구로 교체:
  계정 비활성화 + 개인정보·작성 기록 2년 보관 후 자동 삭제 + 보관 기간 내
  재로그인 시 복구 고지

## 변경 파일
| 파일 | 변경 내용 |
|------|----------|
| `features/auth/services/auth_repository.dart` | logout 순서 수정 + cleanupAfterWithdraw + Prompt.login |
| `features/mypage/screens/profile_edit_screen.dart` | 탈퇴 핸들러 정리 순서 적용 |
| `features/mypage/widgets/withdraw_dialog.dart` | 2년 보존 고지 문구 |
| `core/storage/secure_storage.dart` | force_kakao_relogin 플래그 저장소 |

## 검증
- [x] `flutter analyze` 0건
- [x] `flutter test` 100건 통과
- [x] 에뮬레이터 E2E: 탈퇴 → unlink 동작 확인(재로그인 시 동의화면 진입)
- [ ] 탈퇴 → 재로그인 → 이메일/비번 강제 입력 → 동의 → 홈 진입 전체 사이클
  최종 확인 (진행 중)

## 참고
- 카카오 계정 자체의 기기 로그인(웹 세션)은 서비스가 해제할 수 없음 —
  탈퇴 후에도 계정 이메일이 자동 표기되는 것은 카카오 정상 동작이며,
  Prompt.login이 이를 무시하고 재인증을 강제한다
- 서버 측 재활성화 로직은 `bugfix/member-withdraw-rejoin` PR과 짝 —
  서버 PR 머지 전에는 탈퇴 후 재로그인이 M0009로 실패한다 (머지 순서: 서버 먼저)
