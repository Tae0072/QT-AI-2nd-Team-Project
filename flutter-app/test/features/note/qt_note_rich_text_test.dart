import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/note/models/qt_note_rich_text.dart';

void main() {
  test('parses stacked bold, font size, text color, and background markers',
      () {
    final span = QtNoteRichTextParser.parse(
      '**[fs=22][fg=#2563EB][bg=#DBEAFE]말씀**',
      const TextStyle(fontSize: 16, color: Colors.black),
      emojiFontSize: 16,
    );

    final visible = span.children!
        .whereType<TextSpan>()
        .where((child) => child.text == '말' || child.text == '씀')
        .toList();

    expect(visible, hasLength(2));
    for (final child in visible) {
      expect(child.style!.fontWeight, FontWeight.w700);
      expect(child.style!.fontSize, 22);
      expect(child.style!.color, const Color(0xFF2563EB));
      expect(child.style!.backgroundColor, const Color(0xFFDBEAFE));
    }
  });

  test('builds scoped markers around a selected range', () {
    final updated = QtNoteRichTextMarkup.applyScopedMarker(
      text: 'abc',
      start: 1,
      end: 2,
      openMarker: '[fg=#DC2626]',
      closeMarker: '[fg=]',
    );

    expect(updated.text, 'a[fg=#DC2626]b[fg=]c');
    expect(updated.cursor, 'a[fg=#DC2626]b[fg=]'.length);
  });

  test('builds future markers at a collapsed cursor', () {
    final updated = QtNoteRichTextMarkup.applyScopedMarker(
      text: 'abc',
      start: 3,
      end: 3,
      openMarker: '[fs=24]',
      closeMarker: '[fs=]',
    );

    expect(updated.text, 'abc[fs=24]');
    expect(updated.cursor, 'abc[fs=24]'.length);
  });
}
