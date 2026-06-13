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

  const QtNoteFormatToolbar({
    super.key,
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
    return SizedBox(
      height: 46,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          _fontFamilyButton(),
          _fontSizeButton(fontSize, onFontSize),
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
        ],
      ),
    );
  }

  Widget _button(
    IconData icon,
    String tooltip,
    VoidCallback onPressed, {
    bool active = false,
  }) {
    // 적용 중인 서식은 색칠된 배경 + 강조색 아이콘으로 켜진 상태를 보여준다.
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

  /// 서체(글꼴) 선택 드롭다운. 현재 선택 라벨을 보여주고 누르면 목록이 뜬다.
  Widget _fontFamilyButton() {
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
        child: Padding(
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

  Widget _fontSizeButton(double fontSize, VoidCallback onPressed) {
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
