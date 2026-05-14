import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Dio 클라이언트 + JWT/Refresh Interceptor 표준.
///
/// 08_프론트엔드_Flutter_가이드 § 6.4:
/// - Authorization 헤더 자동 부착
/// - 401 시 Mutex로 refresh 직렬화 후 재시도
/// - RFC 9457 ProblemDetail 매핑은 application 레이어에서.
///
/// TODO(김지민):
/// - synchronized 패키지로 refresh race 차단
/// - retry 1회 + 실패 시 /login 으로 리다이렉트
final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: const String.fromEnvironment(
      'QTAI_BASE_URL',
      defaultValue: 'http://10.0.2.2:8080', // Android 에뮬레이터 → 호스트
    ),
    connectTimeout: const Duration(seconds: 5),
    receiveTimeout: const Duration(seconds: 30),
  ));

  const storage = FlutterSecureStorage();
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      final token = await storage.read(key: 'qtai_access_token');
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      return handler.next(options);
    },
    onError: (e, handler) async {
      // TODO: 401 → /auth/refresh → 동일 요청 retry
      return handler.next(e);
    },
  ));

  return dio;
});

/// secure_storage 키 표준 (08_프론트엔드 § 7.3).
abstract class StorageKeys {
  static const accessToken = 'qtai_access_token';
  static const refreshToken = 'qtai_refresh_token';
  static const userId = 'qtai_user_id';
}
