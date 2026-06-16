import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../note/models/note_models.dart';
import '../../note/providers/note_providers.dart';

/// 하단 탭 인덱스(홈/성경/나눔/기록/마이). 화면 어디서든 탭 전환을 위해 provider로 둔다.
final homeTabIndexProvider = StateProvider<int>((ref) => 0);

/// 홈의 "최근 묵상 기록" — 기록(노트) 최근 항목 일부를 필터 없이 조회한다.
/// 기록 탭과 같은 데이터(noteRepository)를 쓰므로 연동된다.
final homeRecentNotesProvider =
    FutureProvider.autoDispose<List<NoteListItem>>((ref) async {
  final repository = ref.watch(noteRepositoryProvider);
  final response = await repository.getNotes();
  return response.items.take(3).toList();
});
