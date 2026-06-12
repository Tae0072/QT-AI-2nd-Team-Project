import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:qtai_app/features/note/services/note_repository.dart';

/// NoteRepository.getNotes의 category/status 쿼리 파라미터 회귀 테스트.
/// (QA ⑤⑧ — 상태 필터 추가에 따른 신규 동작)
void main() {
  late Dio dio;
  late DioAdapter dioAdapter;
  late NoteRepository repository;

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'http://localhost/api/v1'));
    dioAdapter = DioAdapter(dio: dio);
    repository = NoteRepository(dio);
  });

  Map<String, dynamic> emptyPage() => {
        'success': true,
        'data': {'content': <dynamic>[], 'last': true},
      };

  group('getNotes 쿼리 파라미터', () {
    test('category·status를 주면 둘 다 쿼리로 전송한다', () async {
      // queryParameters가 정확히 일치할 때만 매칭되므로, status가 실려야 이 라우트가 잡힌다.
      dioAdapter.onGet(
        '/notes',
        (server) => server.reply(200, emptyPage()),
        queryParameters: {
          'category': 'PRAYER',
          'status': 'SAVED',
          'page': 0,
          'size': 20,
        },
      );

      final result =
          await repository.getNotes(category: 'PRAYER', status: 'SAVED');

      expect(result.items, isEmpty);
      expect(result.hasNext, isFalse);
    });

    test('status가 null이면 status 파라미터를 보내지 않는다(전체)', () async {
      // status 키 없이 등록 → 요청이 page/size만 보낼 때만 매칭. status가 섞이면 매칭 실패해
      // DioException이 나므로, 이 테스트 통과 자체가 "status 미전송"을 증명한다.
      dioAdapter.onGet(
        '/notes',
        (server) => server.reply(200, emptyPage()),
        queryParameters: {'page': 0, 'size': 20},
      );

      final result = await repository.getNotes();

      expect(result.items, isEmpty);
    });

    test('category만 주면 category만 전송한다', () async {
      dioAdapter.onGet(
        '/notes',
        (server) => server.reply(200, emptyPage()),
        queryParameters: {'category': 'GRATITUDE', 'page': 0, 'size': 20},
      );

      final result = await repository.getNotes(category: 'GRATITUDE');

      expect(result.items, isEmpty);
    });
  });
}
