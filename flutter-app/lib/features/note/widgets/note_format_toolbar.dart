import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';

/// 노트 본문용 마크다운 서식 툴바 (N-03 작성/수정 공통).
///
/// 백엔드 body는 평문(text)이라, 진짜 리치텍스트 대신 마크다운 "마커 문자"를
/// 커서/선택 위치에 삽입한다(설계 A·2026-06-05). 저장은 그대로 평문.
/// 색상·글자크기·하이라이트는 마크다운으로 불가 → 회의 안건(B/리치텍스트).
class NoteFormatToolbar extends StatelessWidget {
  final TextEditingController controller;

  const NoteFormatToolbar({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    return SizedBox(
      height: 44,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          _btn(Icons.format_bold, l.fmtBold, () => _wrap('**')),
          _btn(Icons.format_italic, l.fmtItalic, () => _wrap('*')),
          _btn(Icons.title, l.fmtHeading, () => _linePrefix('# ')),
          _btn(Icons.format_list_bulleted, l.fmtList, () => _linePrefix('- ')),
          _btn(Icons.format_quote, l.fmtQuote, () => _linePrefix('> ')),
          _btn(Icons.check_box_outlined, l.fmtCheckbox, () => _linePrefix('- [ ] ')),
          _btn(Icons.horizontal_rule, l.fmtDivider, () => _insertBlock('---')),
        ],
      ),
    );
  }

  Widget _btn(IconData icon, String tooltip, VoidCallback onTap) {
    return IconButton(
      icon: Icon(icon, size: 20),
      tooltip: tooltip,
      onPressed: onTap,
    );
  }

  /// 선택 텍스트를 마커로 감싼다. 선택이 없으면 마커만 넣고 커서를 그 사이에 둔다.
  void _wrap(String marker) {
    final text = controller.text;
    final sel = controller.selection;
    // ✏️ 커서/선택이 아직 없을 수도 있어(포커스 전) 그 경우 맨 끝에 붙인다.
    final start = sel.start < 0 ? text.length : sel.start;
    final end = sel.end < 0 ? text.length : sel.end;
    final selected = text.substring(start, end);
    final newText = text.replaceRange(start, end, '$marker$selected$marker');
    // 선택이 있었으면 감싼 뒤 끝으로, 없었으면 마커 사이에 커서.
    final cursor =
        selected.isEmpty ? start + marker.length : end + marker.length * 2;
    _apply(newText, cursor);
  }

  /// 현재 줄의 맨 앞에 prefix를 붙인다(제목/목록/인용/체크박스).
  void _linePrefix(String prefix) {
    final text = controller.text;
    final sel = controller.selection;
    final pos = sel.start < 0 ? text.length : sel.start;
    // ✏️ 현재 줄 시작 = 커서 이전의 마지막 줄바꿈 다음 칸(없으면 0).
    final lineStart = text.lastIndexOf('\n', pos - 1) + 1;
    final newText = text.replaceRange(lineStart, lineStart, prefix);
    _apply(newText, pos + prefix.length);
  }

  /// 커서 위치에 한 줄짜리 블록(구분선 등)을 끼워 넣는다.
  void _insertBlock(String block) {
    final text = controller.text;
    final sel = controller.selection;
    final pos = sel.start < 0 ? text.length : sel.start;
    final insert = '\n$block\n';
    final newText = text.replaceRange(pos, pos, insert);
    _apply(newText, pos + insert.length);
  }

  /// 본문과 커서를 한 번에 갱신한다.
  void _apply(String newText, int cursor) {
    controller.value = TextEditingValue(
      text: newText,
      selection: TextSelection.collapsed(offset: cursor),
    );
  }
}
