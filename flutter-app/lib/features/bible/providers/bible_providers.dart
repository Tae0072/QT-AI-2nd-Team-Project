import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/bible_models.dart';
import '../services/bible_repository.dart';
import 'dev_demo_qt_video.dart'; // ⚠️ 시연용 데모 영상(디버그 한정), 운영 전 제거

final bibleRepositoryProvider = Provider<BibleRepository>((ref) {
  return BibleRepository(ref.watch(dioProvider));
});

final todayQtPassageProvider =
    FutureProvider.autoDispose<TodayQtPassage>((ref) async {
  final repository = ref.watch(bibleRepositoryProvider);
  // 하드코딩 fallback 본문 제거(P0-9) — range가 없으면 repository가 StateError를 던지고
  // UI는 '준비 중/오류'로 처리한다. 다른 본문을 오늘 QT로 보여주지 않는다.
  final passage = await repository.getTodayQtPassage();
  // demo(remove before production): force simulator READY so the video button is enabled.
  return withDemoSimulatorReady(passage);
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
    FutureProvider.autoDispose.family<QtVideoClip, int>((ref, qtPassageId) async {
  final repository = ref.watch(bibleRepositoryProvider);
  final clip = await repository.getQtVideo(qtPassageId);
  // ⚠️ 시연용: 디버그 + 클립이 준비 안 됐을 때만 샘플 영상으로 대체(운영 전 제거).
  return withDemoQtVideo(clip, qtPassageId);
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
