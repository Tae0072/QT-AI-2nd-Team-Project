// ignore_for_file: avoid_print

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';

void main() {
  late Dio dio;
  late DioAdapter dioAdapter;
  late BibleRepository repository;

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'http://localhost/api/v1'));
    dioAdapter = DioAdapter(dio: dio);
    repository = BibleRepository(dio);
  });

  group('getTodayQtPassage', () {
    test('서버 Today QT range를 기준으로 절 범위를 조회한다', () async {
      dioAdapter.onGet('/qt/today', (server) {
        server.reply(200, {
          'success': true,
          'data': {
            'qtPassageId': 7,
            'passageDate': '2026-06-03',
            'title': '분열을 멈추라',
            'simulatorStatus': 'MISSING',
            'hasExplanation': false,
            'draftNoteId': null,
            'cacheStatus': 'HIT',
            'range': {
              'testament': 'NEW',
              'bookCode': '1CO',
              'koreanBookName': '고린도전서',
              'englishBookName': '1 Corinthians',
              'chapter': 1,
              'verseFrom': 10,
              'verseTo': 17,
              'displayText': '고린도전서 1:10-17',
            },
          },
        });
      });
      dioAdapter.onGet(
        '/bible/verses',
        (server) {
          server.reply(200, {
            'success': true,
            'data': {
              'book': {
                'code': '1CO',
                'koreanName': '고린도전서',
                'englishName': '1 Corinthians',
                'chapter': 1,
              },
              'verses': [
                {
                  'id': 1010,
                  'bookCode': '1CO',
                  'chapterNo': 1,
                  'verseNo': 10,
                  'koreanText': '더미 한글 본문 10',
                  'englishText': 'Dummy English verse 10',
                },
              ],
            },
          });
        },
        queryParameters: {
          'bookCode': '1CO',
          'chapter': 1,
          'verseFrom': 10,
          'verseTo': 17,
        },
      );

      final result = await repository.getTodayQtPassage();

      expect(result.qtPassageId, 7);
      expect(result.title, '분열을 멈추라');
      expect(result.reference.displayText, '고린도전서 1:10-17');
      expect(result.book.code, '1CO');
      expect(result.verses.single.verseNo, 10);
      // §6 버튼 게이팅 입력 — 서버 simulatorStatus/hasExplanation/draftNoteId 파싱 회귀 방지
      expect(result.simulatorStatus, 'MISSING');
      expect(result.hasExplanation, isFalse);
      expect(result.draftNoteId, isNull);
    });

    test('장 교차 Today QT(10:14-11:1)는 장별로 조회해 경계 절만 이어 붙인다', () async {
      dioAdapter.onGet('/qt/today', (server) {
        server.reply(200, {
          'success': true,
          'data': {
            'qtPassageId': 9,
            'passageDate': '2026-06-16',
            'title': '스스로 종이 된 자유인',
            'simulatorStatus': 'MISSING',
            'hasExplanation': false,
            'draftNoteId': null,
            'cacheStatus': 'HIT',
            'range': {
              'testament': 'NEW',
              'bookCode': '1CO',
              'koreanBookName': '고린도전서',
              'englishBookName': '1 Corinthians',
              'chapter': 10,
              'endChapter': 11,
              'verseFrom': 14,
              'verseTo': 1,
              'displayText': '고린도전서 10:14-11:1',
            },
          },
        });
      });
      // 시작 장(10) 전체 — 경계 필터로 14,15만 남는다.
      dioAdapter.onGet(
        '/bible/verses',
        (server) => server.reply(200, {
          'success': true,
          'data': {
            'book': {
              'code': '1CO',
              'koreanName': '고린도전서',
              'englishName': '1 Corinthians',
              'chapter': 10,
            },
            'verses': [
              _verseJson(id: 1013, chapter: 10, verseNo: 13),
              _verseJson(id: 1014, chapter: 10, verseNo: 14),
              _verseJson(id: 1015, chapter: 10, verseNo: 15),
            ],
          },
        }),
        queryParameters: {'bookCode': '1CO', 'chapter': 10},
      );
      // 종료 장(11) 전체 — 경계 필터로 1만 남는다.
      dioAdapter.onGet(
        '/bible/verses',
        (server) => server.reply(200, {
          'success': true,
          'data': {
            'book': {
              'code': '1CO',
              'koreanName': '고린도전서',
              'englishName': '1 Corinthians',
              'chapter': 11,
            },
            'verses': [
              _verseJson(id: 1101, chapter: 11, verseNo: 1),
              _verseJson(id: 1102, chapter: 11, verseNo: 2),
            ],
          },
        }),
        queryParameters: {'bookCode': '1CO', 'chapter': 11},
      );

      final result = await repository.getTodayQtPassage();

      expect(result.qtPassageId, 9);
      expect(result.reference.displayText, '고린도전서 10:14-11:1');
      expect(result.verses.map((v) => v.verseNo).toList(), [14, 15, 1]);
    });

    test('READY 상태와 해설 존재 여부를 그대로 파싱하고, 미지의 상태값은 MISSING으로 방어한다', () {
      final ready = TodayQtSummary.fromJson(const {
        'qtPassageId': 7,
        'passageDate': '2026-06-03',
        'title': 't',
        'simulatorStatus': 'READY',
        'hasExplanation': true,
        'draftNoteId': 42,
        'cacheStatus': 'HIT',
        'range': null,
      });
      expect(ready.simulatorStatus, 'READY');
      expect(ready.hasExplanation, isTrue);
      expect(ready.draftNoteId, 42);

      final unknown = TodayQtSummary.fromJson(const {
        'qtPassageId': 7,
        'simulatorStatus': 'SOMETHING_NEW',
        'cacheStatus': 'HIT',
        'range': null,
      });
      expect(unknown.simulatorStatus, 'MISSING'); // 미지 값 → 버튼 비활성 방어
      expect(unknown.hasExplanation, isFalse);
    });

    test('서버에 오늘 QT 범위가 없으면 옛 fallback 본문을 표시하지 않는다', () async {
      dioAdapter.onGet('/qt/today', (server) {
        server.reply(200, {
          'success': true,
          'data': {
            'qtPassageId': null,
            'passageDate': null,
            'title': null,
            'simulatorStatus': 'DISABLED',
            'hasExplanation': false,
            'draftNoteId': null,
            'cacheStatus': 'MISS',
            'range': null,
          },
        });
      });

      await expectLater(
        repository.getTodayQtPassage(),
        throwsA(isA<StateError>()),
      );
    });
  });

  group('getPassageFromReferenceText', () {
    test('성서유니온 표기를 DB bookCode로 변환해 절 범위를 조회한다', () async {
      dioAdapter.onGet('/bible/books', (server) {
        server.reply(200, {
          'success': true,
          'data': [
            {
              'id': 46,
              'testament': 'NEW',
              'code': '1CO',
              'koreanName': '고린도전서',
              'englishName': '1 Corinthians',
              'displayOrder': 46,
            },
          ],
        });
      });
      dioAdapter.onGet(
        '/bible/verses',
        (server) {
          server.reply(200, {
            'success': true,
            'data': {
              'book': {
                'code': '1CO',
                'koreanName': '고린도전서',
                'englishName': '1 Corinthians',
                'chapter': 1,
              },
              'verses': [
                {
                  'id': 1010,
                  'bookCode': '1CO',
                  'chapterNo': 1,
                  'verseNo': 10,
                  'koreanText': '더미 한글 본문 10',
                  'englishText': 'Dummy English verse 10',
                },
                {
                  'id': 1017,
                  'bookCode': '1CO',
                  'chapterNo': 1,
                  'verseNo': 17,
                  'koreanText': '더미 한글 본문 17',
                  'englishText': 'Dummy English verse 17',
                },
              ],
            },
          });
        },
        queryParameters: {
          'bookCode': '1CO',
          'chapter': 1,
          'verseFrom': 10,
          'verseTo': 17,
        },
      );

      final result = await repository.getPassageFromReferenceText(
        '고린도전서(1 Corinthians)1:10 - 1:17',
      );

      print(result.book.code);
      print(result.reference.displayText);
      print(result.verses
          .map((v) => {
                'id': v.id,
                'bookCode': v.bookCode,
                'chapterNo': v.chapterNo,
                'verseNo': v.verseNo,
                'koreanText': v.koreanText,
                'englishText': v.englishText,
              })
          .toList());

      expect(result.reference.displayText, '고린도전서 1:10-17');
      expect(result.book.code, '1CO');
      expect(result.verses.map((verse) => verse.verseNo), [10, 17]);
    });
  });

  // (장 교차 테스트용 절 JSON 헬퍼는 파일 하단 _verseJson 참조)

  group('getQtStudyContent', () {
    test('QT 해설 응답을 summary, 절별 해설, 단어 풀이로 파싱한다', () async {
      dioAdapter.onGet('/qt/7/study-content', (server) {
        server.reply(200, {
          'success': true,
          'data': {
            'summary': '본문 전체 요약',
            'explanations': [
              {
                'verseId': 2001,
                'summary': '절 요약',
                'explanation': '절 해설',
                'sourceLabel': 'QT-AI verified content',
                'aiAssetId': 91,
              },
            ],
            'glossaryTerms': [
              {
                'id': 31,
                'verseId': 2001,
                'term': '지혜',
                'meaning': '본문 단어 풀이',
                'sourceLabel': 'QT-AI glossary',
              },
            ],
          },
        });
      });

      final result = await repository.getQtStudyContent(7);

      expect(result.summary, '본문 전체 요약');
      expect(result.explanations.single.explanation, '절 해설');
      expect(result.glossaryTerms.single.term, '지혜');
      expect(result.hasVisibleContent, isTrue);
    });
  });
}

Map<String, dynamic> _verseJson({
  required int id,
  required int chapter,
  required int verseNo,
}) =>
    {
      'id': id,
      'bookCode': '1CO',
      'chapterNo': chapter,
      'verseNo': verseNo,
      'koreanText': '더미 한글 본문 $verseNo',
      'englishText': 'Dummy English verse $verseNo',
    };
