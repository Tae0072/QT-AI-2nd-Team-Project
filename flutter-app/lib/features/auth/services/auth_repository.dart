import 'package:dio/dio.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';

import '../../../core/storage/secure_storage.dart';

/// 인증 관련 API 호출 및 토큰 관리.
class AuthRepository {
  final Dio _dio;

  AuthRepository({required Dio dio}) : _dio = dio;

  /// 카카오 로그인 → 서버 JWT 발급.
  ///
  /// 1. 카카오톡 또는 카카오계정으로 로그인
  /// 2. 카카오 액세스 토큰을 서버에 전달
  /// 3. 서버에서 JWT access/refresh 토큰 반환
  /// 4. SecureStorage에 저장
  Future<LoginResult> loginWithKakao() async {
    // 1) 카카오 로그인
    OAuthToken kakaoToken;
    if (await isKakaoTalkInstalled()) {
      kakaoToken = await UserApi.instance.loginWithKakaoTalk();
    } else {
      kakaoToken = await UserApi.instance.loginWithKakaoAccount();
    }

    // 2) 서버에 카카오 토큰 전달 → JWT 발급
    final response = await _dio.post(
      '/auth/kakao',
      data: {'kakaoAccessToken': kakaoToken.accessToken},
    );

    final data = response.data['data'] as Map<String, dynamic>;
    final accessToken = data['accessToken'] as String;
    final refreshToken = data['refreshToken'] as String;
    // 서버 응답: member.onboardingRequired (닉네임 미설정 시 true)
    final member = data['member'] as Map<String, dynamic>?;
    final isNewMember = member?['onboardingRequired'] as bool? ?? false;

    // 3) 토큰 저장
    await SecureStorage.setAccessToken(accessToken);
    await SecureStorage.setRefreshToken(refreshToken);

    return LoginResult(
      accessToken: accessToken,
      refreshToken: refreshToken,
      isNewMember: isNewMember,
    );
  }

  /// 로그아웃 — 토큰 삭제.
  Future<void> logout() async {
    try {
      await UserApi.instance.logout();
    } catch (_) {
      // 카카오 로그아웃 실패해도 로컬 토큰은 삭제
    }
    await SecureStorage.clearTokens();
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
