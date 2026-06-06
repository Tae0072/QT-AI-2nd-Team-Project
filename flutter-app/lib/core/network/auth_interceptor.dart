import 'dart:async';
import 'package:dio/dio.dart';
import '../storage/secure_storage.dart';
import 'auth_refresh_exception.dart';

/// JWT 인증 인터셉터.
///
/// - 요청마다 Authorization 헤더에 accessToken을 자동 첨부한다.
/// - 401 응답 시 refreshToken으로 갱신 후 원래 요청을 재시도한다.
/// - 동시 401 처리 시 [Completer] 기반 single-flight 락으로
///   refresh가 한 번만 실행되도록 보장한다.
/// - refresh 전용 [Dio] 인스턴스를 분리하여 인터셉터 chain 재귀를 방지한다.
/// - 재시도 요청이 다시 401을 받는 무한 루프를 `_retried` extra flag로 방지한다.
class AuthInterceptor extends Interceptor {
  static void Function()? globalOnAuthFailure;

  final Dio _dio;
  late final Dio _refreshDio;

  /// 동시 401 요청의 single-flight 락.
  Completer<String>? _refreshCompleter;

  /// refresh 실패 시 호출되는 콜백.
  /// 실제 라우터에서 로그인 화면 이동 등을 등록한다.
  /// Phase 3 Auth PR에서 구현 예정.
  void Function()? onAuthFailure;

  /// 재시도 표시용 extra key. 이 플래그가 true인 요청이 401을 받으면
  /// refresh를 시도하지 않고 즉시 에러를 전달한다.
  static const _retriedKey = '_authRetried';

  /// 인증 엔드포인트 — 여기서의 401은 "로그인/재발급 실패"이지 만료가 아니므로
  /// refresh·전역 로그아웃을 트리거하지 않고 그대로 호출부에 전달한다(P1-12).
  static const _authPaths = ['/auth/kakao', '/auth/refresh', '/auth/logout'];

  bool _isAuthPath(String path) => _authPaths.any(path.contains);

  /// refresh 호출 횟수 추적 (테스트에서 single-flight 검증용).
  int refreshCallCount = 0;

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
  Future<void> onError(
      DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode != 401) {
      return handler.next(err);
    }

    // /auth/** 의 401은 로그인/재발급 실패다 — refresh·전역 로그아웃을 트리거하지 않고
    // 그대로 전달해 호출부가 "로그인 실패" 안내를 할 수 있게 한다 (P1-12).
    if (_isAuthPath(err.requestOptions.path)) {
      return handler.next(err);
    }

    // 이미 재시도한 요청이 다시 401을 받으면 무한 루프 방지
    if (err.requestOptions.extra[_retriedKey] == true) {
      await SecureStorage.clearTokens();
      _notifyAuthFailure();
      return handler.next(err);
    }

    try {
      final newAccessToken = await _refreshTokenSingleFlight();
      // 원래 요청 재시도 — copyWith로 불변성 보장, _retried 플래그 추가
      final retryOptions = err.requestOptions.copyWith(
        headers: {
          ...err.requestOptions.headers,
          'Authorization': 'Bearer $newAccessToken',
        },
        extra: {
          ...err.requestOptions.extra,
          _retriedKey: true,
        },
      );
      final retryResponse = await _dio.fetch(retryOptions);
      return handler.resolve(retryResponse);
    } on AuthRefreshException {
      await SecureStorage.clearTokens();
      _notifyAuthFailure();
      return handler.next(err);
    } on DioException catch (refreshErr) {
      await SecureStorage.clearTokens();
      _notifyAuthFailure();
      return handler.next(refreshErr);
    }
  }

  void _notifyAuthFailure() {
    onAuthFailure?.call();
    globalOnAuthFailure?.call();
  }

  /// Single-flight 패턴: 첫 번째 401이 refresh를 실행하고,
  /// 동시에 진입하는 다른 401은 같은 [Completer]를 await한다.
  ///
  /// 설계 의도: `finally` 블록에서 `_refreshCompleter = null`로 초기화한다.
  /// `complete()`/`completeError()` 호출 시점과 이 사이에 끼어드는 `await`이
  /// 없지만, Dart의 이벤트 루프 특성상 `finally`와 같은 마이크로태스크
  /// 내에서 실행되므로 이 사이에 결과를 받기 전에 null이 될 수 있다.
  /// 이를 방지하기 위해 대기자는 로컬 변수에 캡처한 completer.future를
  /// await하므로, `_refreshCompleter`가 null이 되어도 안전하다.
  Future<String> _refreshTokenSingleFlight() async {
    if (_refreshCompleter != null) {
      // 이미 진행 중인 refresh가 있으면 로컬 캡처 후 await.
      // _refreshCompleter가 finally에서 null이 되어도 로컬 참조는 유효.
      final completer = _refreshCompleter!;
      return completer.future;
    }

    final completer = Completer<String>();
    _refreshCompleter = completer;
    // 단일 요청 시 completer.future를 await하는 대기자가 없으므로
    // completeError() 호출 시 unhandled future error 방지
    completer.future.ignore();
    try {
      final newAccessToken = await _executeRefresh();
      completer.complete(newAccessToken);
      return newAccessToken;
    } catch (e) {
      completer.completeError(e);
      rethrow;
    } finally {
      _refreshCompleter = null;
    }
  }

  Future<String> _executeRefresh() async {
    refreshCallCount++;

    final refreshToken = await SecureStorage.getRefreshToken();
    if (refreshToken == null) {
      throw AuthRefreshException(
        message: 'Refresh token이 없습니다.',
        reason: AuthRefreshFailureReason.noRefreshToken,
      );
    }

    late final Response response;
    try {
      response = await _refreshDio.post(
        '/auth/refresh',
        data: {'refreshToken': refreshToken},
      );
    } on DioException catch (e) {
      // 401 응답 = refresh token 만료 → 명시적 분기
      if (e.response?.statusCode == 401) {
        throw AuthRefreshException(
          message: 'Refresh token이 만료되었습니다.',
          reason: AuthRefreshFailureReason.noRefreshToken,
        );
      }
      rethrow;
    }

    final body = response.data;
    if (body is! Map<String, dynamic>) {
      throw AuthRefreshException(
        message: 'Refresh 응답 형식이 올바르지 않습니다.',
        reason: AuthRefreshFailureReason.invalidResponse,
      );
    }

    final data = body['data'];
    if (data is! Map<String, dynamic>) {
      throw AuthRefreshException(
        message: 'Refresh 응답 data 필드가 없습니다.',
        reason: AuthRefreshFailureReason.invalidResponse,
      );
    }

    // 04_API_명세서 기준 camelCase: accessToken, refreshToken (명세서 §2.3 응답 예시)
    final newAccess = data['accessToken'];
    final newRefresh = data['refreshToken'];
    if (newAccess is! String || newRefresh is! String) {
      throw AuthRefreshException(
        message: 'Refresh 응답에 토큰이 포함되지 않았습니다.',
        reason: AuthRefreshFailureReason.missingTokens,
      );
    }

    await SecureStorage.setAccessToken(newAccess);
    await SecureStorage.setRefreshToken(newRefresh);
    return newAccess;
  }
}
