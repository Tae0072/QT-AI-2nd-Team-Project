import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/utils/date_format_utils.dart';
import '../../../core/widgets/calm_paper.dart';
import '../models/note_models.dart';

/// 기록(노트) 목록 카드 (DESIGN_PROTOTYPE 사진처럼):
/// 제목 + 카테고리 배지 + 날짜·범위 메타 + (임시저장)·(나눔) 배지.
class NoteCard extends StatelessWidget {
  final NoteListItem item;
  final VoidCallback onTap;

  const NoteCard({super.key, required this.item, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    final l = AppLocalizations.of(context);
    final title = item.title.isEmpty ? l.noteUntitled : item.title;

    // 메타: 날짜(QT 노트는 qtDate, 그 외 updatedAt) + 범위 라벨.
    final metaParts = <String>[];
    final date = item.qtDate != null
        ? monthDayFromIso(item.qtDate!)
        : (item.updatedAt != null ? monthDayLabel(item.updatedAt!) : null);
    if (date != null) metaParts.add(date);
    if (item.rangeLabel != null && item.rangeLabel!.isNotEmpty) {
      metaParts.add(item.rangeLabel!);
    }
    final meta = metaParts.join(' · ');

    return Container(
      decoration: BoxDecoration(
        color: c.bgSunken,
        borderRadius: BorderRadius.circular(14),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                          fontFamily: 'GowunDodum',
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: c.text),
                    ),
                  ),
                  const SizedBox(width: 8),
                  CpBadge(noteCategoryLabel(item.category)),
                ],
              ),
              if (meta.isNotEmpty ||
                  item.status == 'DRAFT' ||
                  item.shared) ...[
                const SizedBox(height: 6),
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        meta,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                            fontFamily: 'GowunDodum',
                            fontSize: 13,
                            color: c.text2),
                      ),
                    ),
                    if (item.status == 'DRAFT')
                      Padding(
                        padding: const EdgeInsets.only(left: 8),
                        child: Text(l.noteDraft,
                            style: TextStyle(
                                fontFamily: 'GowunDodum',
                                fontSize: 12,
                                color: c.textMuted)),
                      ),
                    if (item.shared)
                      const Padding(
                        padding: EdgeInsets.only(left: 8),
                        child: CpBadge('나눔', dot: true),
                      ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
