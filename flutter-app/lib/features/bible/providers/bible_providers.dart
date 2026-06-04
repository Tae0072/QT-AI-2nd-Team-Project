import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/bible_models.dart';
import '../services/bible_repository.dart';

const sumTodayReferenceText = '고린도전서(1 Corinthians)1:10 - 1:17';

final bibleRepositoryProvider = Provider<BibleRepository>((ref) {
  return BibleRepository(ref.watch(dioProvider));
});

final todayQtPassageProvider =
    FutureProvider.autoDispose<TodayQtPassage>((ref) {
  final repository = ref.watch(bibleRepositoryProvider);
  return repository.getTodayQtPassage(
    fallbackReferenceText: sumTodayReferenceText,
  );
});
