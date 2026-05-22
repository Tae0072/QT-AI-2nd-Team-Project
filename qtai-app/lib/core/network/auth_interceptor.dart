import 'dart:async';
import 'package:dio/dio.dart';
import '../storage/secure_storage.dart';

/// JWT 인증 인터셉터.
///
/// - 요청마다 Authorization 헤더에 accessToken을 자동 첨부한다.
/// - 401 응답 시 refreshToken으로 갱신 후 원래 요청을 재시도한다.
/// - 동시 401 처리 시 [Completer] 기반 single-flight 락으로
///   refresh가 한 번만 호출되도록 보장한다.
/// - refresh 전용 [Dio] 인스턴스를 분리하여 인터셉터 chain 재귀를 방지한다.
class AuthInterceptor extends Interceptor {
  final Dio _dio;
  late final Dio _refreshDio;

  /// 동시 401 요청의 single-flight 락
  Completer<String>? _refreshCompleter;

  AuthInterceptor(this._dio) {
    _refreshDio = Dio(BaseOptions(
      baseUrl: _dio.options.baseUrl,
      connectTimeout: const Duration(seconds: 5),
      receiveTimeout: const Duration(seconds: 5),
      headers: {'Content-Type': 'application/json'},
    ));
  }

  @override
  void onRequest(
      RequestOptions options, RequestInterceptorHandler handler) async {
    final token = await SecureStorage.getAccessToken();
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode != 401) {
      return handler.next(err);
    }

    try {
      final newAccessToken = await _refreshTokenSingleFlight();
      // 원래 요청 재시도
      final opts = err.requestOptions;
      opts.headers['Authorization'] = 'Bearer $newAccessToken';
      final retryResponse = await _dio.fetch(opts);
      return handler.resolve(retryResponse);
    } on DioException catch (refreshErr) {
      await SecureStorage.clearTokens();
      return handler.next(refreshErr);
    } catch (_) {
      await SecureStorage.clearTokens();
      return handler.next(err);
    }
  }

  /// Single-flight 패턴: 첫 번째 401이 refresh를 실행하고,
  /// 동시에 도착한 다른 401은 같은 [Completer]를 await한다.
  Future<String> _refreshTokenSingleFlight() async {
    if (_refreshCompleter != null) {
      return _refreshCompleter!.future;
    }

    _refreshCompleter = Completer<String>();
    try {
      final newAccessToken = await _executeRefresh();
      _refreshCompleter!.complete(newAccessToken);
      return newAccessToken;
    } catch (e) {
      _refreshCompleter!.completeError(e);
      rethrow;
    } finally {
      _refreshCompleter = null;
    }
  }

  Future<String> _executeRefresh() async {
    final refreshToken = await SecureStorage.getRefreshToken();
    if (refreshToken == null) {
      throw DioException(
        requestOptions: RequestOptions(path: '/auth/refresh'),
        type: DioExceptionType.unknown,
        error: 'Refresh token이 없습니다.',
      );
    }

    final response = await _refreshDio.post(
      '/auth/refresh',
      data: {'refreshToken': refreshToken},
    );

    final body = response.data;
    if (body is! Map<String, dynamic>) {
      throw DioException(
        requestOptions: response.requestOptions,
        type: DioExceptionType.badResponse,
        error: 'Refresh 응답 형식이 올바르지 않습니다.',
      );
    }

    final data = body['data'];
    if (data is! Map<String, dynamic>) {
      throw DioException(
        requestOptions: response.requestOptions,
        type: DioExceptionType.badResponse,
        error: 'Refresh 응답 data 필드가 없습니다.',
      );
    }

    final newAccess = data['accessToken'];
    final newRefresh = data['refreshToken'];
    if (newAccess is! String || newRefresh is! String) {
      throw DioException(
        requestOptions: response.requestOptions,
        type: DioExceptionType.badResponse,
        error: 'Refresh 응답에 토큰이 누락되었습니다.',
      );
    }

    await SecureStorage.setAccessToken(newAccess);
    await SecureStorage.setRefreshToken(newRefresh);
    return newAccess;
  }
}