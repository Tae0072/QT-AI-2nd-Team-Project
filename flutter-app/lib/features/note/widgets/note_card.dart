import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/utils/date_format_utils.dart';
import '../models/note_models.dart';

/// 기록(노트) 목록 카드.
///
/// 레이아웃(승인 시안): 좌측 카테고리 색 세로선 + [날짜·시각(스택)·배지(오전/오후 + 카테고리)
/// → 제목 → 본문 2줄 미리보기]. 아이콘·썸네일은 쓰지 않는다.
/// 다중 선택 모드에서는 본문 앞에 체크 아이콘을 두고, 탭은 선택 토글로 동작한다.
class NoteCard extends StatelessWidget {
  final NoteListItem item;
  final VoidCallback onTap;

  /// 다중 선택 모드. true면 좌측 체크박스를 보이고, 탭은 [onToggleSelect]로 간다.
  final bool selectionMode;
  final bool selected;
  final VoidCallback? onToggleSelect;

  const NoteCard({
    super.key,
    required this.item,
    required this.onTap,
    this.selectionMode = false,
    this.selected = false,
    this.onToggleSelect,
  });

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    final l = AppLocalizations.of(context);
    final accent = _accentOf(item.category);
    final title = item.title.isEmpty ? l.noteUntitled : item.title;

    // 날짜·시각 — 편집/생성 시각이 있으면 날짜+시각+오전/오후, 없으면 QT 날짜만.
    final ts = item.updatedAt ?? item.createdAt;
    String? dateLine;
    String? timeLine;
    String? amPm;
    if (ts != null) {
      dateLine = dateDotLabel(ts);
      timeLine = clockLabel(ts);
      amPm = amPmKoLabel(ts);
    } else if (item.qtDate != null && item.qtDate!.isNotEmpty) {
      dateLine = dateDotFromIso(item.qtDate!);
    }

    final preview = item.bodyPreview?.trim();

    return Container(
      decoration: BoxDecoration(
        color: c.bgSunken,
        borderRadius: BorderRadius.circular(14),
        // 선택된 카드는 강조 테두리로 구분한다.
        border: selected ? Border.all(color: c.accent, width: 1.5) : null,
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        // 선택 모드면 탭이 선택 토글, 아니면 상세 이동.
        onTap: selectionMode ? onToggleSelect : onTap,
        child: IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 카테고리 색 세로선 — 카드 높이만큼 늘어난다(좌측 라운드에 맞춰 클립).
              Container(width: 3, color: accent),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(14, 13, 14, 13),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // 다중 선택 모드: 좌측 체크 아이콘
                      if (selectionMode) ...[
                        Padding(
                          padding: const EdgeInsets.only(top: 1, right: 10),
                          child: Icon(
                            selected
                                ? Icons.check_circle
                                : Icons.radio_button_unchecked,
                            size: 22,
                            color: selected ? c.accent : c.textMuted,
                          ),
                        ),
                      ],
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                // 날짜·시각(왼쪽, 위아래로 스택)
                                if (dateLine != null) ...[
                                  Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(dateLine, style: _metaStyle(c)),
                                      if (timeLine != null)
                                        Text(timeLine, style: _metaStyle(c)),
                                    ],
                                  ),
                                  const SizedBox(width: 8),
                                ],
                                // 배지(오른쪽, 공간 부족 시 다음 줄로): 오전/오후 + 카테고리 (+ 임시저장/나눔)
                                Expanded(
                                  child: Wrap(
                                    spacing: 6,
                                    runSpacing: 6,
                                    alignment: WrapAlignment.end,
                                    children: [
                                      if (amPm != null)
                                        _Chip(label: amPm, color: c.text2),
                                      _Chip(
                                          label:
                                              noteCategoryLabel(item.category),
                                          color: accent,
                                          tinted: true),
                                      if (item.status == 'DRAFT')
                                        _Chip(
                                            label: l.noteDraft,
                                            color: c.textMuted),
                                      if (item.shared)
                                        _Chip(
                                            label: '나눔공개',
                                            color: accent,
                                            tinted: true),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text(
                              title,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontFamily: 'GowunDodum',
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                height: 1.3,
                                color: c.text,
                              ),
                            ),
                            if (preview != null && preview.isNotEmpty) ...[
                              const SizedBox(height: 4),
                              Text(
                                preview,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  fontFamily: 'GowunDodum',
                                  fontSize: 13,
                                  height: 1.5,
                                  color: c.text2,
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  TextStyle _metaStyle(AppColors c) => TextStyle(
        fontFamily: 'GowunDodum',
        fontSize: 12,
        height: 1.35,
        color: c.text2,
      );
}

/// 카드 배지(알약). [tinted]면 색의 옅은 배경 + 진한 글자, 아니면 외곽선 없는 회색 칩.
class _Chip extends StatelessWidget {
  final String label;
  final Color color;
  final bool tinted;

  const _Chip({required this.label, required this.color, this.tinted = false});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
      decoration: BoxDecoration(
        color: tinted ? color.withValues(alpha: 0.16) : context.appColors.bg,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontFamily: 'GowunDodum',
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }
}

/// 카테고리 색 세로선·배지 accent (라이트/다크 공통으로 읽히는 중간 톤).
const Map<String, Color> _catAccent = {
  'MEDITATION': Color(0xFFD99A2B), // QT — 금빛
  'SERMON': Color(0xFF2FA37C), // 설교 — 세이지/틸
  'PRAYER': Color(0xFF8B79C9), // 기도 — 라일락
  'REPENTANCE': Color(0xFFC97A6D), // 회개 — 로즈
  'GRATITUDE': Color(0xFFCB9A52), // 감사 — 골드
};

Color _accentOf(String category) =>
    _catAccent[category] ?? const Color(0xFF9C9588);
