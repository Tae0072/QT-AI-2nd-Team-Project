/// Auth refresh 과정에서 발생하는 예외.
///
/// [DioException.error]에 raw 문자열 대신 타입 안전한 예외를 사용하여
/// [ErrorInterceptor]의 [ApiError] envelope 파싱과 충돌을 방지한다.
class AuthRefreshException implements Exception {
  final String message;
  final AuthRefreshFailureReason reason;

  const AuthRefreshException({
    required this.message,
    required this.reason,
  });

  @override
  String toString() => 'AuthRefreshException($reason): $message';
}

/// Refresh 실패 원인 분류.
enum AuthRefreshFailureReason {
  /// 저장된 refresh token이 없음.
  noRefreshToken,

  /// 서버 응답 형식이 올바르지 않음.
  invalidResponse,

  /// 서버 응답에 토큰이 포함되지 않음.
  missingTokens,
}
