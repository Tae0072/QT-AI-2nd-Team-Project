import 'package:dio/dio.dart';

class ErrorInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final response = err.response;
    if (response != null && response.data is Map<String, dynamic>) {
      final data = response.data as Map<String, dynamic>;
      final error = data['error'];
      if (error is Map<String, dynamic>) {
        final code = error['code'] ?? 'UNKNOWN';
        final message = error['message'] ?? '알 수 없는 오류가 발생했습니다.';
        handler.next(DioException(
          requestOptions: err.requestOptions,
          response: err.response,
          type: err.type,
          error: ApiError(code: code, message: message),
        ));
        return;
      }
    }
    handler.next(err);
  }
}

class ApiError {
  final String code;
  final String message;

  const ApiError({required this.code, required this.message});

  @override
  String toString() => '[$code] $message';
}