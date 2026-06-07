import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:qtai_app/core/network/error_interceptor.dart';

void main() {
  late Dio dio;
  late DioAdapter dioAdapter;

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
    dioAdapter = DioAdapter(dio: dio);
    dio.interceptors.add(ErrorInterceptor());
  });

  group('ErrorInterceptor', () {
    test('2xx인데 success=false면 ApiError로 변환해 에러로 던진다 (P1-12 Null cast 방지)', () async {
      dioAdapter.onGet(
        '/test',
        (server) => server.reply(200, {
          'success': false,
          'data': null,
          'error': {'code': 'C0002', 'message': '잘못된 요청'},
          'traceId': 'trace-2xx',
        }),
      );

      try {
        await dio.get('/test');
        fail('Should have thrown');
      } on DioException catch (e) {
        expect(e.error, isA<ApiError>());
        final apiError = e.error as ApiError;
        expect(apiError.code, 'C0002');
        expect(apiError.traceId, 'trace-2xx');
      }
    });

    test('2xx success=true는 정상 통과한다', () async {
      dioAdapter.onGet(
        '/test',
        (server) => server.reply(200, {'success': true, 'data': {'ok': 1}}),
      );

      final res = await dio.get('/test');
      expect(res.statusCode, 200);
      expect((res.data as Map)['success'], true);
    });

    test('표준 에러 envelope — ApiError로 변환', () async {
      dioAdapter.onGet(
        '/test',
        (server) => server.reply(404, {
          'success': false,
          'error': {'code': 'M0001', 'message': '회원을 찾을 수 없습니다.'},
          'traceId': 'abc-123',
        }),
      );

      try {
        await dio.get('/test');
        fail('Should have thrown');
      } on DioException catch (e) {
        expect(e.error, isA<ApiError>());
        final apiError = e.error as ApiError;
        expect(apiError.code, 'M0001');
        expect(apiError.message, '회원을 찾을 수 없습니다.');
        expect(apiError.traceId, 'abc-123');
      }
    });

    test('error code가 int일 때 String으로 변환', () async {
      dioAdapter.onGet(
        '/test',
        (server) => server.reply(400, {
          'success': false,
          'error': {'code': 1001, 'message': '잘못된 요청'},
        }),
      );

      try {
        await dio.get('/test');
        fail('Should have thrown');
      } on DioException catch (e) {
        expect(e.error, isA<ApiError>());
        final apiError = e.error as ApiError;
        expect(apiError.code, '1001');
        expect(apiError.message, '잘못된 요청');
        expect(apiError.traceId, isNull);
      }
    });

    test('비표준 응답 — envelope 없는 경우 원본 에러 전달', () async {
      dioAdapter.onGet(
        '/test',
        (server) => server.reply(500, 'Internal Server Error'),
      );

      try {
        await dio.get('/test');
        fail('Should have thrown');
      } on DioException catch (e) {
        // envelope이 없으므로 ApiError로 변환되지 않음
        expect(e.error, isNot(isA<ApiError>()));
        expect(e.response?.statusCode, 500);
      }
    });

    test('response가 null인 경우 원본 에러 전달', () async {
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
        expect(e.error, isNot(isA<ApiError>()));
        expect(e.type, DioExceptionType.connectionTimeout);
      }
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
