import 'package:flutter/material.dart';

class QtNoteFormatToolbar extends StatelessWidget {
  final double fontSize;
  final Color textColor;
  final Color backgroundColor;
  final String fontFamilyLabel;
  final List<String> fontFamilyOptions;
  final ValueChanged<String> onFontFamily;
  final VoidCallback onFontSize;
  final bool boldActive;
  final bool italicActive;
  final bool underlineActive;
  final bool strikethroughActive;
  final VoidCallback onBold;
  final VoidCallback onItalic;
  final VoidCallback onUnderline;
  final VoidCallback onStrikethrough;
  final VoidCallback onTextColor;
  final VoidCallback onBackgroundColor;
  final VoidCallback onIndent;
  final VoidCallback onBullet;
  final VoidCallback onParenNumber;
  final VoidCallback onPlainNumber;
  final VoidCallback onVerseMention;

  /// 툴바 진행 방향. [Axis.horizontal]=가로(기존), [Axis.vertical]=왼쪽 세로 패널.
  final Axis axis;

  // ── 손그림/페이지 모드(선택) ─────────────────────────────────────────────
  // 콜백이 주어지면 해당 버튼을 그린다. 없으면(기존 호출부) 버튼을 숨겨 호환 유지.
  final bool penActive;
  final bool eraserActive;
  final bool manuscriptActive;
  final VoidCallback? onTogglePen;

  /// 펜 버튼을 '길게 누르면' 호출 — 펜 색상 선택 시트를 연다(선택 사항).
  final VoidCallback? onTogglePenLongPress;
  final VoidCallback? onToggleEraser;
  final VoidCallback? onUndoStroke;
  final VoidCallback? onClearStrokes;
  final VoidCallback? onToggleManuscript;

  const QtNoteFormatToolbar({
    super.key,
    this.axis = Axis.horizontal,
    this.penActive = false,
    this.eraserActive = false,
    this.manuscriptActive = false,
    this.onTogglePen,
    this.onTogglePenLongPress,
    this.onToggleEraser,
    this.onUndoStroke,
    this.onClearStrokes,
    this.onToggleManuscript,
    required this.fontSize,
    required this.textColor,
    required this.backgroundColor,
    required this.fontFamilyLabel,
    required this.fontFamilyOptions,
    required this.onFontFamily,
    required this.onFontSize,
    this.boldActive = false,
    this.italicActive = false,
    this.underlineActive = false,
    this.strikethroughActive = false,
    required this.onBold,
    required this.onItalic,
    required this.onUnderline,
    required this.onStrikethrough,
    required this.onTextColor,
    required this.onBackgroundColor,
    required this.onIndent,
    required this.onBullet,
    required this.onParenNumber,
    required this.onPlainNumber,
    required this.onVerseMention,
  });

