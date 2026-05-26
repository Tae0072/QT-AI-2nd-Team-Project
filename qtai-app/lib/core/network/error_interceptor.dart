import 'package:dio/dio.dart';

/// API 에러 응답 envelope 파싱 인터셉터.
///
/// 서버 표준 응답 `{success, data, error{code, message}, traceId}`에서
/// error 필드를 추출하여 [ApiError]로 변환한다.
class ErrorInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final response = err.response;
    if (response != null && response.data is Map<String, dynamic>) {
      final data = response.data as Map<String, dynamic>;
      final error = data['error'];
      if (error is Map<String, dynamic>) {
        final code = '${error['code'] ?? 'UNKNOWN'}';
        final message =
            '${error['message'] ?? '예상치 못한 오류가 발생했습니다.'}';
        final traceId = data['traceId']?.toString();
        handler.next(DioException(
          requestOptions: err.requestOptions,
          response: err.response,
          type: err.type,
          error: ApiError(code: code, message: message, traceId: traceId),
        ));
        return;
      }
    }
    handler.next(err);
  }
}

/// API 에러 모델. [code]는 항상 String으로 정규화된다.
class ApiError {
  final String code;
  final String message;
  final String? traceId;

  const ApiError({
    required this.code,
    required this.message,
    this.traceId,
  });

  @override
  String toString() => '[$code] $message${traceId != null ? ' (trace: $traceId)' : ''}';
}
