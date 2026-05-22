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
    headers: {'Content-Type': 'application/json'},
  ));

  dio.interceptors.addAll([
    AuthInterceptor(dio),
    ErrorInterceptor(),
    if (AppConfig.instance.isDev)
      LogInterceptor(
        requestBody: true,
        responseBody: true,
        requestHeader: false, // Authorization 헤더 노출 방지
      ),
  ]);

  return dio;
});