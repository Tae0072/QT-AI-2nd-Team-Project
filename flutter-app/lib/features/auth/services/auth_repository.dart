import 'package:dio/dio.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';

import '../../../core/dev/dev_mode.dart'; // [DEV_MODE]
import '../../../core/storage/secure_storage.dart';
import 'kakao_auth_client.dart';

/// 인증 관련 API 호출 및 토큰 관리.
///
/// 카카오 SDK 호출은 [KakaoAuthClient] 포트를 통한다 —
/// 싱글톤(UserApi.instance) 직접 의존을 끊어 단위 테스트를 가능하게 한다.
class AuthRepository {
  final Dio _dio;
  final KakaoAuthClient _kakao;

  AuthRepository({required Dio dio, KakaoAuthClient? kakaoAuthClient})
      : _dio = dio,
        _kakao = kakaoAuthClient ?? SdkKakaoAuthClient();

  /// 카카오 로그인 → 서버 JWT 발급.
  ///
  /// 1. 카카오톡 또는 카카오계정으로 로그인
  /// 2. 카카오 액세스 토큰을 서버에 전달
  /// 3. 서버에서 JWT access/refresh 토큰 반환
  /// 4. SecureStorage에 저장
  Future<LoginResult> loginWithKakao() async {
    // 1) 카카오 로그인
    // 탈퇴 직후 첫 로그인은 Prompt.login으로 카카오 계정 재인증(이메일/비번 입력)을
    // 강제한다 — '완전히 새로 가입하는' 경험 제공 (2026-06-05 Lead 결정).
    final forceRelogin = await SecureStorage.getForceKakaoRelogin();
    KakaoLoginLog.add('카카오 로그인 시작 (forceRelogin=$forceRelogin)'); // [DEV_MODE]
    try {
      String kakaoAccessToken;
      if (forceRelogin) {
        kakaoAccessToken =
            await _kakao.loginWithKakaoAccount(prompts: [Prompt.login]);
      } else if (await _kakao.isKakaoTalkAvailable()) {
        kakaoAccessToken = await _kakao.loginWithKakaoTalk();
      } else {
        kakaoAccessToken = await _kakao.loginWithKakaoAccount();
      }
      KakaoLoginLog.add('카카오 액세스 토큰 획득'); // [DEV_MODE]

      // 2) 서버에 카카오 토큰 전달 → JWT 발급
      final response = await _dio.post(
        '/auth/kakao',
        data: {'kakaoAccessToken': kakaoAccessToken},
      );

      final data = response.data['data'] as Map<String, dynamic>;
      final accessToken = data['accessToken'] as String;
      final refreshToken = data['refreshToken'] as String;
      // 서버 응답: member.onboardingRequired (닉네임 미설정 시 true)
      final member = data['member'] as Map<String, dynamic>?;
      final isNewMember = member?['onboardingRequired'] as bool? ?? false;
      KakaoLoginLog.add('서버 로그인 성공 (isNewMember=$isNewMember)'); // [DEV_MODE]

      // 3) 토큰 저장 + 재인증 강제 플래그 해제(1회성)
      await SecureStorage.setAccessToken(accessToken);
      await SecureStorage.setRefreshToken(refreshToken);
      await SecureStorage.clearForceKakaoRelogin();

      return LoginResult(
        accessToken: accessToken,
        refreshToken: refreshToken,
        isNewMember: isNewMember,
      );
    } catch (e) {
      KakaoLoginLog.add('카카오 로그인 실패: $e'); // [DEV_MODE]
      rethrow;
    }
  }

  /// 로그아웃 — 서버 폐기를 먼저 시도하고, 어떤 경우에도 로컬 토큰을 삭제한다.
  ///
  /// 서버 `/auth/logout`은 인터셉터가 SecureStorage의 access token을 헤더에
  /// 붙여 호출하므로, 로컬 토큰 삭제보다 먼저 호출해야 Redis refresh token이
  /// 정상 폐기된다 (기존: 토큰을 먼저 지워 무인증 호출 → 서버 폐기 실패 버그).
  /// 서버/카카오 호출이 실패해도 finally에서 로컬 토큰은 반드시 삭제한다.
  Future<void> logout() async {
    try {
      // 1) 서버 Refresh Token 폐기 (Redis 삭제) — 토큰이 살아있는 상태에서 호출
      await _dio.post('/auth/logout');
    } catch (_) {
      // 서버 호출 실패해도 로컬 정리는 계속 진행
    } finally {
      // 2) 카카오 SDK 로그아웃 (세션 종료 — 연결은 유지)
      try {
        await _kakao.logout();
      } catch (_) {
        // 카카오 로그아웃 실패해도 무시
      }
      // 3) 로컬 토큰 삭제 — 어떤 경우에도 로컬 인증 상태 초기화 보장
      await SecureStorage.clearTokens();
    }
  }

  /// 회원 탈퇴 후 로컬 정리 — 카카오 연결끊기(unlink) + 로컬 토큰 삭제.
  ///
  /// 서버 탈퇴(DELETE /me)가 성공한 뒤 호출한다.
  /// unlink는 카카오 계정에서 QT-AI 앱 연결 자체를 해제하므로,
  /// 재로그인 시 동의화면부터 다시 시작한다 (탈퇴 후에도 카카오에
  /// 내 정보가 자동으로 뜨는 현상 방지). unlink 실패는 무시하고
  /// 로컬 토큰은 반드시 삭제한다.
  Future<void> cleanupAfterWithdraw() async {
    try {
      await _kakao.unlink();
    } catch (_) {
      // 카카오 연결끊기 실패해도 로컬 정리는 계속 진행
    } finally {
      await SecureStorage.clearTokens();
      // 다음 로그인 1회는 카카오 계정 재인증(이메일/비번 입력)부터 시작
      await SecureStorage.setForceKakaoRelogin();
    }
  }

  /// 저장된 액세스 토큰 확인.
  Future<bool> hasToken() async {
    final token = await SecureStorage.getAccessToken();
    return token != null && token.isNotEmpty;
  }
}

/// 로그인 결과.
class LoginResult {
  final String accessToken;
  final String refreshToken;
  final bool isNewMember;

  const LoginResult({
    required this.accessToken,
    required this.refreshToken,
    required this.isNewMember,
  });
}
