import 'package:dio/dio.dart';

import '../models/bible_models.dart';
import '../models/bible_reference.dart';

/// 성경/QT 화면에서 사용하는 서버 API 호출 레이어.
class BibleRepository {
  final Dio _dio;

  BibleRepository(this._dio);

  Future<List<BibleBook>> getBooks() async {
    final response = await _dio.get('/bible/books');
    final data = response.data['data'] as List<dynamic>;
    return data
        .map((item) => BibleBook.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<TodayQtSummary> getTodayQt() async {
    final response = await _dio.get('/qt/today');
    final data = response.data['data'] as Map<String, dynamic>;
    return TodayQtSummary.fromJson(data);
  }

  Future<BibleVerseRange> getVerses({
    required String bookCode,
    required int chapter,
    required int verseFrom,
    required int verseTo,
  }) async {
    final response = await _dio.get('/bible/verses', queryParameters: {
      'bookCode': bookCode,
      'chapter': chapter,
      'verseFrom': verseFrom,
      'verseTo': verseTo,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return BibleVerseRange.fromJson(data);
  }

  Future<BibleVerseRange> getChapterVerses({
    required String bookCode,
    required int chapter,
  }) async {
    final response = await _dio.get('/bible/verses', queryParameters: {
      'bookCode': bookCode,
      'chapter': chapter,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return BibleVerseRange.fromJson(data);
  }

  Future<QtStudyContent> getQtStudyContent(int qtPassageId) async {
    final response = await _dio.get('/qt/$qtPassageId/study-content');
    final data = response.data['data'] as Map<String, dynamic>;
    return QtStudyContent.fromJson(data);
  }

  Future<TodayQtPassage> getPassageFromReferenceText(
    String referenceText,
  ) async {
    final reference = BibleReferenceParser.parse(referenceText);
    final books = await getBooks();
    final book = books.firstWhere(
      (item) => item.matchesReference(reference),
      orElse: () => throw StateError('성경 권을 찾을 수 없습니다: $referenceText'),
    );
    final range = await getVerses(
      bookCode: book.code,
      chapter: reference.chapter,
      verseFrom: reference.verseFrom,
      verseTo: reference.verseTo,
    );

    return TodayQtPassage(
      reference: reference,
      book: range.book,
      verses: range.verses,
    );
  }

  Future<TodayQtPassage> getTodayQtPassage({
    String fallbackReferenceText = '고린도전서(1 Corinthians)1:10 - 1:17',
  }) async {
    final todayQt = await getTodayQt();
    final todayRange = todayQt.range;

    if (todayRange == null) {
      if (todayQt.qtPassageId == null) {
        throw StateError('오늘 QT 본문 범위가 아직 준비되지 않았습니다.');
      }
      final fallback = await getPassageFromReferenceText(fallbackReferenceText);
      return TodayQtPassage(
        qtPassageId: todayQt.qtPassageId,
        passageDate: todayQt.passageDate,
        title: todayQt.title,
        cacheStatus: todayQt.cacheStatus,
        hasExplanation: todayQt.hasExplanation,
        reference: fallback.reference,
        book: fallback.book,
        verses: fallback.verses,
      );
    }

    final verseRange = await getVerses(
      bookCode: todayRange.bookCode,
      chapter: todayRange.chapter,
      verseFrom: todayRange.verseFrom,
      verseTo: todayRange.verseTo,
    );

    return TodayQtPassage(
      qtPassageId: todayQt.qtPassageId,
      passageDate: todayQt.passageDate,
      title: todayQt.title,
      cacheStatus: todayQt.cacheStatus,
      hasExplanation: todayQt.hasExplanation,
      reference: todayRange.toReference(),
      book: verseRange.book,
      verses: verseRange.verses,
    );
  }
}
