/// 웹 카카오 로그인 사전 가드 (코드리뷰 TODO 2).
///
/// 카카오 dart SDK는 웹에서 동작하지 않으므로, 웹 사용자가 버튼을 누르면 SDK 예외가
/// 그대로 발생한다. 화면에서는 버튼을 비활성화하고 안내 문구를 보여준다.
///
/// `kIsWeb`은 테스트에서 분기 제어가 어려워, 판정을 순수 함수로 분리해 단위 테스트한다.
/// dev 전용 웹 우회(`webDevNoLogin`, 삼중 게이트)가 켜진 경우는 로그인 화면을 거치지
/// 않거나 우회 인증을 쓰므로 가드 대상에서 제외한다.
///
/// 참고: 웹 카카오 로그인 정식 지원은 서버측 OAuth가 필요해 기존 결정(서버사이드
/// `/oauth2/**` 미사용, CLAUDE.md §5)과 충돌하는 미해결 설계 이슈 — 이 가드는 안내까지만.
bool isKakaoLoginUnsupported({
  required bool isWeb,
  required bool webDevBypassEnabled,
}) {
  return isWeb && !webDevBypassEnabled;
}
