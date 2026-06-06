import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 토큰 보안 저장소.
///
/// Android: [EncryptedSharedPreferences] 사용을 명시하여
/// 구버전 Android에서도 평문 저장을 방지한다.
class SecureStorage {
  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  static const _accessTokenKey = 'access_token';
  static const _refreshTokenKey = 'refresh_token';
  static const _forceKakaoReloginKey = 'force_kakao_relogin';

  // Access Token
  static Future<String?> getAccessToken() =>
      _storage.read(key: _accessTokenKey);
  static Future<void> setAccessToken(String token) =>
      _storage.write(key: _accessTokenKey, value: token);

  // Refresh Token
  static Future<String?> getRefreshToken() =>
      _storage.read(key: _refreshTokenKey);
  static Future<void> setRefreshToken(String token) =>
      _storage.write(key: _refreshTokenKey, value: token);

  // 전체 토큰 삭제
  static Future<void> clearTokens() async {
    await _storage.delete(key: _accessTokenKey);
    await _storage.delete(key: _refreshTokenKey);
  }

  // 탈퇴 후 1회 카카오 재인증(이메일/비번 입력) 강제 플래그
  static Future<bool> getForceKakaoRelogin() async =>
      await _storage.read(key: _forceKakaoReloginKey) == 'true';
  static Future<void> setForceKakaoRelogin() =>
      _storage.write(key: _forceKakaoReloginKey, value: 'true');
  static Future<void> clearForceKakaoRelogin() =>
      _storage.delete(key: _forceKakaoReloginKey);
}
