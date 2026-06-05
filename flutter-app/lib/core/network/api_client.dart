import 'dart:developer' as dev;
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../config/app_config.dart';
import 'auth_interceptor.dart';
import 'error_interceptor.dart';

final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: AppConfig.instance.baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
    headers: {
      'Content-Type': 'application/json',
      if (AppConfig.instance.isDev) 'X-Dev-User-Id': '1',
    },
  ));

  dio.interceptors.addAll([
    AuthInterceptor(dio),
    ErrorInterceptor(),
    if (AppConfig.instance.isDev) _DevLogInterceptor(),
  ]);

  return dio;
});

/// dev 전용 로그 인터셉터.
///
/// [LogInterceptor] 대신 직접 구현하여 /auth/* 경로의
/// 응답 body(토큰 포함 가능)를 마스킹한다.
class _DevLogInterceptor extends Interceptor {
  static const _authPaths = ['/auth/kakao', '/auth/refresh', '/auth/logout'];

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final hasAuth = options.headers.containsKey('Authorization');
    dev.log(
      '[REQ] ${options.method} ${options.path} auth=${hasAuth ? 'present' : 'missing'}',
      name: 'Dio',
    );
    if (options.data != null) {
      final data = options.data.toString();
      dev.log('[REQ BODY] ${_maskIfAuth(options.path, data)}', name: 'Dio');
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    final path = response.requestOptions.path;
    dev.log('[RES] ${response.statusCode} $path', name: 'Dio');
    if (response.data != null) {
      final data = response.data.toString();
      dev.log('[RES BODY] ${_maskIfAuth(path, data)}', name: 'Dio');
    }
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    dev.log(
      '[ERR] ${err.response?.statusCode ?? 'N/A'} ${err.requestOptions.path}',
      name: 'Dio',
    );
    handler.next(err);
  }

  String _maskIfAuth(String path, String body) {
    if (_authPaths.any((p) => path.contains(p))) {
      return '*** AUTH RESPONSE MASKED ***';
    }
    return body;
  }
}
