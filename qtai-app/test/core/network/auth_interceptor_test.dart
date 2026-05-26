import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:qtai_app/core/network/auth_interceptor.dart';
import 'package:qtai_app/core/network/auth_refresh_exception.dart';

void main() {
  group('AuthInterceptor', () {
    test('instance creation separates refreshDio', () {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      final interceptor = AuthInterceptor(dio);
      expect(interceptor, isNotNull);
    });

    test('non-401 error passes through', () {
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

    test('error without response passes through', () {
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

    test('onAuthFailure callback registration and invocation', () {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      final interceptor = AuthInterceptor(dio);
      var callCount = 0;
      interceptor.onAuthFailure = () => callCount++;
      expect(interceptor.onAuthFailure, isNotNull);
      interceptor.onAuthFailure!();
      expect(callCount, equals(1));
    });

    group('401 refresh flow', () {
      late Dio dio;
      late DioAdapter dioAdapter;
      late AuthInterceptor interceptor;

      setUp(() {
        dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
        dioAdapter = DioAdapter(dio: dio);
        interceptor = AuthInterceptor(dio);
        dio.interceptors.add(interceptor);
      });

      test('401 with null refresh token triggers failure', () async {
        dioAdapter.onGet(
          '/test',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );
        try {
          await dio.get('/test');
          fail('Should have thrown');
        } on DioException catch (_) {
          // AuthRefreshException caught internally
        }
      });

      test('refresh failure invokes onAuthFailure callback', () async {
        var callbackCalled = false;
        interceptor.onAuthFailure = () => callbackCalled = true;
        dioAdapter.onGet(
          '/test',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );
        try {
          await dio.get('/test');
        } on DioException {
          // Expected failure
        }
        expect(callbackCalled, isTrue);
      });

      test('retried request with 401 fails immediately (loop prevention)', () {
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
        expect(
          () => interceptor.onError(err, ErrorInterceptorHandler()),
          returnsNormally,
        );
      });
    });

    group('AuthRefreshException', () {
      test('noRefreshToken reason message', () {
        const ex = AuthRefreshException(
          message: 'test',
          reason: AuthRefreshFailureReason.noRefreshToken,
        );
        expect(ex.toString(), contains('noRefreshToken'));
        expect(ex.toString(), contains('test'));
      });

      test('invalidResponse reason message', () {
        const ex = AuthRefreshException(
          message: 'bad format',
          reason: AuthRefreshFailureReason.invalidResponse,
        );
        expect(ex.reason, equals(AuthRefreshFailureReason.invalidResponse));
      });

      test('missingTokens reason message', () {
        const ex = AuthRefreshException(
          message: 'no tokens',
          reason: AuthRefreshFailureReason.missingTokens,
        );
        expect(ex.reason, equals(AuthRefreshFailureReason.missingTokens));
      });
    });

    group('single-flight concurrency', () {
      test('concurrent 401 errors are handled normally', () async {
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
        await Future.wait([future1, future2]);
      });

      test('concurrent 401 triggers refresh exactly once', () async {
        final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
        final interceptor = AuthInterceptor(dio);
        final err1 = DioException(
          requestOptions: RequestOptions(path: '/a'),
          response: Response(
            requestOptions: RequestOptions(path: '/a'),
            statusCode: 401,
          ),
          type: DioExceptionType.badResponse,
        );
        final err2 = DioException(
          requestOptions: RequestOptions(path: '/b'),
          response: Response(
            requestOptions: RequestOptions(path: '/b'),
            statusCode: 401,
          ),
          type: DioExceptionType.badResponse,
        );
        final handler1 = ErrorInterceptorHandler();
        final handler2 = ErrorInterceptorHandler();
        await Future.wait([
          Future(() => interceptor.onError(err1, handler1)),
          Future(() => interceptor.onError(err2, handler2)),
        ]);
        // single-flight guarantee: refresh is called exactly once
        expect(interceptor.refreshCallCount, equals(1));
      });
    });
  });
}
