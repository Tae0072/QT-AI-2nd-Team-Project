import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../models/note_models.dart';
import '../providers/note_providers.dart';
import '../widgets/meditation_calendar.dart';
import '../widgets/note_card.dart';
import 'note_edit_screen.dart' show NoteEditArgs;

/// 노트 목록 화면 (N-01).
///
/// 단일 통합 화면(QA ⑤⑧): 상단 달력(항상) + 카테고리 칩 + 상태 필터 + 목록.
/// 목록↔달력 토글은 제거했다(2026-06-12).
/// - 카테고리 칩(전체 + QT/설교/기도/회개/감사) = ⑧ 종류별 분리
/// - 상태 필터(전체/임시저장/저장) = ⑧ 임시저장 따로 보기
/// - 기도/회개/감사 칩 선택 시 "+ {카테고리} 작성"으로 N-02 생략하고 N-03 직행 = ⑤ 빠른 작성
/// - 우하단 FAB → N-02 카테고리 선택 → N-03 작성(전체/QT/설교 맥락)
/// - GET /api/v1/notes 연동, 빈/에러는 공통 위젯으로 처리, 항목 탭 시 N-04 상세
class NoteListScreen extends ConsumerStatefulWidget {
  const NoteListScreen({super.key});

  @override
  ConsumerState<NoteListScreen> createState() => _NoteListScreenState();
}

