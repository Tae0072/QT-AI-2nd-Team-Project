import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../models/note_models.dart';
import '../providers/note_providers.dart';
import '../widgets/meditation_calendar.dart';
import '../widgets/note_card.dart';

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
    final visibleIds =
        ref.read(notesProvider).valueOrNull?.items.map((e) => e.id).toSet() ??
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
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        // 부분 성공도 정확히: 성공 n개 + 실패 m개를 함께 안내.
        content: Text(failed.isEmpty
            ? l.noteDeletedCount(okCount)
            : l.noteDeletePartial(okCount, failed.length)),
        duration: const Duration(seconds: 2),
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
    // 작성 버튼은 선택 모드만 아니면 항상 보인다(어느 카테고리에서도 노트 작성 가능).
    // QT·설교 칩에서도 일반 작성 흐름(N-02 카테고리 선택)으로 진입한다.
    final showFab = !selectionMode;

    return Scaffold(
      // 선택 모드면 AppBar가 ✕ + "n개 선택" + 전체선택으로 바뀐다.
      appBar: selectionMode
          ? AppBar(
              title: Text(l.noteSelectedCount(selectedIds.length)),
              centerTitle: true,
              // ✕/전체선택/선택취소는 모두 칩 줄 오른쪽으로 내렸다.
            )
          : AppBar(
              title: Text(l.navRecord),
              centerTitle: true,
            ),
      // ✏️ 작성 진입은 우하단 + FAB 하나로 통일한다(모든 카테고리 동일한 둥근 형식).
      // 기도/회개/감사 칩에서는 N-02를 건너뛰고 그 카테고리로 바로 작성(N-03), 그 외엔
      // N-02(카테고리 선택)로 간다 — 동작만 다르고 버튼 모양은 같다.
      // AI 챗봇 런처처럼 둥글게 떠 있는 액센트 + 버튼.
      floatingActionButton: !showFab
          ? null
          : FloatingActionButton(
              heroTag: 'note-create-fab',
              onPressed: () => quickCreateCategory != null
                  ? Navigator.of(context).pushNamed(
                      AppRouter.noteEdit,
                      arguments: NoteEditArgs(category: quickCreateCategory),
                    )
                  : Navigator.of(context).pushNamed(AppRouter.noteCategorySelect),
              backgroundColor: context.appColors.accentDot,
              foregroundColor: Colors.white,
              shape: const CircleBorder(),
              child: const Icon(Icons.add, size: 28),
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
          // 카테고리 칩(전체 + 5종, 가로 스크롤) + 우측 상태 필터 드롭다운 — 한 줄에 배치.
          SizedBox(
            height: 48,
            child: Row(
              children: [
                Expanded(
                  child: ListView(
                    key: const ValueKey('note-category-chip-list'),
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
                // 칩 줄 오른쪽:
                //  · 비선택 = [전체 상태 드롭다운] [☰ 선택 진입]
                //  · 선택 모드 = [전체선택(드롭다운 자리)] [선택취소(☰ 자리)]
                if (!selectionMode) ...[
                  Padding(
                    padding: const EdgeInsets.only(left: 4),
                    child: _StatusFilterDropdown(selected: selectedStatus),
                  ),
                  TextButton(
                    onPressed: () => ref
                        .read(noteSelectionModeProvider.notifier)
                        .state = true,
                    style: TextButton.styleFrom(
                      visualDensity: VisualDensity.compact,
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                    ),
                    child: Text(l.noteSelect),
                  ),
                ] else ...[
                  TextButton(
                    onPressed: () => _selectAll(ref),
                    style: TextButton.styleFrom(
                      visualDensity: VisualDensity.compact,
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                    ),
                    child: Text(l.noteSelectAll),
                  ),
                  TextButton(
                    onPressed: () => _exitSelection(ref),
                    style: TextButton.styleFrom(
                      visualDensity: VisualDensity.compact,
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                    ),
                    child: const Text('선택취소'),
                  ),
                ],
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
          child: ListView.builder(
            itemCount: response.items.length,
            padding: const EdgeInsets.fromLTRB(12, 0, 16, 16),
            itemBuilder: (context, index) {
              final item = response.items[index];
              final card = NoteCard(
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
              // 좌측 타임라인 구분바(점 + 세로 연결선) + 카드.
              return IntrinsicHeight(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _NoteTimelineRail(
                      isFirst: index == 0,
                      isLast: index == response.items.length - 1,
                    ),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: card,
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        );
      },
    );
  }
}

/// 기록 목록 좌측 타임라인 구분바 — 항목마다 점, 점들을 세로선으로 연결한다.
class _NoteTimelineRail extends StatelessWidget {
  final bool isFirst;
  final bool isLast;

  const _NoteTimelineRail({required this.isFirst, required this.isLast});

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    return SizedBox(
      width: 18,
      child: CustomPaint(
        painter: _TimelineRailPainter(
          isFirst: isFirst,
          isLast: isLast,
          lineColor: colors.hairline,
          dotColor: colors.text2,
        ),
      ),
    );
  }
}

class _TimelineRailPainter extends CustomPainter {
  final bool isFirst;
  final bool isLast;
  final Color lineColor;
  final Color dotColor;

  // 점이 카드 상단(날짜 줄)과 비슷한 높이에 오도록.
  static const double _dotY = 22;
  static const double _dotR = 4;

  const _TimelineRailPainter({
    required this.isFirst,
    required this.isLast,
    required this.lineColor,
    required this.dotColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final cx = size.width / 2;
    final line = Paint()
      ..color = lineColor
      ..strokeWidth = 2
      ..strokeCap = StrokeCap.round;
    // 점 위쪽 선(첫 항목 제외).
    if (!isFirst) {
      canvas.drawLine(Offset(cx, 0), Offset(cx, _dotY), line);
    }
    // 점 아래쪽 선(마지막 항목 제외) — 다음 점까지 이어진다.
    if (!isLast) {
      canvas.drawLine(Offset(cx, _dotY), Offset(cx, size.height), line);
    }
    // 점.
    canvas.drawCircle(Offset(cx, _dotY), _dotR, Paint()..color = dotColor);
  }

  @override
  bool shouldRepaint(_TimelineRailPainter old) =>
      old.isFirst != isFirst ||
      old.isLast != isLast ||
      old.lineColor != lineColor ||
      old.dotColor != dotColor;
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
      // '나눔' — 나눔 페이지에 공개한 노트만(서버 status가 아닌 공유 여부로 거름).
      kNoteSharedFilter: '나눔',
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
