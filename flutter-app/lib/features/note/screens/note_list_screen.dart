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
class NoteListScreen extends ConsumerWidget {
  const NoteListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedCategory = ref.watch(noteCategoryFilterProvider);
    final selectedStatus = ref.watch(noteStatusFilterProvider);
    final l = AppLocalizations.of(context);

    // 기도/회개/감사 칩이 선택됐을 때만 작성 단축이 가능하다(칩 컨텍스트 작성).
    // QT·설교는 각자 QT/성경 화면에서 작성하므로 여기서 작성 진입을 두지 않는다.
    final quickCreateCategory =
        writableNoteCategories.contains(selectedCategory)
            ? selectedCategory
            : null;

    return Scaffold(
      appBar: AppBar(
        title: Text(l.navRecord),
        centerTitle: true,
      ),
      // ✏️ 작성 진입은 Material 관례대로 우하단 FAB 하나로 모은다(항상 노출 → 발견성).
      // 기도/회개/감사 칩 선택 시에는 FAB가 "+ {카테고리} 작성" 확장 버튼으로 바뀌어
      // N-02(카테고리 선택)를 건너뛰고 N-03로 직행한다. 그 외에는 N-02로 간다.
      // FAB 색은 페일 블러시(#EADAD2) 채움 + 다크 브라운 글자(라이트/다크 공통 고정색).
      // 크기·여백도 타이트하게. 전역 FAB 토큰은 그대로 두고 이 화면만 오버라이드.
      floatingActionButton: quickCreateCategory != null
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
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
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
              onPressed: () =>
                  Navigator.of(context).pushNamed(AppRouter.noteCategorySelect),
              backgroundColor: _kFabBg,
              foregroundColor: _kFabFg,
              elevation: 0,
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              child: const Icon(Icons.add, size: 22),
            ),
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
          const Divider(height: 1),
          // 구분선과 목록 사이 숨통(디자인 간격).
          const SizedBox(height: 8),
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
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            separatorBuilder: (_, __) => const SizedBox(height: 10),
            itemBuilder: (context, index) {
              final item = response.items[index];
              // 항목 탭 → N-04 상세. 수정·삭제 시 그쪽이 invalidate → 돌아오면 최신 반영.
              return NoteCard(
                item: item,
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
