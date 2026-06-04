import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/note_models.dart';
import '../services/note_repository.dart';

/// 노트 Repository 주입.
final noteRepositoryProvider = Provider<NoteRepository>((ref) {
  return NoteRepository(ref.watch(dioProvider));
});

/// 선택된 카테고리 필터 상태 (null = 전체).
final noteCategoryFilterProvider = StateProvider<String?>((ref) => null);

/// 노트 목록.
///
/// ✏️ 왜 이렇게 짰냐면:
/// 카테고리 필터(noteCategoryFilterProvider)를 watch 하므로
/// 사용자가 탭을 바꾸면 이 provider가 자동으로 다시 조회한다.
/// autoDispose = 화면을 떠나면 캐시를 비워 메모리/낡은 데이터를 정리한다.
final notesProvider = FutureProvider.autoDispose<NoteListResponse>((ref) {
  final repository = ref.watch(noteRepositoryProvider);
  final category = ref.watch(noteCategoryFilterProvider);
  return repository.getNotes(category: category);
});