  @override
  Widget build(BuildContext context) {
    final vertical = axis == Axis.vertical;
    final children = <Widget>[
      _fontFamilyButton(vertical),
      _fontSizeButton(fontSize, onFontSize, vertical),
      _button(Icons.alternate_email, '구절 삽입', onVerseMention),
      _button(Icons.format_bold, '굵게', onBold, active: boldActive),
      _button(Icons.format_italic, '기울임', onItalic, active: italicActive),
      _button(Icons.format_underlined, '밑줄', onUnderline,
          active: underlineActive),
      _button(Icons.format_strikethrough, '취소선', onStrikethrough,
          active: strikethroughActive),
      _colorButton(
        icon: Icons.format_color_text,
        tooltip: '텍스트 색상',
        color: textColor,
        onPressed: onTextColor,
      ),
      _colorButton(
        icon: Icons.format_color_fill,
        tooltip: '배경 색상',
        color: backgroundColor,
        onPressed: onBackgroundColor,
      ),
      _button(Icons.format_indent_increase, '들여쓰기', onIndent),
      _button(Icons.format_list_bulleted, '동그라미 목록', onBullet),
      _textButton('(1)', '(1) 목록', onParenNumber),
      _textButton('1)', '1) 목록', onPlainNumber),
      // 손그림/페이지 모드 도구(콜백이 있을 때만).
      if (onTogglePen != null || onToggleManuscript != null) _divider(vertical),
      if (onToggleManuscript != null)
        _button(Icons.horizontal_rule, '원고/일반 전환', onToggleManuscript!,
            active: manuscriptActive),
      if (onTogglePen != null)
        // 툴팁은 기존 그대로 유지(테스트·UX 계약). 길게 누르면 펜 색 선택이 뜬다.
        _button(Icons.draw, '펜으로 그리기', onTogglePen!,
            active: penActive, onLongPress: onTogglePenLongPress),
      if (onToggleEraser != null)
        _button(Icons.auto_fix_normal, '지우개', onToggleEraser!,
            active: eraserActive),
      if (onUndoStroke != null)
        _button(Icons.undo, '획 실행취소', onUndoStroke!),
      if (onClearStrokes != null)
        _button(Icons.delete_outline, '그림 전체 지우기', onClearStrokes!),
    ];

    // 세로(왼쪽 패널): 좁은 고정폭. SingleChildScrollView+Column으로 모든 버튼을
    // 즉시 생성한다(ListView는 화면 밖 버튼을 지연 생성해 접근이 어렵다). 길면 스크롤된다.
    if (vertical) {
      return SizedBox(
        width: 52,
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: children,
          ),
        ),
      );
    }
    return SizedBox(
      height: 46,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: children,
      ),
    );
  }

  /// 서식 버튼과 그림 도구를 구분하는 얇은 구분선.
  Widget _divider(bool vertical) {
    return vertical
        ? const Padding(
            padding: EdgeInsets.symmetric(vertical: 6, horizontal: 12),
            child: Divider(height: 1, thickness: 1),
          )
        : const Padding(
            padding: EdgeInsets.symmetric(horizontal: 4, vertical: 8),
            child: VerticalDivider(width: 1, thickness: 1),
          );
  }

  Widget _button(
    IconData icon,
    String tooltip,
    VoidCallback onPressed, {
    bool active = false,
    VoidCallback? onLongPress,
  }) {
    // 적용 중인 서식은 색칠된 배경 + 강조색 아이콘으로 켜진 상태를 보여준다.
    // 길게 누르기가 필요 없는 일반 버튼은 기존처럼 IconButton을 쓴다.
    if (onLongPress == null) {
      return IconButton(
        tooltip: tooltip,
        icon: Icon(icon, size: 21),
        isSelected: active,
        style: active
            ? IconButton.styleFrom(
                backgroundColor: const Color(0xFFE5E7EB),
                foregroundColor: const Color(0xFF111827),
              )
            : null,
        onPressed: onPressed,
      );
    }
    // 길게 누르기가 필요한 버튼(펜)은 InkWell로 직접 구성한다.
    // IconButton 바깥을 GestureDetector로 감싸면 내부 InkWell이 제스처를 가로채
    // 롱프레스가 동작하지 않으므로, 탭·롱프레스를 같은 InkWell에 둔다(아레나 충돌 없음).
    return Tooltip(
      message: tooltip,
      child: InkWell(
        onTap: onPressed,
        onLongPress: onLongPress,
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.all(8),
          decoration: active
              ? BoxDecoration(
                  color: const Color(0xFFE5E7EB),
                  borderRadius: BorderRadius.circular(8),
                )
              : null,
          child: Icon(icon,
              size: 21, color: active ? const Color(0xFF111827) : null),
        ),
      ),
    );
  }

  /// 서체(글꼴) 선택 드롭다운. 현재 선택 라벨을 보여주고 누르면 목록이 뜬다.
  /// 세로 패널([vertical])에선 폭이 좁아 글꼴 아이콘만 보여준다.
  Widget _fontFamilyButton(bool vertical) {
    return Tooltip(
      message: '서체',
      child: PopupMenuButton<String>(
        onSelected: onFontFamily,
        itemBuilder: (context) => [
          for (final option in fontFamilyOptions)
            PopupMenuItem<String>(
              value: option,
              child: Row(
                children: [
                  if (option == fontFamilyLabel)
                    const Icon(Icons.check, size: 18)
                  else
                    const SizedBox(width: 18),
                  const SizedBox(width: 8),
                  Text(option),
                ],
              ),
            ),
        ],
        child: vertical
            ? const Padding(
                padding: EdgeInsets.symmetric(vertical: 10),
                child: Icon(Icons.font_download_outlined, size: 22),
              )
            : Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(fontFamilyLabel),
                    const Icon(Icons.arrow_drop_down, size: 20),
                  ],
                ),
              ),
      ),
    );
  }

  Widget _fontSizeButton(double fontSize, VoidCallback onPressed, bool vertical) {
    // 세로 패널에선 아이콘 위·숫자 아래로 좁게 쌓는다.
    if (vertical) {
      return Tooltip(
        message: '글씨 크기',
        child: InkWell(
          onTap: onPressed,
          borderRadius: BorderRadius.circular(8),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 6),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.format_size, size: 20),
                Text(
                  fontSize.round().toString(),
                  style: const TextStyle(fontSize: 11),
                ),
              ],
            ),
          ),
        ),
      );
    }
    return Tooltip(
      message: '글씨 크기',
      child: TextButton.icon(
        onPressed: onPressed,
        icon: const Icon(Icons.format_size, size: 21),
        label: Text(fontSize.round().toString()),
      ),
    );
  }

  Widget _colorButton({
    required IconData icon,
    required String tooltip,
    required Color color,
    required VoidCallback onPressed,
  }) {
    return Tooltip(
      message: tooltip,
      child: IconButton(
        onPressed: onPressed,
        icon: Stack(
          alignment: Alignment.bottomRight,
          children: [
            Icon(icon, size: 22),
            Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                color: color,
                border: Border.all(color: const Color(0xFF9CA3AF)),
                borderRadius: BorderRadius.circular(3),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _textButton(String label, String tooltip, VoidCallback onPressed) {
    return Tooltip(
      message: tooltip,
      child: TextButton(
        onPressed: onPressed,
        child: Text(label),
      ),
    );
  }
}
