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

/// 선택된 상태 필터 (null = 전체, 'DRAFT' = 임시저장, 'SAVED' = 저장).
/// 서버 notes.status 값(04 §8.2)을 그대로 쓴다. DELETED는 목록에서 빠지므로 대상 아님.
///
/// DRAFT를 목록에 노출하는 것은 "자동저장 금지" 정책과 무관하다 — DRAFT는 사용자가
/// 명시적으로 [임시저장] 버튼을 눌러 만든 상태이지 자동 생성물이 아니다(07 §6.4 저장 정책).
final noteStatusFilterProvider = StateProvider<String?>((ref) => null);

/// 검색어 (null = 검색 안 함). 검색바 제출 시 설정한다.
/// 서버 `GET /notes?q=`(제목·본문 LIKE)로 전달된다.
final noteSearchQueryProvider = StateProvider<String?>((ref) => null);

/// 노트 목록.
///
/// 왜 이렇게 짰냐면:
/// 카테고리·상태·검색어를 watch 하므로 사용자가 칩/상태/검색을 바꾸면
/// 이 provider가 자동으로 다시 조회한다.
/// autoDispose = 화면을 떠나면 캐시를 비워 메모리/낡은 데이터를 정리한다.
final notesProvider = FutureProvider.autoDispose<NoteListResponse>((ref) {
  final repository = ref.watch(noteRepositoryProvider);
  final category = ref.watch(noteCategoryFilterProvider);
  final status = ref.watch(noteStatusFilterProvider);
  final q = ref.watch(noteSearchQueryProvider);
  return repository.getNotes(category: category, status: status, q: q);
});

/// 노트 상세 (id별).
///
/// 왜 이렇게 짰냐면:
/// 상세는 id마다 결과가 달라서 family로 noteId를 인자로 받는다.
/// 화면은 noteDetailProvider(42)처럼 호출 → 42 전용 결과를 따로 캐시한다.
/// autoDispose = 상세화면을 떠나면 캐시를 비워, 다시 들어올 때 최신으로 재조회('i 방식').
final noteDetailProvider =
    FutureProvider.autoDispose.family<NoteDetail, int>((ref, noteId) {
  return ref.watch(noteRepositoryProvider).getDetail(noteId);
});

/// 묵상 달력 (월별).
///
/// 달은 "yyyy-MM"마다 결과가 달라서 family로 month를 인자로 받는다.
/// 사용자가 이전/다음 달로 넘기면 그 달 문자열로 provider를 다시 watch한다.
final meditationCalendarProvider =
    FutureProvider.autoDispose.family<MeditationCalendar, String>((ref, month) {
  return ref.watch(noteRepositoryProvider).getMeditationCalendar(month);
});
