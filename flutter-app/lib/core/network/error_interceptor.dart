import 'package:dio/dio.dart';

/// API 에러 응답 envelope 파싱 인터셉터.
///
/// 서버 표준 응답 `{success, data, error{code, message}, traceId}`에서
/// error 필드를 추출하여 [ApiError]로 변환한다.
class ErrorInterceptor extends Interceptor {
  /// 2xx인데 success=false인 응답을 에러로 변환한다 (P1-12).
  ///
  /// 서버 에러는 보통 non-2xx로 오지만, 방어적으로 2xx+success:false를 처리하지 않으면
  /// 호출부가 null인 data에 인덱싱해 Null cast로 죽는다. 이를 DioException(ApiError)로
  /// 바꿔 일반 에러 흐름을 타게 한다.
  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    final data = response.data;
    if (data is Map<String, dynamic> && data['success'] == false) {
      final error = data['error'];
      final code = error is Map<String, dynamic>
          ? '${error['code'] ?? 'UNKNOWN'}'
          : 'UNKNOWN';
      final message = error is Map<String, dynamic>
          ? '${error['message'] ?? '예상치 못한 오류가 발생했습니다.'}'
          : '예상치 못한 오류가 발생했습니다.';
      final traceId = data['traceId']?.toString();
      handler.reject(DioException(
        requestOptions: response.requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
        error: ApiError(code: code, message: message, traceId: traceId),
      ));
      return;
    }
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final response = err.response;
    if (response != null && response.data is Map<String, dynamic>) {
      final data = response.data as Map<String, dynamic>;
      final error = data['error'];
      if (error is Map<String, dynamic>) {
        final code = '${error['code'] ?? 'UNKNOWN'}';
        final message = '${error['message'] ?? '예상치 못한 오류가 발생했습니다.'}';
        final traceId = data['traceId']?.toString();
        handler.next(DioException(
          requestOptions: err.requestOptions,
          response: err.response,
          type: err.type,
          error: ApiError(code: code, message: message, traceId: traceId),
        ));
        return;
      }
      if (data.containsKey('code') || data.containsKey('message')) {
        final code = '${data['code'] ?? 'UNKNOWN'}';
        final message = '${data['message'] ?? 'Unexpected error occurred.'}';
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
  String toString() =>
      '[$code] $message${traceId != null ? ' (trace: $traceId)' : ''}';
}
