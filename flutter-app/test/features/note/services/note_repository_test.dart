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

  group('verseIds 저장 (QA ⑪)', () {
    test('create는 verseIds를 본문에 포함해 전송한다', () async {
      // data가 정확히 일치할 때만 매칭 → verseIds가 실려야 잡힌다.
      dioAdapter.onPost(
        '/notes',
        (server) => server.reply(201, {
          'success': true,
          'data': {
            'id': 1,
            'category': 'SERMON',
            'status': 'SAVED',
            'visibility': 'PRIVATE',
          },
        }),
        data: {
          'category': 'SERMON',
          'title': '설교',
          'body': '본문',
          'verseIds': [101, 102],
          'status': 'SAVED',
          'visibility': 'PRIVATE',
        },
      );

      final result = await repository.create(
        category: 'SERMON',
        title: '설교',
        body: '본문',
        verseIds: [101, 102],
      );

      expect(result.id, 1);
    });

    test('update는 verseIds를 본문에 포함해 전송한다(보존)', () async {
      dioAdapter.onPatch(
        '/notes/7',
        (server) => server.reply(200, {'success': true, 'data': {}}),
        data: {
          'title': '설교',
          'body': '본문',
          'verseIds': [500],
          'status': 'SAVED',
          'visibility': 'PRIVATE',
        },
      );

      await repository.update(7, title: '설교', body: '본문', verseIds: [500]);
    });

    test('update에 verseIds를 안 주면 키를 생략한다(절 전체 삭제 함정 방지)',
        () async {
      // 정확히 이 body(=verseIds 키 없음)일 때만 매칭. update가 빈 배열이라도 보내면
      // 매칭 실패로 DioException → 테스트 실패. 통과 자체가 "키 생략"을 증명한다.
      dioAdapter.onPatch(
        '/notes/7',
        (server) => server.reply(200, {'success': true, 'data': {}}),
        data: {
          'title': '설교',
          'body': '본문',
          'status': 'SAVED',
          'visibility': 'PRIVATE',
        },
      );

      await repository.update(7, title: '설교', body: '본문');
    });

    test('create도 verseIds를 안 주면 키를 생략한다', () async {
      dioAdapter.onPost(
        '/notes',
        (server) => server.reply(201, {
          'success': true,
          'data': {
            'id': 2,
            'category': 'PRAYER',
            'status': 'SAVED',
            'visibility': 'PRIVATE',
          },
        }),
        data: {
          'category': 'PRAYER',
          'title': '기도',
          'body': '본문',
          'status': 'SAVED',
          'visibility': 'PRIVATE',
        },
      );

      final result = await repository.create(
        category: 'PRAYER',
        title: '기도',
        body: '본문',
      );

      expect(result.id, 2);
    });
  });

  group('deleteMany 다중 삭제 (QA ①)', () {
    test('성공/실패(DioException)를 구분해 실패 id만 반환한다', () async {
      dioAdapter.onDelete('/notes/1', (server) => server.reply(200, {}));
      dioAdapter.onDelete(
          '/notes/2', (server) => server.reply(500, {})); // 5xx → DioException

      final failed = await repository.deleteMany([1, 2]);

      expect(failed, [2]); // 1은 성공, 2만 실패
    });

    test('전부 성공하면 빈 목록을 반환한다', () async {
      dioAdapter.onDelete('/notes/1', (server) => server.reply(200, {}));
      dioAdapter.onDelete('/notes/2', (server) => server.reply(200, {}));

      final failed = await repository.deleteMany([1, 2]);

      expect(failed, isEmpty);
    });
  });
}
