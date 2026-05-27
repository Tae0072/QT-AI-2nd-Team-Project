import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/core/network/error_interceptor.dart';

void main() {
  late ErrorInterceptor interceptor;

  setUp(() {
    interceptor = ErrorInterceptor();
  });

  group('ErrorInterceptor', () {
    test('표준 에러 envelope — 예외 없이 파싱 성공', () {
      final requestOptions = RequestOptions(path: '/test');
      final response = Response(
        requestOptions: requestOptions,
        statusCode: 404,
        data: {
          'success': false,
          'error': {'code': 'M0001', 'message': '회원을 찾을 수 없습니다.'},
          'traceId': 'abc-123',
        },
      );

      final err = DioException(
        requestOptions: requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
      );

      expect(
        () => interceptor.onError(err, ErrorInterceptorHandler()),
        returnsNormally,
      );
    });

    test('error code가 int일 때 String으로 변환', () {
      final requestOptions = RequestOptions(path: '/test');
      final response = Response(
        requestOptions: requestOptions,
        statusCode: 400,
        data: {
          'success': false,
          'error': {'code': 1001, 'message': '잘못된 요청'},
        },
      );

      final err = DioException(
        requestOptions: requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
      );

      expect(
        () => interceptor.onError(err, ErrorInterceptorHandler()),
        returnsNormally,
      );
    });

    test('비표준 응답 — envelope 없는 경우 원본 에러 전달', () {
      final requestOptions = RequestOptions(path: '/test');
      final response = Response(
        requestOptions: requestOptions,
        statusCode: 500,
        data: 'Internal Server Error',
      );

      final err = DioException(
        requestOptions: requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
      );

      expect(
        () => interceptor.onError(err, ErrorInterceptorHandler()),
        returnsNormally,
      );
    });

    test('response가 null인 경우 원본 에러 전달', () {
      final err = DioException(
        requestOptions: RequestOptions(path: '/test'),
        type: DioExceptionType.connectionTimeout,
      );

      expect(
        () => interceptor.onError(err, ErrorInterceptorHandler()),
        returnsNormally,
      );
    });
  });

  group('ApiError', () {
    test('toString 포맷 — traceId 포함', () {
      const error = ApiError(
        code: 'M0001',
        message: '회원을 찾을 수 없습니다.',
        traceId: 'abc-123',
      );
      expect(
          error.toString(), '[M0001] 회원을 찾을 수 없습니다. (trace: abc-123)');
    });

    test('toString 포맷 — traceId 없음', () {
      const error = ApiError(code: 'C0001', message: '서버 오류');
      expect(error.toString(), '[C0001] 서버 오류');
    });
  });
}
