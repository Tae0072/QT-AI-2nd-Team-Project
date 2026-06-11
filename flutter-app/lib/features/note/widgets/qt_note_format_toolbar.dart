import 'package:flutter/material.dart';

class QtNoteFormatToolbar extends StatelessWidget {
  final double fontSize;
  final Color textColor;
  final Color backgroundColor;
  final VoidCallback onFontSize;
  final VoidCallback onBold;
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
    required this.onFontSize,
    required this.onBold,
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
          _fontSizeButton(fontSize, onFontSize),
          _button(Icons.alternate_email, '구절 삽입', onVerseMention),
          _button(Icons.format_bold, '굵게', onBold),
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

  Widget _button(IconData icon, String tooltip, VoidCallback onPressed) {
    return IconButton(
      tooltip: tooltip,
      icon: Icon(icon, size: 21),
      onPressed: onPressed,
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