class _NoteListScreenState extends ConsumerState<NoteListScreen> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // 화면을 다시 그릴 때 기존 검색어와 입력칸을 맞춘다(provider가 단일 진실).
    final q = ref.read(noteSearchQueryProvider);
    if (q != null) _searchController.text = q;
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _submitSearch(String value) {
    final q = value.trim();
    ref.read(noteSearchQueryProvider.notifier).state = q.isEmpty ? null : q;
  }

  void _clearSearch() {
    _searchController.clear();
    ref.read(noteSearchQueryProvider.notifier).state = null;
  }

  /// 선택 모드 종료 + 선택 비우기.
  void _exitSelection(WidgetRef ref) {
    ref.read(noteSelectionModeProvider.notifier).state = false;
    ref.read(noteSelectedIdsProvider.notifier).state = <int>{};
  }

  /// 전체선택 토글: 현재 목록 전부 선택됐으면 해제, 아니면 전부 선택.
  void _selectAll(WidgetRef ref) {
    final items = ref.read(notesProvider).valueOrNull?.items ?? const [];
    final allIds = items.map((e) => e.id).toSet();
    final current = ref.read(noteSelectedIdsProvider);
    ref.read(noteSelectedIdsProvider.notifier).state =
        (allIds.isNotEmpty && current.length == allIds.length)
            ? <int>{}
            : allIds;
  }

  /// 선택한 노트들을 확인 후 삭제. 부분 실패는 안내한다.
  Future<void> _deleteSelected(BuildContext context, WidgetRef ref) async {
    final l = AppLocalizations.of(context);
    // 안전장치: 현재 목록(필터 적용된 화면)에 보이는 노트만 삭제 대상으로 삼는다.
    // 필터가 바뀌어 선택 집합에 안 보이는 노트가 남아 있어도 의도치 않게 삭제되지 않게 한다.
    final visibleIds = ref.read(notesProvider).valueOrNull?.items
            .map((e) => e.id)
            .toSet() ??
        <int>{};
    final ids =
        ref.read(noteSelectedIdsProvider).where(visibleIds.contains).toList();
    if (ids.isEmpty) return;

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l.noteDeleteSelectedTitle),
        content: Text(l.noteDeleteSelectedBody(ids.length)),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: Text(l.commonCancel)),
          FilledButton(
              onPressed: () => Navigator.of(ctx).pop(true),
              child: Text(l.commonDelete)),
        ],
      ),
    );
    if (ok != true) return;

    final failed = await ref.read(noteRepositoryProvider).deleteMany(ids);
    ref.invalidate(notesProvider);
    ref.invalidate(meditationCalendarProvider);
    _exitSelection(ref);
    if (!context.mounted) return;
    final okCount = ids.length - failed.length;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      // 부분 성공도 정확히: 성공 n개 + 실패 m개를 함께 안내.
      content: Text(failed.isEmpty
          ? l.noteDeletedCount(okCount)
          : l.noteDeletePartial(okCount, failed.length)),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final selectedCategory = ref.watch(noteCategoryFilterProvider);
    final selectedStatus = ref.watch(noteStatusFilterProvider);
    final hasQuery = ref.watch(noteSearchQueryProvider) != null;
    final selectionMode = ref.watch(noteSelectionModeProvider);
    final selectedIds = ref.watch(noteSelectedIdsProvider);
    final l = AppLocalizations.of(context);

    // 필터(카테고리·상태)가 바뀌면 보이지 않는 노트가 선택에 남는 누수를 막기 위해
    // 선택을 초기화한다(선택은 항상 현재 화면 기준).
    ref.listen(noteCategoryFilterProvider, (prev, next) {
      if (prev != next) _exitSelection(ref);
    });
    ref.listen(noteStatusFilterProvider, (prev, next) {
      if (prev != next) _exitSelection(ref);
    });

    // 기도/회개/감사 칩이 선택됐을 때만 작성 단축이 가능하다(칩 컨텍스트 작성).
    // QT·설교는 각자 QT/성경 화면에서 작성하므로 여기서 작성 진입을 두지 않는다.
    final quickCreateCategory =
        writableNoteCategories.contains(selectedCategory)
            ? selectedCategory
            : null;
    // ② 기록에서 작성하지 않는 맥락(QT·설교)·선택 모드에서는 작성 FAB을 숨긴다.
    final showFab = !selectionMode &&
        !tabAuthoredCategories.contains(selectedCategory);

    return Scaffold(
      // 선택 모드면 AppBar가 ✕ + "n개 선택" + 전체선택으로 바뀐다.
      appBar: selectionMode
          ? AppBar(
              leading: IconButton(
                icon: const Icon(Icons.close),
                tooltip: l.commonCancel,
                onPressed: () => _exitSelection(ref),
              ),
              title: Text(l.noteSelectedCount(selectedIds.length)),
              centerTitle: true,
              actions: [
                TextButton(
                  onPressed: () => _selectAll(ref),
                  child: Text(l.noteSelectAll),
                ),
              ],
            )
          : AppBar(
              title: Text(l.navRecord),
              centerTitle: true,
              actions: [
                // ☰ 선택 — 누르면 목록 다중 선택 모드로 진입.
                IconButton(
                  icon: const Icon(Icons.menu),
                  tooltip: l.noteSelect,
                  onPressed: () =>
                      ref.read(noteSelectionModeProvider.notifier).state = true,
                ),
              ],
            ),
      // ✏️ 작성 진입은 Material 관례대로 우하단 FAB 하나로 모은다(항상 노출 → 발견성).
      // 기도/회개/감사 칩 선택 시에는 FAB가 "+ {카테고리} 작성" 확장 버튼으로 바뀌어
      // N-02(카테고리 선택)를 건너뛰고 N-03로 직행한다. 그 외에는 N-02로 간다.
      // FAB 색은 페일 블러시(#EADAD2) 채움 + 다크 브라운 글자(라이트/다크 공통 고정색).
      // 크기·여백도 타이트하게. 전역 FAB 토큰은 그대로 두고 이 화면만 오버라이드.
      floatingActionButton: !showFab
          ? null
          : quickCreateCategory != null
              // 확장 FAB은 Material 규격상 높이 48dp가 고정이라 위아래 폭이 안 줄어든다.
              // 커스텀 알약(Material+InkWell)으로 만들어 vertical 패딩으로 높이를 직접 잡는다.
              ? Material(
                  color: _kFabBg,
                  shape: const StadiumBorder(),
                  clipBehavior: Clip.antiAlias,
                  child: InkWell(
                    onTap: () => Navigator.of(context).pushNamed(
                      AppRouter.noteEdit,
                      arguments: NoteEditArgs(category: quickCreateCategory),
                    ),
                    child: Padding(
                      // ▼ 위아래 폭은 vertical 값으로 조절(작게=6, 크게=10).
                      padding: const EdgeInsets.symmetric(
                          horizontal: 14, vertical: 7),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.add, size: 16, color: _kFabFg),
                          const SizedBox(width: 4),
                          Text(
                            l.noteQuickCreate(
                                noteCategoryLabel(quickCreateCategory)),
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: _kFabFg,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                )
              : FloatingActionButton.small(
                  onPressed: () => Navigator.of(context)
                      .pushNamed(AppRouter.noteCategorySelect),
                  backgroundColor: _kFabBg,
                  foregroundColor: _kFabFg,
                  elevation: 0,
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  child: const Icon(Icons.add, size: 22),
                ),
      // 선택 모드 하단 삭제 바.
      bottomNavigationBar: selectionMode
          ? _SelectionDeleteBar(
              count: selectedIds.length,
              onDelete: () => _deleteSelected(context, ref),
            )
          : null,
      body: Column(
        children: [
          // 상단 달력(항상 표시). 월/2주 토글·색점은 meditation_calendar.dart(④⑥⑦)가 담당.
          const MeditationCalendarView(),
          const Divider(height: 1),
          // 상태 필터(전체/임시저장/저장) — 카테고리 칩 위, 우측 한 줄.
          Padding(
            padding: const EdgeInsets.only(right: 8, top: 4, bottom: 8),
            child: Align(
              alignment: Alignment.centerRight,
              child: _StatusFilterDropdown(selected: selectedStatus),
            ),
          ),
          // 카테고리 칩 (전체 + 5종) — 풀폭 가로 스크롤.
          SizedBox(
            height: 48,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              children: [
                _CategoryChip(
                    label: l.noteFilterAll,
                    value: null,
                    selected: selectedCategory),
                for (final entry in noteCategoryLabels.entries)
                  _CategoryChip(
                    label: entry.value,
                    value: entry.key,
                    selected: selectedCategory,
                  ),
              ],
            ),
          ),
          const Divider(height: 1),
          // 검색바 — 제목·본문을 서버에서 검색(GET /notes?q=). 엔터로 제출.
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 6),
            child: TextField(
              controller: _searchController,
              textInputAction: TextInputAction.search,
              decoration: InputDecoration(
                hintText: l.noteSearchHint,
                prefixIcon: const Icon(Icons.search),
                isDense: true,
                suffixIcon: hasQuery
                    ? IconButton(
                        icon: const Icon(Icons.close),
                        tooltip: l.commonClose,
                        onPressed: _clearSearch,
                      )
                    : null,
              ),
              onSubmitted: _submitSearch,
            ),
          ),
          const Expanded(child: _NoteListBody()),
        ],
      ),
    );
  }
}

