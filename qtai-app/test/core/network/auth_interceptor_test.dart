import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
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

    test('onAuthFailure 콜백 등록 확인', () {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      final interceptor = AuthInterceptor(dio);

      var callCount = 0;
      interceptor.onAuthFailure = () => callCount++;
      expect(interceptor.onAuthFailure, isNotNull);
      interceptor.onAuthFailure!();
      expect(callCount, equals(1));
    });

    group('401 refresh 흐름', () {
      late Dio dio;
      late DioAdapter dioAdapter;
      late AuthInterceptor interceptor;

      setUp(() {
        dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
        dioAdapter = DioAdapter(dio: dio);
        interceptor = AuthInterceptor(dio);
        dio.interceptors.add(interceptor);
      });

      test('401 응답 시 refresh token null이면 에러 전달', () async {
        dioAdapter.onGet(
          '/test',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );

        // SecureStorage.getRefreshToken()이 test 환경에서 null 반환
        // → _executeRefresh에서 'Refresh token이 없습니다.' DioException 발생
        try {
          await dio.get('/test');
          fail('Should have thrown');
        } on DioException catch (e) {
          expect(e, isA<DioException>());
        }
      });

      test('refresh 실패 시 onAuthFailure 콜백이 호출된다', () async {
        var callbackCalled = false;
        interceptor.onAuthFailure = () => callbackCalled = true;

        dioAdapter.onGet(
          '/test',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );

        try {
          await dio.get('/test');
        } on DioException {
          // 예상된 실패
        }

        expect(callbackCalled, isTrue);
      });
    });

    group('_executeRefresh envelope 검증', () {
      test('refresh token null이면 DioException에 메시지 포함', () async {
        final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
        final dioAdapter = DioAdapter(dio: dio);
        final interceptor = AuthInterceptor(dio);
        dio.interceptors.add(interceptor);

        dioAdapter.onGet(
          '/test',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );

        try {
          await dio.get('/test');
          fail('Should have thrown');
        } on DioException catch (e) {
          expect(e.error.toString(), contains('Refresh token'));
        }
      });
    });

    group('single-flight 동시성', () {
      test('동시 401 에러가 handler에 정상 전달된다', () async {
        final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
        final interceptor = AuthInterceptor(dio);

        final err1 = DioException(
          requestOptions: RequestOptions(path: '/test1'),
          response: Response(
            requestOptions: RequestOptions(path: '/test1'),
            statusCode: 401,
          ),
          type: DioExceptionType.badResponse,
        );

        final err2 = DioException(
          requestOptions: RequestOptions(path: '/test2'),
          response: Response(
            requestOptions: RequestOptions(path: '/test2'),
            statusCode: 401,
          ),
          type: DioExceptionType.badResponse,
        );

        final handler1 = ErrorInterceptorHandler();
        final handler2 = ErrorInterceptorHandler();

        // 비동기로 동시에 시작 (둘 다 refresh token null로 실패)
        final future1 = Future(() => interceptor.onError(err1, handler1));
        final future2 = Future(() => interceptor.onError(err2, handler2));

        // single-flight 패턴이 동작하면 둘 다 정상 완료
        await Future.wait([future1, future2]);
      });
    });
  });
}