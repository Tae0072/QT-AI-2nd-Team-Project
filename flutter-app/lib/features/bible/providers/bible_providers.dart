import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/bible_models.dart';
import '../services/bible_repository.dart';

final bibleRepositoryProvider = Provider<BibleRepository>((ref) {
  return BibleRepository(ref.watch(dioProvider));
});

final todayQtPassageProvider =
    FutureProvider.autoDispose<TodayQtPassage>((ref) {
  final repository = ref.watch(bibleRepositoryProvider);
  // 하드코딩 fallback 본문 제거(P0-9) — range가 없으면 repository가 StateError를 던지고
  // UI는 '준비 중/오류'로 처리한다. 다른 본문을 오늘 QT로 보여주지 않는다.
  return repository.getTodayQtPassage();
});

/// 성경 권 목록 (B-02 성경 브라우저 드롭다운).
///
/// ② Riverpod 일관화: 화면 FutureBuilder/setState 대신 Provider로 조회한다.
final bibleBooksProvider = FutureProvider.autoDispose<List<BibleBook>>((ref) {
  return ref.watch(bibleRepositoryProvider).getBooks();
});

final qtStudyContentProvider =
    FutureProvider.autoDispose.family<QtStudyContent, int>((ref, qtPassageId) {
  final repository = ref.watch(bibleRepositoryProvider);
  return repository.getQtStudyContent(qtPassageId);
});

final qtVideoClipProvider =
    FutureProvider.autoDispose.family<QtVideoClip, int>((ref, qtPassageId) {
  final repository = ref.watch(bibleRepositoryProvider);
  return repository.getQtVideo(qtPassageId);
});

/// 성경 본문 범위의 해설 진입점 가용성 (전체 페이지의 '해설' 버튼 활성 기준).
typedef BiblePassageRef = ({
  String bookCode,
  int chapter,
  int verseFrom,
  int verseTo,
});

final biblePassageStudyProvider = FutureProvider.autoDispose
    .family<BiblePassageStudy, BiblePassageRef>((ref, range) {
  return ref.read(bibleRepositoryProvider).getBiblePassageStudy(
        bookCode: range.bookCode,
        chapter: range.chapter,
        verseFrom: range.verseFrom,
        verseTo: range.verseTo,
      );
});