/// 노트탭 FAB 색 — 페일 블러시 채움 + 다크 브라운 글자(라이트/다크 공통 고정).
/// 톤 바꾸려면 이 두 값만 교체하면 된다.
const Color _kFabBg = Color(0xFFEADAD2); // 페일 블러시
const Color _kFabFg = Color(0xFF4A3B2E); // 다크 브라운(블러시 위 가독)

/// 노트 목록 본문 — 카테고리·상태 필터가 적용된 notesProvider를 구독해 그린다.
class _NoteListBody extends ConsumerWidget {
  const _NoteListBody();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notesAsync = ref.watch(notesProvider);
    final hasQuery = ref.watch(noteSearchQueryProvider) != null;
    final selectionMode = ref.watch(noteSelectionModeProvider);
    final selectedIds = ref.watch(noteSelectedIdsProvider);
    final category = ref.watch(noteCategoryFilterProvider);
    final l = AppLocalizations.of(context);

    // 로딩/에러/빈 화면은 공통 위젯 whenOrDefault가 처리.
    return notesAsync.whenOrDefault(
      data: (response) {
        if (response.items.isEmpty) {
          // 검색 중이면 "검색 결과 없음" 우선. 아니면 QT·설교는 작성 위치 안내, 그 외 기본 빈 문구.
          final message = hasQuery
              ? l.noteSearchEmpty
              : category == kNoteCatMeditation
                  ? l.noteEmptyQtHint
                  : category == kNoteCatSermon
                      ? l.noteEmptySermonHint
                      : l.noteEmpty;
          return EmptyView(message: message);
        }
        return RefreshIndicator(
          onRefresh: () async => ref.invalidate(notesProvider),
          child: ListView.separated(
            itemCount: response.items.length,
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            separatorBuilder: (_, __) => const SizedBox(height: 10),
            itemBuilder: (context, index) {
              final item = response.items[index];
              return NoteCard(
                item: item,
                selectionMode: selectionMode,
                selected: selectedIds.contains(item.id),
                // 선택 모드: 탭 = 선택 토글. 일반: 탭 = N-04 상세.
                onToggleSelect: () {
                  ref.read(noteSelectedIdsProvider.notifier).update((s) {
                    final next = {...s};
                    next.contains(item.id)
                        ? next.remove(item.id)
                        : next.add(item.id);
                    return next;
                  });
                },
                onTap: () => Navigator.of(context).pushNamed(
                  AppRouter.noteDetail,
                  arguments: item.id,
                ),
              );
            },
          ),
        );
      },
    );
  }
}

