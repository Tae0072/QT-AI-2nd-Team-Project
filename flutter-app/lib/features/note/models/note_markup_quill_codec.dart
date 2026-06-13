import 'package:flutter_quill/quill_delta.dart';

/// 노트 본문의 "마커 평문"(저장 형식)과 Quill `Delta`(편집 형식)를 양방향 변환한다.
///
/// 저장 형식은 그대로 유지(백엔드·상세보기 호환)하면서 편집만 flutter_quill로 하기
/// 위한 코덱이다. 마커 규칙은 [QtNoteRichTextParser]와 동일하다.
///
/// 인라인 서식 매핑:
/// - `**` 굵게  ↔ bold
/// - `//` 기울임 ↔ italic
/// - `__` 밑줄  ↔ underline
/// - `~~` 취소선 ↔ strike
/// - `[fs=N]…[fs=]`     ↔ size(숫자 문자열)
/// - `[fg=#HEX]…[fg=]`  ↔ color
/// - `[bg=#HEX]…[bg=]`  ↔ background
/// - `==`(레거시 하이라이트) ↔ background `#FFF2A8`
///
/// 글머리표/번호(`• `, `(1) `, `1) `)는 현재 저장 형식상 본문 안의 "일반 글자"이므로
/// 별도 블록 속성으로 바꾸지 않고 텍스트 그대로 둔다(라운드트립 정확성 우선).
class NoteMarkupQuillCodec {
  const NoteMarkupQuillCodec._();

  static final RegExp _styleMarker =
      RegExp(r'\[(fs|fg|bg)=([#A-Fa-f0-9]*)\]');
  static const String _legacyHighlight = '#FFF2A8';

  /// 마커 평문 → Quill Delta. 결과는 항상 끝에 `\n`을 둬 Quill 문서 규약을 맞춘다.
  static Delta markersToDelta(String text) {
    final delta = Delta();
    var bold = false;
    var italic = false;
    var underline = false;
    var strike = false;
    String? size;
    String? color;
    String? background;
    final buffer = StringBuffer();

    void flush() {
      if (buffer.isEmpty) return;
      final attrs = <String, dynamic>{};
      if (bold) attrs['bold'] = true;
      if (italic) attrs['italic'] = true;
      if (underline) attrs['underline'] = true;
      if (strike) attrs['strike'] = true;
      if (size != null) attrs['size'] = size;
      if (color != null) attrs['color'] = color;
      if (background != null) attrs['background'] = background;
      delta.insert(buffer.toString(), attrs.isEmpty ? null : attrs);
      buffer.clear();
    }

    var i = 0;
    while (i < text.length) {
      if (text.startsWith('**', i)) {
        flush();
        bold = !bold;
        i += 2;
        continue;
      }
      if (text.startsWith('//', i)) {
        flush();
        italic = !italic;
        i += 2;
        continue;
      }
      if (text.startsWith('__', i)) {
        flush();
        underline = !underline;
        i += 2;
        continue;
      }
      if (text.startsWith('~~', i)) {
        flush();
        strike = !strike;
        i += 2;
        continue;
      }
      if (text.startsWith('==', i)) {
        flush();
        background = background == _legacyHighlight ? null : _legacyHighlight;
        i += 2;
        continue;
      }
      final marker = _styleMarker.matchAsPrefix(text, i);
      if (marker != null) {
        flush();
        final key = marker.group(1)!;
        final value = marker.group(2)!;
        switch (key) {
          case 'fs':
            size = value.isEmpty ? null : value;
          case 'fg':
            color = value.isEmpty ? null : value;
          case 'bg':
            background = value.isEmpty ? null : value;
        }
        i += marker.group(0)!.length;
        continue;
      }
      buffer.write(text[i]);
      i += 1;
    }
    flush();

    // Quill 문서는 반드시 '\n'으로 끝나야 한다.
    final ops = delta.toList();
    final lastIsText = ops.isNotEmpty && ops.last.data is String;
    final endsWithNewline =
        lastIsText && (ops.last.data as String).endsWith('\n');
    if (!endsWithNewline) {
      delta.insert('\n');
    }
    return delta;
  }

  /// Quill Delta → 마커 평문. Quill이 강제하는 끝 `\n` 하나는 제거해 저장 형식과 맞춘다.
  static String deltaToMarkers(Delta delta) {
    final sb = StringBuffer();
    var bold = false;
    var italic = false;
    var underline = false;
    var strike = false;
    String? size;
    String? color;
    String? background;

    for (final op in delta.toList()) {
      final data = op.data;
      if (data is! String) continue; // 임베드(이미지 등)는 현재 형식 밖이므로 건너뛴다.
      final attrs = op.attributes ?? const <String, dynamic>{};
      final nextBold = attrs['bold'] == true;
      final nextItalic = attrs['italic'] == true;
      final nextUnderline = attrs['underline'] == true;
      final nextStrike = attrs['strike'] == true;
      final nextSize = attrs['size']?.toString();
      final nextColor = attrs['color']?.toString();
      final nextBackground = attrs['background']?.toString();

      if (nextBold != bold) {
        sb.write('**');
        bold = nextBold;
      }
      if (nextItalic != italic) {
        sb.write('//');
        italic = nextItalic;
      }
      if (nextUnderline != underline) {
        sb.write('__');
        underline = nextUnderline;
      }
      if (nextStrike != strike) {
        sb.write('~~');
        strike = nextStrike;
      }
      if (nextSize != size) {
        sb.write('[fs=${nextSize ?? ''}]');
        size = nextSize;
      }
      if (nextColor != color) {
        sb.write('[fg=${nextColor ?? ''}]');
        color = nextColor;
      }
      if (nextBackground != background) {
        sb.write('[bg=${nextBackground ?? ''}]');
        background = nextBackground;
      }
      sb.write(data);
    }

    // 끝에 열린 채로 남은 서식을 닫는다.
    if (bold) sb.write('**');
    if (italic) sb.write('//');
    if (underline) sb.write('__');
    if (strike) sb.write('~~');
    if (size != null) sb.write('[fs=]');
    if (color != null) sb.write('[fg=]');
    if (background != null) sb.write('[bg=]');

    var out = sb.toString();
    if (out.endsWith('\n')) {
      out = out.substring(0, out.length - 1);
    }
    return out;
  }
}
