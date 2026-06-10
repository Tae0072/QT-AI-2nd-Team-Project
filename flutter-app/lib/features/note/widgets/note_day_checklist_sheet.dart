import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../models/note_models.dart';

/// 묵상 달력에서 날짜 탭 시 그날의 노트 기록을 보여주는 체크리스트 바텀시트. (표시 전용)
///
/// 5종(설교·묵상·기도·회개·감사) 중 그날 작성된 카테고리에 ✓.
/// 탭해서 작성/이동하는 동작은 없다 — 평소 작성 흐름(예: + 버튼으로 기도 노트 작성)으로
/// 노트를 쓰면 달력 데이터가 갱신되며 해당 항목이 자동으로 ✓ 된다.
Future<void> showNoteDayChecklistSheet(
  BuildContext context,
  DateTime date,
  CalendarDay? day,
) {
  return showModalBottomSheet<void>(
    context: context,
    showDragHandle: true,
    builder: (_) => _NoteDayChecklistSheet(date: date, day: day),
  );
}

/// 체크리스트 표시 순서(요구사항 2026-06-08: 설교·묵상·기도·회개·감사).
const List<String> _checklistCategories = [
  'SERMON',
  'MEDITATION',
  'PRAYER',
  'REPENTANCE',
  'GRATITUDE',
];

class _NoteDayChecklistSheet extends StatelessWidget {
  final DateTime date;
  final CalendarDay? day;

  const _NoteDayChecklistSheet({required this.date, required this.day});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);
    // ✏️ 그날 작성된 카테고리 집합. 데이터가 없는 날(미작성)은 빈 집합 → 전부 미체크.
    final written = day?.categories.toSet() ?? <String>{};

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(_formatDate(date), style: theme.textTheme.titleMedium),
            const SizedBox(height: 2),
            Text(l.calDayChecklistTitle,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.outline)),
            const SizedBox(height: 8),
            for (final code in _checklistCategories)
              _ChecklistRow(
                label: noteCategoryLabel(code),
                checked: written.contains(code),
              ),
          ],
        ),
      ),
    );
  }

  String _formatDate(DateTime d) =>
      '${d.year}.${d.month.toString().padLeft(2, '0')}.${d.day.toString().padLeft(2, '0')}';
}

/// 체크리스트 한 줄 — 작성됨(✓ 강조) / 미작성(빈 원·흐림).
class _ChecklistRow extends StatelessWidget {
  final String label;
  final bool checked;

  const _ChecklistRow({required this.label, required this.checked});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Icon(
            checked ? Icons.check_circle : Icons.radio_button_unchecked,
            size: 22,
            color: checked
                ? theme.colorScheme.primary
                : theme.colorScheme.outlineVariant,
          ),
          const SizedBox(width: 12),
          Text(
            label,
            style: theme.textTheme.bodyLarge?.copyWith(
              color: checked ? null : theme.colorScheme.outline,
            ),
          ),
        ],
      ),
    );
  }
}
