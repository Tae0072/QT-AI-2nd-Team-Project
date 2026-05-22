import 'dart:async';
import 'package:dio/dio.dart';
import '../storage/secure_storage.dart';

/// JWT 인증 인터셉터.
///
/// - 요청마다 Authorization 헤더에 accessToken을 자동 첨부한다.
/// - 401 응답 시 refreshToken으로 갱신 후 원래 요청을 재시도한다.
/// - 동시 401 처리 시 [Completer] 기반 single-flight 락으로
///   refresh가 한 번만 실행되도록 보장한다.
/// - refresh 전용 [Dio] 인스턴스를 분리하여 인터셉터 chain 재귀를 방지한다.
class AuthInterceptor extends Interceptor {
  final Dio _dio;
  late final Dio _refreshDio;

  /// 동시 401 요청의 single-flight 락.
  Completer<String>? _refreshCompleter;

  /// refresh 실패 시 호출되는 콜백.
  /// 앱 라우터에서 로그인 화면 이동 등을 등록한다.
  /// Phase 3 Auth PR에서 구현 예정.
  void Function()? onAuthFailure;

  AuthInterceptor(this._dio) {
    _refreshDio = Dio(BaseOptions(
      baseUrl: _dio.options.baseUrl,
      connectTimeout: const Duration(seconds: 5),
      receiveTimeout: const Duration(seconds: 5),
      headers: {'Content-Type': 'application/json'},
    ));
  }

  @override
  Future<void> onRequest(
      RequestOptions options, RequestInterceptorHandler handler) async {
    final token = await SecureStorage.getAccessToken();
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
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
      // TODO(Phase3-Auth): onAuthFailure 콜백으로 로그인 화면 라우팅
      onAuthFailure?.call();
      return handler.next(refreshErr);
    } catch (_) {
      await SecureStorage.clearTokens();
      onAuthFailure?.call();
      return handler.next(err);
    }
  }

  /// Single-flight 패턴: 첫 번째 401이 refresh를 실행하고,
  /// 동시에 도착하는 다른 401은 같은 [Completer]를 await한다.
  ///
  /// 락 해제 타이밍: completer.future가 완료된 후에 _refreshCompleter를
  /// null로 초기화한다. finally에서 즉시 null로 두면 동일 마이크로태스크에서
  /// 두 번째 호출이 새 refresh를 트리거할 수 있으므로, future 참조를 로컬로
  /// 캡처한 뒤 await 완료 후 해제한다.
  Future<String> _refreshTokenSingleFlight() async {
    if (_refreshCompleter != null) {
      // 이미 진행 중인 refresh가 있으면 해당 future를 로컬 캡처 후 await.
      final completer = _refreshCompleter!;
      return completer.future;
    }

    final completer = Completer<String>();
    _refreshCompleter = completer;
    try {
      final newAccessToken = await _executeRefresh();
      completer.complete(newAccessToken);
      return newAccessToken;
    } catch (e) {
      completer.completeError(e);
      rethrow;
    } finally {
      // completer.future가 완료된 시점에서 락 해제.
      // complete/completeError 후이므로 대기 중인 호출자들은 이미 결과를 받았다.
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
        error: 'Refresh 응답에 토큰이 포함되지 않았습니다.',
      );
    }

    await SecureStorage.setAccessToken(newAccess);
    await SecureStorage.setRefreshToken(newRefresh);
    return newAccess;
  }
}