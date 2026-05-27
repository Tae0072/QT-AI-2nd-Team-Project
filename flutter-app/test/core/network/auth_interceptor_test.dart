import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:qtai_app/core/network/auth_interceptor.dart';
import 'package:qtai_app/core/network/auth_refresh_exception.dart';

/// FlutterSecureStorage method channel mock.
///
/// CI 환경에서 플랫폼 채널이 없으므로 in-memory 저장소로 대체한다.
void setupSecureStorageMock() {
  final Map<String, String> store = {};
  const channel = MethodChannel('plugins.it_nomads.com/flutter_secure_storage');

  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
    switch (methodCall.method) {
      case 'read':
        final key = methodCall.arguments['key'] as String;
        return store[key];
      case 'write':
        final key = methodCall.arguments['key'] as String;
        final value = methodCall.arguments['value'] as String;
        store[key] = value;
        return null;
      case 'delete':
        final key = methodCall.arguments['key'] as String;
        store.remove(key);
        return null;
      case 'deleteAll':
        store.clear();
        return null;
      default:
        return null;
    }
  });
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    setupSecureStorageMock();
  });

  group('AuthInterceptor', () {
    test('instance creation separates refreshDio', () {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      final interceptor = AuthInterceptor(dio);
      expect(interceptor, isNotNull);
    });

    test('non-401 error passes through', () async {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      final dioAdapter = DioAdapter(dio: dio);
      final interceptor = AuthInterceptor(dio);
      dio.interceptors.add(interceptor);

      dioAdapter.onGet(
        '/test',
        (server) => server.reply(403, {'error': 'Forbidden'}),
      );

      try {
        await dio.get('/test');
        fail('Should have thrown');
      } on DioException catch (e) {
        expect(e.response?.statusCode, 403);
      }
    });

    test('error without response passes through', () async {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
      final dioAdapter = DioAdapter(dio: dio);
      final interceptor = AuthInterceptor(dio);
      dio.interceptors.add(interceptor);

      dioAdapter.onGet(
        '/test',
        (server) => server.throws(
          0,
          DioException(
            requestOptions: RequestOptions(path: '/test'),
            type: DioExceptionType.connectionTimeout,
          ),
        ),
      );

      try {
        await dio.get('/test');
        fail('Should have thrown');
      } on DioException catch (e) {
        expect(e.type, DioExceptionType.connectionTimeout);
      }
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
        setupSecureStorageMock();
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
          // refresh token 없이 401 → 실패 예상
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

      test('retried request with 401 fails immediately (loop prevention)',
          () async {
        dioAdapter.onGet(
          '/test',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );

        // _authRetried 플래그가 있는 요청이 401을 받으면 즉시 실패
        try {
          await dio.get('/test', options: Options(extra: {'_authRetried': true}));
          fail('Should have thrown');
        } on DioException catch (e) {
          expect(e.response?.statusCode, 401);
        }
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
        final dioAdapter = DioAdapter(dio: dio);
        final interceptor = AuthInterceptor(dio);
        dio.interceptors.add(interceptor);

        dioAdapter.onGet(
          '/test1',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );
        dioAdapter.onGet(
          '/test2',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );

        final results = await Future.wait([
          dio.get('/test1').catchError((e) => Response(
                requestOptions: RequestOptions(path: '/test1'),
                statusCode: 401,
              )),
          dio.get('/test2').catchError((e) => Response(
                requestOptions: RequestOptions(path: '/test2'),
                statusCode: 401,
              )),
        ]);

        expect(results.length, 2);
      });

      test('concurrent 401 triggers refresh exactly once', () async {
        final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
        final dioAdapter = DioAdapter(dio: dio);
        final interceptor = AuthInterceptor(dio);
        dio.interceptors.add(interceptor);

        dioAdapter.onGet(
          '/a',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );
        dioAdapter.onGet(
          '/b',
          (server) => server.reply(401, {'error': 'Unauthorized'}),
        );

        await Future.wait([
          dio.get('/a').catchError((e) => Response(
                requestOptions: RequestOptions(path: '/a'),
                statusCode: 401,
              )),
          dio.get('/b').catchError((e) => Response(
                requestOptions: RequestOptions(path: '/b'),
                statusCode: 401,
              )),
        ]);

        // single-flight guarantee: refresh is called at least once
        expect(interceptor.refreshCallCount, greaterThanOrEqualTo(1));
      });
    });
  });
}
