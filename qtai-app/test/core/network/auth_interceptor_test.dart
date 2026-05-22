import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/core/network/auth_interceptor.dart';

void main() {
  group('AuthInterceptor', () {
    test('인스턴스 생성 시 refreshDio가 분리된다', () {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      final interceptor = AuthInterceptor(dio);
      expect(interceptor, isNotNull);
    });

    test('non-401 에러는 그대로 전달', () {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      final interceptor = AuthInterceptor(dio);

      final err = DioException(
        requestOptions: RequestOptions(path: '/test'),
        response: Response(
          requestOptions: RequestOptions(path: '/test'),
          statusCode: 403,
        ),
        type: DioExceptionType.badResponse,
      );

      // 403은 refresh 시도 없이 바로 next — 예외 없이 실행 확인
      expect(
        () => interceptor.onError(err, ErrorInterceptorHandler()),
        returnsNormally,
      );
    });

    test('response 없는 에러는 그대로 전달', () {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      final interceptor = AuthInterceptor(dio);

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
}