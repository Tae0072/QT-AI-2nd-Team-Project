import 'package:flutter/material.dart';

class QtNoteMarkupEdit {
  final String text;
  final int cursor;

  const QtNoteMarkupEdit({required this.text, required this.cursor});
}

final class QtNoteRichTextMarkup {
  const QtNoteRichTextMarkup._();

  static QtNoteMarkupEdit applyScopedMarker({
    required String text,
    required int start,
    required int end,
    required String openMarker,
    required String closeMarker,
  }) {
    final safeStart = start.clamp(0, text.length);
    final safeEnd = end.clamp(safeStart, text.length);
    final selected = text.substring(safeStart, safeEnd);

    if (safeStart == safeEnd) {
      final newText = text.replaceRange(safeStart, safeEnd, openMarker);
      return QtNoteMarkupEdit(
        text: newText,
        cursor: safeStart + openMarker.length,
      );
    }

    final replacement = '$openMarker$selected$closeMarker';
    final newText = text.replaceRange(safeStart, safeEnd, replacement);
    return QtNoteMarkupEdit(
      text: newText,
      cursor: safeEnd + openMarker.length + closeMarker.length,
    );
  }
}

final class QtNoteRichTextParser {
  static final RegExp _styleMarkerPattern =
      RegExp(r'\[(fs|fg|bg)=([#A-Fa-f0-9]*)\]');

  const QtNoteRichTextParser._();

  static TextSpan parse(
    String text,
    TextStyle? baseStyle, {
    required double emojiFontSize,
  }) {
    if (text.isEmpty) {
      return TextSpan(style: baseStyle, text: '');
    }

    final children = <TextSpan>[];
    var index = 0;
    var bold = false;
    var italic = false;
    var underline = false;
    var strikethrough = false;
    double? fontSize;
    Color? foregroundColor;
    Color? backgroundColor;

    while (index < text.length) {
      if (text.startsWith('**', index)) {
        _addHiddenMarker(children, baseStyle, '**');
        bold = !bold;
        index += 2;
        continue;
      }

      if (text.startsWith('//', index)) {
        _addHiddenMarker(children, baseStyle, '//');
        italic = !italic;
        index += 2;
        continue;
      }

      if (text.startsWith('__', index)) {
        _addHiddenMarker(children, baseStyle, '__');
        underline = !underline;
        index += 2;
        continue;
      }

      if (text.startsWith('~~', index)) {
        _addHiddenMarker(children, baseStyle, '~~');
        strikethrough = !strikethrough;
        index += 2;
        continue;
      }

      if (text.startsWith('==', index)) {
        _addHiddenMarker(children, baseStyle, '==');
        backgroundColor = backgroundColor == _legacyHighlightColor
            ? null
            : _legacyHighlightColor;
        index += 2;
        continue;
      }

      final markerMatch = _styleMarkerPattern.matchAsPrefix(text, index);
      if (markerMatch != null) {
        final marker = markerMatch.group(0)!;
        final key = markerMatch.group(1)!;
        final rawValue = markerMatch.group(2)!;
        _addHiddenMarker(children, baseStyle, marker);
        switch (key) {
          case 'fs':
            fontSize = rawValue.isEmpty ? null : double.tryParse(rawValue);
          case 'fg':
            foregroundColor = _parseColor(rawValue);
          case 'bg':
            backgroundColor = _parseColor(rawValue);
        }
        index += marker.length;
        continue;
      }

      final cluster = text.substring(index).characters.first;
      _addStyledCluster(
        children,
        cluster,
        _mergeStyle(
          baseStyle,
          bold: bold,
          italic: italic,
          underline: underline,
          strikethrough: strikethrough,
          fontSize: fontSize,
          foregroundColor: foregroundColor,
          backgroundColor: backgroundColor,
        ),
        emojiFontSize,
      );
      index += cluster.length;
    }

    return TextSpan(style: baseStyle, children: children);
  }

  static const Color _legacyHighlightColor = Color(0xFFFFF2A8);

  static TextStyle? _mergeStyle(
    TextStyle? baseStyle, {
    required bool bold,
    required bool italic,
    required bool underline,
    required bool strikethrough,
    required double? fontSize,
    required Color? foregroundColor,
    required Color? backgroundColor,
  }) {
    final decorations = <TextDecoration>[
      if (underline) TextDecoration.underline,
      if (strikethrough) TextDecoration.lineThrough,
    ];
    return baseStyle?.copyWith(
      fontWeight: bold ? FontWeight.w800 : baseStyle.fontWeight,
      fontStyle: italic ? FontStyle.italic : baseStyle.fontStyle,
      decoration: decorations.isEmpty
          ? baseStyle.decoration
          : TextDecoration.combine(decorations),
      fontSize: fontSize ?? baseStyle.fontSize,
      color: foregroundColor ?? baseStyle.color,
      backgroundColor: backgroundColor,
    );
  }

  static Color? _parseColor(String rawValue) {
    if (rawValue.isEmpty) {
      return null;
    }
    final hex = rawValue.startsWith('#') ? rawValue.substring(1) : rawValue;
    if (hex.length != 6) {
      return null;
    }
    final value = int.tryParse(hex, radix: 16);
    return value == null ? null : Color(0xFF000000 | value);
  }

  static void _addHiddenMarker(
    List<TextSpan> children,
    TextStyle? baseStyle,
    String marker,
  ) {
    children.add(TextSpan(
      text: marker,
      style: baseStyle?.copyWith(
        color: Colors.transparent,
        fontSize: 0.1,
      ),
    ));
  }

  static void _addStyledCluster(
    List<TextSpan> children,
    String cluster,
    TextStyle? style,
    double emojiFontSize,
  ) {
    children.add(TextSpan(
      text: cluster,
      style: _isEmojiCluster(cluster)
          ? style?.copyWith(fontSize: emojiFontSize)
          : style,
    ));
  }

  static bool _isEmojiCluster(String cluster) {
    return cluster.runes.any((rune) =>
        (rune >= 0x1F300 && rune <= 0x1FAFF) ||
        (rune >= 0x2600 && rune <= 0x27BF));
  }
}
