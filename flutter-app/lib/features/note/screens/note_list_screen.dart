import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../models/note_models.dart';
import '../providers/note_providers.dart';
import '../widgets/meditation_calendar.dart';

/// 노트 목록 화면 (N-01).
///
/// - 카테고리 탭(전체 + 묵상/설교/기도/감사/회개) 통합 목록
/// - GET /api/v1/notes 연동, 빈/에러는 공통 위젯으로 처리
/// - 우하단 FAB → N-02 카테고리 선택(기도/회개/감사) → N-03 작성
/// - (Day2) 목록 ↔ 달력 토글, 항목 탭 시 N-04 상세
class NoteListScreen extends ConsumerWidget {
  const NoteListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedCategory = ref.watch(noteCategoryFilterProvider);
    final showCalendar = ref.watch(noteCalendarViewProvider);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.navRecord),
        centerTitle: true,
        // ✏️ 목록↔달력 전환 토글. 상태는 noteCalendarViewProvider에 둬 화면 전체가 따라 바뀜.
        actions: [
          IconButton(
            tooltip: showCalendar ? l.noteViewList : l.noteViewCalendar,
            icon: Icon(showCalendar
                ? Icons.view_list_outlined
                : Icons.calendar_month_outlined),
            onPressed: () => ref.read(noteCalendarViewProvider.notifier).state =
                !showCalendar,
          ),
        ],
      ),
      // ✏️ 왜 이렇게 짰냐면:
      // 새 노트 작성은 Material 관례대로 우하단 FAB에 둔다.
      // 누르면 N-02(카테고리 선택)로 이동 → 거기서 N-03 작성으로 이어진다.
      floatingActionButton: FloatingActionButton(
        onPressed: () =>
            Navigator.of(context).pushNamed(AppRouter.noteCategorySelect),
        child: const Icon(Icons.add),
      ),
      // ✏️ 달력 모드(기본): 달력 + 그 아래 노트 목록. 목록 모드: 카테고리 칩 + 목록.
      body: showCalendar
          ? const Column(
              children: [
                MeditationCalendarView(),
                Divider(height: 1),
                Expanded(child: _NoteListBody()),
              ],
            )
          : Column(
              children: [
                // 카테고리 필터 칩 (전체 + 5종)
                SizedBox(
                  height: 48,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    padding:
                        const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
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
                const Expanded(child: _NoteListBody()),
              ],
            ),
    );
  }

}

/// 노트 목록 본문(칩 제외) — 달력 모드(달력 아래)·목록 모드가 공유한다.
class _NoteListBody extends ConsumerWidget {
  const _NoteListBody();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notesAsync = ref.watch(notesProvider);
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    // 로딩/에러/빈 화면은 공통 위젯 whenOrDefault가 처리.
    return notesAsync.whenOrDefault(
      data: (response) {
        if (response.items.isEmpty) {
          return EmptyView(message: l.noteEmpty);
        }
        return RefreshIndicator(
          onRefresh: () async => ref.invalidate(notesProvider),
          child: ListView.separated(
            itemCount: response.items.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final item = response.items[index];
              return ListTile(
                title: Text(
                  item.title.isEmpty ? l.noteUntitled : item.title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                subtitle: Row(
                  children: [
                    Text(
                      noteCategoryLabel(item.category),
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: theme.colorScheme.primary),
                    ),
                    if (item.status == 'DRAFT') ...[
                      const SizedBox(width: 8),
                      Text(l.noteDraft,
                          style: theme.textTheme.bodySmall
                              ?.copyWith(color: Colors.orange)),
                    ],
                    const Spacer(),
                    if (item.updatedAt != null)
                      Text(
                        _formatDate(item.updatedAt!),
                        style: theme.textTheme.bodySmall
                            ?.copyWith(color: Colors.grey),
                      ),
                  ],
                ),
                // 항목 탭 → N-04 상세. 수정·삭제 시 그쪽이 invalidate → 돌아오면 최신 반영.
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

  // DateTime을 'yyyy.MM.dd'로만 표시(V1, intl 불필요).
  String _formatDate(DateTime d) =>
      '${d.year}.${d.month.toString().padLeft(2, '0')}.${d.day.toString().padLeft(2, '0')}';
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
