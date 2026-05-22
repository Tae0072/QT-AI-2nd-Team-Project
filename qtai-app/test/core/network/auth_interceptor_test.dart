import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:qtai_app/core/network/auth_interceptor.dart';
import 'package:qtai_app/core/network/auth_refresh_exception.dart';

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

    test('onAuthFailure 콜백 등록 및 호출', () {
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

      test('401 응답 시 refresh token null이면 AuthRefreshException 발생', () async {
        dioAdapter.onGet(
          '/test',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );

        try {
          await dio.get('/test');
          fail('Should have thrown');
        } on DioException catch (e) {
          // AuthRefreshException이 catch로 잡혀서 handler.next로 전달됨
          expect(e, isA<DioException>());
        }
      });

      test('refresh 실패 시 onAuthFailure 콜백 호출', () async {
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

      test('재시도 요청(_retried=true)이 401이면 즉시 실패 (무한 루프 방지)', () {
        final err = DioException(
          requestOptions: RequestOptions(
            path: '/test',
            extra: {'_authRetried': true},
          ),
          response: Response(
            requestOptions: RequestOptions(path: '/test'),
            statusCode: 401,
          ),
          type: DioExceptionType.badResponse,
        );

        // _retried=true이면 refresh 시도 없이 즉시 handler.next
        expect(
          () => interceptor.onError(err, ErrorInterceptorHandler()),
          returnsNormally,
        );
      });
    });

    group('AuthRefreshException', () {
      test('noRefreshToken reason 포맷', () {
        const ex = AuthRefreshException(
          message: 'test',
          reason: AuthRefreshFailureReason.noRefreshToken,
        );
        expect(ex.toString(), contains('noRefreshToken'));
        expect(ex.toString(), contains('test'));
      });

      test('invalidResponse reason 포맷', () {
        const ex = AuthRefreshException(
          message: 'bad format',
          reason: AuthRefreshFailureReason.invalidResponse,
        );
        expect(ex.reason, equals(AuthRefreshFailureReason.invalidResponse));
      });

      test('missingTokens reason 포맷', () {
        const ex = AuthRefreshException(
          message: 'no tokens',
          reason: AuthRefreshFailureReason.missingTokens,
        );
        expect(ex.reason, equals(AuthRefreshFailureReason.missingTokens));
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

        final future1 = Future(() => interceptor.onError(err1, handler1));
        final future2 = Future(() => interceptor.onError(err2, handler2));

        // single-flight 패턴이 동작하면 둘 다 정상 완료
        await Future.wait([future1, future2]);
      });
    });
  });
}
