import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';

/// 카카오 SDK 호출 경계 — 단위 테스트에서 가짜 구현으로 대체하기 위한 포트.
///
/// `UserApi.instance`는 싱글톤이라 직접 모킹할 수 없으므로,
/// [AuthRepository]는 이 인터페이스를 통해서만 카카오 SDK를 호출한다.
abstract class KakaoAuthClient {
  /// 카카오톡 앱 설치 여부.
  Future<bool> isKakaoTalkAvailable();

  /// 카카오톡 앱으로 로그인 — 발급된 access token을 반환한다.
  Future<String> loginWithKakaoTalk();

  /// 카카오 계정(웹)으로 로그인 — 발급된 access token을 반환한다.
  ///
  /// [prompts]에 [Prompt.login]을 넘기면 기존 세션을 무시하고
  /// 이메일/비밀번호 재인증을 강제한다 (탈퇴 후 1회 재인증 정책).
  Future<String> loginWithKakaoAccount({List<Prompt>? prompts});

  /// 카카오 세션 로그아웃 (연결은 유지).
  Future<void> logout();

  /// 카카오 계정에서 앱 연결 해제 — 재로그인 시 동의화면부터 시작.
  Future<void> unlink();
}

/// 실제 카카오 SDK 위임 구현 (얇은 pass-through — 단위 테스트 대상 아님).
class SdkKakaoAuthClient implements KakaoAuthClient {
  @override
  Future<bool> isKakaoTalkAvailable() => isKakaoTalkInstalled();

  @override
  Future<String> loginWithKakaoTalk() async =>
      (await UserApi.instance.loginWithKakaoTalk()).accessToken;

  @override
  Future<String> loginWithKakaoAccount({List<Prompt>? prompts}) async =>
      (await UserApi.instance.loginWithKakaoAccount(prompts: prompts))
          .accessToken;

  @override
  Future<void> logout() => UserApi.instance.logout();

  @override
  Future<void> unlink() => UserApi.instance.unlink();
}