/// 선택 모드 하단 삭제 바. 선택 0개면 비활성.
class _SelectionDeleteBar extends StatelessWidget {
  final int count;
  final VoidCallback onDelete;

  const _SelectionDeleteBar({required this.count, required this.onDelete});

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
        child: SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: count == 0 ? null : onDelete,
            icon: const Icon(Icons.delete_outline),
            label: Text('${l.commonDelete} ($count)'),
          ),
        ),
      ),
    );
  }
}

/// 카테고리 선택 칩.
class _CategoryChip extends ConsumerWidget {
  final String label;
  final String? value;
  final String? selected;

  const _CategoryChip({
    required this.label,
    required this.value,
    required this.selected,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: FilterChip(
        label: Text(label),
        selected: selected == value,
        onSelected: (_) {
          // ✏️ 같은 칩을 다시 눌러도 그 값으로 유지(토글 아님). 필터 변경 = provider 자동 재조회.
          ref.read(noteCategoryFilterProvider.notifier).state = value;
        },
      ),
    );
  }
}

/// 상태 필터 드롭다운 (전체/임시저장/저장). value=null이 "전체".
///
/// 카테고리 칩 줄 우측에 둔다. 선택 변경 시 notesProvider가 자동 재조회한다.
class _StatusFilterDropdown extends ConsumerWidget {
  final String? selected;

  const _StatusFilterDropdown({required this.selected});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l = AppLocalizations.of(context);
    // null=전체. DropdownButton은 null 값 항목도 허용한다.
    final options = <String?, String>{
      null: l.noteStatusAll,
      'DRAFT': l.noteStatusDraft,
      'SAVED': l.noteStatusSaved,
    };
    return DropdownButtonHideUnderline(
      child: DropdownButton<String?>(
        value: selected,
        icon: const Icon(Icons.arrow_drop_down),
        borderRadius: BorderRadius.circular(8),
        isDense: true,
        items: [
          for (final entry in options.entries)
            DropdownMenuItem<String?>(
              value: entry.key,
              child: Text(entry.value),
            ),
        ],
        onChanged: (value) =>
            ref.read(noteStatusFilterProvider.notifier).state = value,
      ),
    );
  }
}
