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

  /// QT 학습 콘텐츠(해설) 조회 — TTS 해설 읽기에 사용.
  Future<QtStudyContent> getQtStudyContent(int qtPassageId) async {
    final response = await _dio.get('/qt/$qtPassageId/study-content');
    final data = response.data['data'] as Map<String, dynamic>;
    return QtStudyContent.fromJson(data);
  }

  Future<QtVideoClip> getQtVideo(int qtPassageId) async {
    final response = await _dio.get('/qt/$qtPassageId/video');
    final data = response.data['data'] as Map<String, dynamic>;
    return QtVideoClip.fromJson(data);
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

  /// 선택 본문 범위의 해설 진입점 가용성(qtPassageId·hasExplanation) 조회.
  ///
  /// QT 도메인이 bible·study를 조합해 노출하는 엔드포인트라 `/qt/...` 아래에 있다.
  /// (`/bible/verses`는 모듈 순환 방지로 qt·study에 의존할 수 없다.)
  Future<BiblePassageStudy> getBiblePassageStudy({
    required String bookCode,
    required int chapter,
    required int verseFrom,
    required int verseTo,
  }) async {
    final response = await _dio.get('/qt/passage-study', queryParameters: {
      'bookCode': bookCode,
      'chapter': chapter,
      'verseFrom': verseFrom,
      'verseTo': verseTo,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return BiblePassageStudy.fromJson(data);
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

  Future<TodayQtPassage> getTodayQtPassage() async {
    final todayQt = await getTodayQt();
    final todayRange = todayQt.range;

    // range가 없을 때 하드코딩된 본문을 '오늘 QT'처럼 보여주지 않는다.
    if (todayRange == null) {
      throw StateError('오늘 QT 본문 범위가 아직 준비되지 않았습니다.');
    }

    // 장 교차 본문(예: 고린도전서 10:14-11:1)은 단일 장 조회로는 verseFrom>verseTo가 되어 400(C0002)이 난다.
    // 백엔드 collectRangeVerses와 동일하게 장별로 모아 경계를 필터링한다.
    final verseRange = todayRange.isCrossChapter
        ? await _getCrossChapterVerses(todayRange)
        : await getVerses(
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
      simulatorStatus: todayQt.simulatorStatus,
      hasExplanation: todayQt.hasExplanation,
      draftNoteId: todayQt.draftNoteId,
      reference: todayRange.toReference(),
      book: verseRange.book,
      verses: verseRange.verses,
    );
  }

  /// 장 교차 본문(예: 고린도전서 10:14-11:1)의 절을 장별로 조회해 경계를 필터링하고 이어 붙인다.
  ///
  /// `/bible/verses`는 단일 장 전용이라(`chapter`+`verseFrom..verseTo`), 장 교차는
  /// 시작 장은 [TodayQtRange.verseFrom]부터, 종료 장은 [TodayQtRange.verseTo]까지,
  /// 중간 장은 전체를 모은다. 권 메타는 시작 장 응답의 것을 사용한다.
  Future<BibleVerseRange> _getCrossChapterVerses(TodayQtRange range) async {
    final List<BibleVerse> collected = [];
    BibleVerseBook? book;
    for (int ch = range.chapter; ch <= range.endChapter; ch++) {
      final chapterRange =
          await getChapterVerses(bookCode: range.bookCode, chapter: ch);
      book ??= chapterRange.book;
      for (final verse in chapterRange.verses) {
        if (ch == range.chapter && verse.verseNo < range.verseFrom) continue;
        if (ch == range.endChapter && verse.verseNo > range.verseTo) continue;
        collected.add(verse);
      }
    }
    return BibleVerseRange(book: book!, verses: collected);
  }
}
