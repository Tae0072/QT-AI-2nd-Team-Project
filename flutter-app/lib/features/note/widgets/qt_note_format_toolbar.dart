import 'package:flutter/material.dart';

class QtNoteFormatToolbar extends StatelessWidget {
  final double fontSize;
  final VoidCallback onFontSize;
  final VoidCallback onBold;
  final VoidCallback onHighlight;
  final VoidCallback onIndent;
  final VoidCallback onBullet;
  final VoidCallback onParenNumber;
  final VoidCallback onPlainNumber;
  final VoidCallback onVerseMention;

  const QtNoteFormatToolbar({
    super.key,
    required this.fontSize,
    required this.onFontSize,
    required this.onBold,
    required this.onHighlight,
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
          _button(Icons.format_bold, '굵게', onBold),
          _button(Icons.border_color_outlined, '하이라이트', onHighlight),
          _button(Icons.format_indent_increase, '들여쓰기', onIndent),
          _button(Icons.format_list_bulleted, '동그라미 목록', onBullet),
          _textButton('(1)', '(1) 목록', onParenNumber),
          _textButton('1)', '1) 목록', onPlainNumber),
          _button(Icons.alternate_email, '구절 삽입', onVerseMention),
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
