import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../bible/models/bible_chapter_counts.dart';
import '../../bible/models/bible_models.dart';
import '../../bible/providers/bible_providers.dart';
import '../models/qt_note_rich_text.dart';
import 'qt_note_format_toolbar.dart';

/// 노트 본문 컨트롤러 — 마크업(`**`·`==`·`[fs/fg/bg=]`)을 라이브 프리뷰로 렌더한다.
///
/// QT 노트 에디터와 자유 노트(N-03) 에디터가 공유한다(QA ③⑨). 저장 본문은 마커가
/// 섞인 평문이고, N-04 상세도 같은 [QtNoteRichTextParser]로 렌더한다.
class NoteRichBodyController extends TextEditingController {
  final double emojiFontSize;

  NoteRichBodyController({this.emojiFontSize = 16, super.text});

  @override
  TextSpan buildTextSpan({
    required BuildContext context,
    TextStyle? style,
    required bool withComposing,
  }) {
    return QtNoteRichTextParser.parse(
      text,
      style,
      emojiFontSize: emojiFontSize,
    );
  }
}

enum NoteRichTextToolbarPlacement {
  bottom,
  top,
}

/// 리치텍스트 본문 편집 영역 (툴바 + @멘션 구절 삽입 + 본문 입력).
///
/// QT 노트 에디터에서 추출해 자유 노트 N-03과 공유한다(QA ③⑨ A안).
/// 화면(부모)은 제목·저장 버튼·미리보기 패널 등 주변 UI만 두고, 본문 편집은 이 위젯이 담당한다.
/// [controller]는 부모가 만들어 넘기고(저장 시 `controller.text`를 읽는다), 키·라벨은
/// 기존 QT 테스트 호환을 위해 파라미터로 받는다.
class NoteRichTextEditor extends ConsumerStatefulWidget {
  final NoteRichBodyController controller;

  /// 본문 입력창 labelText. QT='노트 작성', N-03=카테고리별 본문 라벨.
  final String bodyLabel;

  /// 본문 TextField 키(테스트 호환용, 선택).
  final Key? bodyFieldKey;

  /// 본문 Scrollbar 키(테스트 호환용, 선택).
  final Key? bodyScrollKey;

  /// @멘션으로 구절을 본문에 삽입할 때, 그 절들의 bibleVerseId를 알린다(선택).
  /// 자유/설교 노트(N-03)는 이를 모아 `note_verses`(verseIds)로 저장한다(§6.4.1).
  /// QT 에디터는 passage 절을 따로 저장하므로 이 콜백을 쓰지 않는다.
  final void Function(List<int> verseIds)? onVerseInserted;

  /// 본문 입력창 위에 함께 배치할 화면별 헤더(예: 제목 입력 박스).
  final Widget? header;

  /// 서식 수정바 배치 위치. 기본값은 기존 자유 노트 화면과 같은 본문 아래이다.
  /// [top]은 화면별 header가 있으면 header 아래, 본문 입력창 위에 배치한다.
  final NoteRichTextToolbarPlacement toolbarPlacement;

  const NoteRichTextEditor({
    super.key,
    required this.controller,
    required this.bodyLabel,
    this.bodyFieldKey,
    this.bodyScrollKey,
    this.onVerseInserted,
    this.header,
    this.toolbarPlacement = NoteRichTextToolbarPlacement.bottom,
  });

  @override
  ConsumerState<NoteRichTextEditor> createState() => _NoteRichTextEditorState();
}

class _NoteRichTextEditorState extends ConsumerState<NoteRichTextEditor> {
  static const double _minFontSize = 10;
  static const double _maxFontSize = 32;
  static const Color _defaultTextColor = Color(0xFF111827);
  static const Color _defaultBackgroundColor = Colors.transparent;

  final _editorScrollController = ScrollController();
  final _bodyFocusNode = FocusNode();
  Future<List<BibleBook>>? _booksFuture;
  String? _mentionQuery;
  TextSelection? _lastSelectedBodyRange;
  String? _lastSelectedBodySnapshot;
  String _lastBody = '';
  bool _applyingAutoPrefix = false;
  double _fontSize = 16;
  Color _textColor = _defaultTextColor;
  Color _backgroundColor = _defaultBackgroundColor;
  final List<Color> _recentColors = <Color>[
    Color(0xFFDC2626),
    Color(0xFF15803D),
    Color(0xFF7C3AED),
    Color(0xFF2563EB),
  ];

  NoteRichBodyController get _bodyController => widget.controller;

  @override
  void initState() {
    super.initState();
    _lastBody = _bodyController.text;
    _bodyController.addListener(_handleBodyChanged);
  }

  @override
  void dispose() {
    _bodyController.removeListener(_handleBodyChanged);
    _bodyFocusNode.dispose();
    _editorScrollController.dispose();
    super.dispose();
  }

  void _handleBodyChanged() {
    if (_applyingAutoPrefix) {
      _lastBody = _bodyController.text;
      return;
    }

    final text = _bodyController.text;
    final selection = _bodyController.selection;
    if (selection.isValid && !selection.isCollapsed) {
      _lastSelectedBodyRange = selection;
      _lastSelectedBodySnapshot = text;
    } else if (_lastSelectedBodySnapshot != null &&
        _lastSelectedBodySnapshot != text) {
      _lastSelectedBodyRange = null;
      _lastSelectedBodySnapshot = null;
    }
    _updateMentionQuery(text, selection.start);

    if (_removeUnusedNumberPrefixOnSpace(text, selection.start)) {
      return;
    }

    if (text.length == _lastBody.length + 1 &&
        selection.start > 0 &&
        text[selection.start - 1] == '\n') {
      final prefix = _nextLinePrefix(text, selection.start);
      if (prefix.isNotEmpty) {
        _insertAtCursor(prefix);
      }
    }
    _lastBody = _bodyController.text;
  }

  void _updateMentionQuery(String text, int cursor) {
    if (cursor < 0 || cursor > text.length) {
      setState(() => _mentionQuery = null);
      return;
    }
    final beforeCursor = text.substring(0, cursor);
    final at = beforeCursor.lastIndexOf('@');
    final token = at < 0 ? '' : beforeCursor.substring(at);
    final blockedByWhitespace =
        at < 0 || token.contains('\n') || token.length > 32;
    final nextQuery =
        blockedByWhitespace ? null : beforeCursor.substring(at + 1);
    if (nextQuery != _mentionQuery) {
      setState(() {
        _mentionQuery = nextQuery;
        if (nextQuery != null) {
          _booksFuture ??= ref.read(bibleRepositoryProvider).getBooks();
        }
      });
    }
  }

  String _nextLinePrefix(String text, int cursor) {
    final previousLineEnd = cursor - 2;
    if (previousLineEnd < 0) return '';
    final previousLineStart = text.lastIndexOf('\n', previousLineEnd) + 1;
    final previousLine = text.substring(previousLineStart, previousLineEnd + 1);
    final match =
        RegExp(r'^(\s*)(• |\((\d+)\) |(\d+)\) )').firstMatch(previousLine);
    if (match == null) return '';
    final indent = match.group(1) ?? '';
    final paren = match.group(3);
    final plain = match.group(4);
    if (paren != null) return '$indent(${int.parse(paren) + 1}) ';
    if (plain != null) return '$indent${int.parse(plain) + 1}) ';
    return '$indent• ';
  }

  bool _removeUnusedNumberPrefixOnSpace(String text, int cursor) {
    if (text.length != _lastBody.length + 1 ||
        cursor < 1 ||
        text[cursor - 1] != ' ') {
      return false;
    }
    final lineStart = text.lastIndexOf('\n', cursor - 1) + 1;
    final line = text.substring(lineStart, cursor);
    final match = RegExp(r'^(\s*)((\(\d+\)|\d+\))\s{2})$').firstMatch(line);
    if (match == null) {
      return false;
    }
    final indent = match.group(1) ?? '';
    final newText = text.replaceRange(lineStart, cursor, indent);
    _applyText(newText, lineStart + indent.length);
    return true;
  }

  TextSelection _styleTargetSelection() {
    final current = _bodyController.selection;
    if (current.isValid && !current.isCollapsed) {
      return current;
    }
    final last = _lastSelectedBodyRange;
    if (last != null &&
        last.isValid &&
        !last.isCollapsed &&
        _lastSelectedBodySnapshot == _bodyController.text &&
        last.start >= 0 &&
        last.end <= _bodyController.text.length) {
      return last;
    }
    return current;
  }

  Future<void> _openFontSizeSheet() async {
    final targetSelection = _styleTargetSelection();
    final result = await showModalBottomSheet<double>(
      context: context,
      showDragHandle: true,
      builder: (context) => _FontSizeSheet(
        currentFontSize: _fontSize,
        minFontSize: _minFontSize,
        maxFontSize: _maxFontSize,
      ),
    );
    if (result == null) {
      return;
    }
    _applyFontSize(result, targetSelection: targetSelection);
  }

  Future<void> _openTextColorSheet() async {
    final targetSelection = _styleTargetSelection();
    final color = await _openColorSheet(
      title: '텍스트 색상',
      selectedColor: _textColor,
      colors: _textColors,
    );
    if (color == null) {
      return;
    }
    _applyColor(color, isBackground: false, targetSelection: targetSelection);
  }

  Future<void> _openBackgroundColorSheet() async {
    final targetSelection = _styleTargetSelection();
    final color = await _openColorSheet(
      title: '배경 색상',
      selectedColor: _backgroundColor,
      colors: _backgroundColors,
    );
    if (color == null) {
      return;
    }
    _applyColor(color, isBackground: true, targetSelection: targetSelection);
  }

  Future<Color?> _openColorSheet({
    required String title,
    required Color selectedColor,
    required List<Color> colors,
  }) {
    return showModalBottomSheet<Color>(
      context: context,
      showDragHandle: true,
      builder: (context) => _ColorPaletteSheet(
        title: title,
        selectedColor: selectedColor,
        recentColors: _recentColors,
        colors: colors,
      ),
    );
  }

  void _wrapSelection(String marker) {
    final text = _bodyController.text;
    final selection = _styleTargetSelection();
    final start = selection.start < 0 ? text.length : selection.start;
    final end = selection.end < 0 ? text.length : selection.end;
    final selected = text.substring(start, end);
    final newText = text.replaceRange(start, end, '$marker$selected$marker');
    final cursor =
        selected.isEmpty ? start + marker.length : end + marker.length * 2;
    _applyText(newText, cursor);
  }

  void _applyFontSize(double fontSize, {TextSelection? targetSelection}) {
    final size = fontSize.round().clamp(
          _minFontSize.round(),
          _maxFontSize.round(),
        );
    setState(() => _fontSize = size.toDouble());
    _applyScopedMarker('[fs=$size]', '[fs=]', targetSelection: targetSelection);
  }

  void _applyColor(
    Color color, {
    required bool isBackground,
    TextSelection? targetSelection,
  }) {
    setState(() {
      if (isBackground) {
        _backgroundColor = color;
      } else {
        _textColor = color;
      }
      if (color != Colors.transparent) {
        _recentColors.remove(color);
        _recentColors.insert(0, color);
        if (_recentColors.length > 5) {
          _recentColors.removeRange(5, _recentColors.length);
        }
      }
    });
    final markerType = isBackground ? 'bg' : 'fg';
    final value = color == Colors.transparent ? '' : _hexColor(color);
    _applyScopedMarker(
      '[$markerType=$value]',
      '[$markerType=]',
      targetSelection: targetSelection,
    );
  }

  void _applyScopedMarker(
    String openMarker,
    String closeMarker, {
    TextSelection? targetSelection,
  }) {
    final text = _bodyController.text;
    final selection = targetSelection ?? _bodyController.selection;
    final start = selection.start < 0 ? text.length : selection.start;
    final end = selection.end < 0 ? start : selection.end;
    final edit = QtNoteRichTextMarkup.applyScopedMarker(
      text: text,
      start: start,
      end: end,
      openMarker: openMarker,
      closeMarker: closeMarker,
    );
    _applyText(edit.text, edit.cursor);
  }

  String _hexColor(Color color) {
    final value = color.toARGB32() & 0xFFFFFF;
    return '#${value.toRadixString(16).padLeft(6, '0').toUpperCase()}';
  }

  void _prefixCurrentLine(String prefix) {
    final text = _bodyController.text;
    final selection = _bodyController.selection;
    final pos = selection.start < 0 ? text.length : selection.start;
    final lineStart = text.lastIndexOf('\n', pos - 1) + 1;
    final newText = text.replaceRange(lineStart, lineStart, prefix);
    _applyText(newText, pos + prefix.length);
  }

  void _insertAtCursor(String value) {
    final text = _bodyController.text;
    final selection = _bodyController.selection;
    final pos = selection.start < 0 ? text.length : selection.start;
    _applyText(text.replaceRange(pos, pos, value), pos + value.length);
  }

  void _applyText(String text, int cursor) {
    final nextCursor = cursor.clamp(0, text.length);
    _applyingAutoPrefix = true;
    _bodyController.value = TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: nextCursor),
    );
    _lastBody = text;
    _lastSelectedBodyRange = null;
    _lastSelectedBodySnapshot = null;
    _applyingAutoPrefix = false;
    _updateMentionQuery(text, nextCursor);
  }

  Future<void> _insertMentionVerse(
    BibleBook book,
    _MentionVerseRange mentionRange,
  ) async {
    try {
      final range = await ref.read(bibleRepositoryProvider).getVerses(
            bookCode: book.code,
            chapter: mentionRange.chapter,
            verseFrom: mentionRange.verseFrom,
            verseTo: mentionRange.verseTo,
          );
      final lines = range.verses.map((verse) {
        final text = verse.koreanText?.trim();
        return '${book.koreanName} ${verse.chapterNo}:${verse.verseNo} ${text ?? ''}';
      }).join('\n');
      _replaceMentionWith(lines);
      // 삽입한 절의 id를 부모에 알려 note_verses(verseIds)로 저장하게 한다(§6.4.1).
      widget.onVerseInserted?.call(range.verses.map((verse) => verse.id).toList());
    } catch (_) {
      if (!mounted) return;
      _showMessage('구절을 불러오지 못했습니다');
    }
  }

  void _replaceMentionWith(String value) {
    final text = _bodyController.text;
    final cursor = _bodyController.selection.start;
    final before = cursor < 0 ? text.length : cursor;
    final at = text.substring(0, before).lastIndexOf('@');
    final start = at < 0 ? before : at;
    final newText = text.replaceRange(start, before, value);
    _applyText(newText, start + value.length);
    setState(() => _mentionQuery = null);
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final toolbar = QtNoteFormatToolbar(
      fontSize: _fontSize,
      textColor: _textColor,
      backgroundColor: _backgroundColor,
      onFontSize: _openFontSizeSheet,
      onBold: () => _wrapSelection('**'),
      onTextColor: _openTextColorSheet,
      onBackgroundColor: _openBackgroundColorSheet,
      onIndent: () => _prefixCurrentLine('    '),
      onBullet: () => _prefixCurrentLine('• '),
      onParenNumber: () => _prefixCurrentLine('(1) '),
      onPlainNumber: () => _prefixCurrentLine('1) '),
      onVerseMention: () => _insertAtCursor('@'),
    );

    return Column(
      children: [
        if (widget.header != null) ...[
          widget.header!,
          const SizedBox(height: 8),
        ],
        if (widget.toolbarPlacement == NoteRichTextToolbarPlacement.top) ...[
          toolbar,
          const SizedBox(height: 8),
        ],
        if (_mentionQuery != null)
          _MentionSuggestions(
            query: _mentionQuery!,
            booksFuture: _booksFuture!,
            onInsertVerse: _insertMentionVerse,
          ),
        Expanded(
          child: Scrollbar(
            key: widget.bodyScrollKey,
            controller: _editorScrollController,
            thumbVisibility: true,
            child: TextField(
              key: widget.bodyFieldKey,
              controller: _bodyController,
              focusNode: _bodyFocusNode,
              scrollController: _editorScrollController,
              hintLocales: const [Locale('ko', 'KR')],
              enableSuggestions: false,
              autocorrect: false,
              enableIMEPersonalizedLearning: false,
              smartDashesType: SmartDashesType.disabled,
              smartQuotesType: SmartQuotesType.disabled,
              decoration: InputDecoration(
                labelText: widget.bodyLabel,
                alignLabelWithHint: true,
                border: const OutlineInputBorder(),
              ),
              style: TextStyle(
                fontSize: widget.controller.emojiFontSize,
                height: 1.55,
              ),
              maxLines: null,
              expands: true,
              textAlignVertical: TextAlignVertical.top,
              keyboardType: TextInputType.multiline,
            ),
          ),
        ),
        if (widget.toolbarPlacement == NoteRichTextToolbarPlacement.bottom)
          toolbar,
      ],
    );
  }
}

class _FontSizeSheet extends StatefulWidget {
  final double currentFontSize;
  final double minFontSize;
  final double maxFontSize;

  const _FontSizeSheet({
    required this.currentFontSize,
    required this.minFontSize,
    required this.maxFontSize,
  });

  @override
  State<_FontSizeSheet> createState() => _FontSizeSheetState();
}

class _FontSizeSheetState extends State<_FontSizeSheet> {
  late double _value;

  @override
  void initState() {
    super.initState();
    _value = widget.currentFontSize;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('글씨 크기', style: theme.textTheme.titleMedium),
                const Spacer(),
                Text('${_value.round()}'),
              ],
            ),
            Slider(
              key: const ValueKey('note-font-size-slider'),
              min: widget.minFontSize,
              max: widget.maxFontSize,
              divisions: (widget.maxFontSize - widget.minFontSize).round(),
              value: _value.clamp(widget.minFontSize, widget.maxFontSize),
              label: '${_value.round()}',
              onChanged: (value) => setState(() => _value = value),
            ),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => Navigator.of(context).pop(_value),
                child: const Text('적용'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ColorPaletteSheet extends StatelessWidget {
  final String title;
  final Color selectedColor;
  final List<Color> recentColors;
  final List<Color> colors;

  const _ColorPaletteSheet({
    required this.title,
    required this.selectedColor,
    required this.recentColors,
    required this.colors,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: theme.textTheme.titleMedium),
            const SizedBox(height: 14),
            if (recentColors.isNotEmpty) ...[
              Text('최근 사용', style: theme.textTheme.labelMedium),
              const SizedBox(height: 8),
              _ColorGrid(
                colors: recentColors,
                selectedColor: selectedColor,
                onSelected: (color) => Navigator.of(context).pop(color),
              ),
              const SizedBox(height: 14),
            ],
            Text(title, style: theme.textTheme.labelMedium),
            const SizedBox(height: 8),
            _ColorGrid(
              colors: colors,
              selectedColor: selectedColor,
              onSelected: (color) => Navigator.of(context).pop(color),
            ),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed: () => Navigator.of(context).pop(Colors.transparent),
              icon: const Icon(Icons.format_color_reset),
              label: const Text('색상 해제'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ColorGrid extends StatelessWidget {
  final List<Color> colors;
  final Color selectedColor;
  final ValueChanged<Color> onSelected;

  const _ColorGrid({
    required this.colors,
    required this.selectedColor,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: [
        for (final color in colors)
          InkWell(
            key: ValueKey('note-color-swatch-${color.toARGB32()}'),
            borderRadius: BorderRadius.circular(8),
            onTap: () => onSelected(color),
            child: Container(
              width: 34,
              height: 34,
              decoration: BoxDecoration(
                color: color,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(
                  color: selectedColor == color
                      ? Theme.of(context).colorScheme.primary
                      : const Color(0xFFCBD5E1),
                  width: selectedColor == color ? 2 : 1,
                ),
              ),
              child: selectedColor == color
                  ? const Icon(Icons.check, size: 18)
                  : null,
            ),
          ),
      ],
    );
  }
}

class _MentionSuggestions extends ConsumerStatefulWidget {
  final String query;
  final Future<List<BibleBook>> booksFuture;
  final void Function(BibleBook book, _MentionVerseRange range) onInsertVerse;

  const _MentionSuggestions({
    required this.query,
    required this.booksFuture,
    required this.onInsertVerse,
  });

  @override
  ConsumerState<_MentionSuggestions> createState() =>
      _MentionSuggestionsState();
}

class _MentionSuggestionsState extends ConsumerState<_MentionSuggestions> {
  static const _defaultVerseCount = 1;

  BibleBook? _selectedBook;
  int _selectedChapter = 1;
  int _verseFrom = 1;
  int _verseTo = 1;
  int _verseCount = _defaultVerseCount;
  int _chapterRequestId = 0;
  bool _isLoadingChapter = false;
  bool _chapterLoadFailed = false;

  @override
  void didUpdateWidget(covariant _MentionSuggestions oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.query != widget.query) {
      _selectedBook = null;
      _selectedChapter = 1;
      _verseFrom = 1;
      _verseTo = 1;
      _verseCount = _defaultVerseCount;
      _chapterLoadFailed = false;
    }
  }

  Future<void> _selectBook(BibleBook book) async {
    final chapterCount = _chapterCountFor(book);
    setState(() {
      _selectedBook = book;
      _selectedChapter = _selectedChapter.clamp(1, chapterCount);
      _verseFrom = 1;
      _verseTo = 1;
      _verseCount = _defaultVerseCount;
      _chapterLoadFailed = false;
    });
    await _loadChapterVerseCount(book.code, _selectedChapter);
  }

  Future<void> _selectChapter(int chapter) async {
    final book = _selectedBook;
    if (book == null) {
      return;
    }
    setState(() {
      _selectedChapter = chapter;
      _verseFrom = 1;
      _verseTo = 1;
      _verseCount = _defaultVerseCount;
      _chapterLoadFailed = false;
    });
    await _loadChapterVerseCount(book.code, chapter);
  }

  Future<void> _loadChapterVerseCount(String bookCode, int chapter) async {
    final requestId = ++_chapterRequestId;
    setState(() {
      _isLoadingChapter = true;
      _chapterLoadFailed = false;
    });
    try {
      final chapterRange =
          await ref.read(bibleRepositoryProvider).getChapterVerses(
                bookCode: bookCode,
                chapter: chapter,
              );
      if (!mounted || requestId != _chapterRequestId) {
        return;
      }
      final count = chapterRange.verses.length;
      setState(() {
        _verseCount = count < 1 ? _defaultVerseCount : count;
        _verseFrom = _verseFrom.clamp(1, _verseCount);
        _verseTo = _verseTo.clamp(_verseFrom, _verseCount);
        _isLoadingChapter = false;
        _chapterLoadFailed = false;
      });
    } catch (_) {
      if (!mounted || requestId != _chapterRequestId) {
        return;
      }
      setState(() {
        _verseCount = _defaultVerseCount;
        _verseFrom = 1;
        _verseTo = 1;
        _isLoadingChapter = false;
        _chapterLoadFailed = true;
      });
    }
  }

  int _chapterCountFor(BibleBook book) {
    return bibleChapterCountForDisplayOrder(book.displayOrder);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return FutureBuilder<List<BibleBook>>(
      future: widget.booksFuture,
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const LinearProgressIndicator();
        }
        final parsedRange = _MentionVerseRange.tryParse(widget.query);
        final bookQuery = parsedRange?.bookQuery ?? widget.query.trim();
        final normalized = bookQuery.toLowerCase();
        final books = snapshot.data!
            .where((book) => _matchesBook(book, bookQuery, normalized))
            .take(8)
            .toList();
        if (books.isEmpty) return const SizedBox.shrink();

        return Container(
          key: const ValueKey('qt-note-mention-suggestions'),
          margin: const EdgeInsets.only(bottom: 8),
          constraints: const BoxConstraints(maxHeight: 112),
          decoration: BoxDecoration(
            border: Border.all(color: theme.colorScheme.outlineVariant),
            borderRadius: BorderRadius.circular(8),
            color: theme.colorScheme.surface,
          ),
          child: Material(
            color: Colors.transparent,
            borderRadius: BorderRadius.circular(8),
            child: _selectedBook == null || parsedRange != null
                ? ListView.separated(
                    shrinkWrap: true,
                    itemCount: books.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final book = books[index];
                      if (parsedRange != null) {
                        return ListTile(
                          dense: true,
                          leading: const Icon(Icons.menu_book_outlined),
                          title: Text(
                            '${book.koreanName} ${parsedRange.displayText} 삽입',
                          ),
                          onTap: () => widget.onInsertVerse(book, parsedRange),
                        );
                      }
                      return ListTile(
                        dense: true,
                        leading: const Icon(Icons.menu_book_outlined),
                        title: Text(book.koreanName),
                        subtitle: Text(book.englishName),
                        onTap: () => _selectBook(book),
                      );
                    },
                  )
                : _MentionRangePicker(
                    book: _selectedBook!,
                    selectedChapter: _selectedChapter,
                    chapterCount: _chapterCountFor(_selectedBook!),
                    verseFrom: _verseFrom,
                    verseTo: _verseTo,
                    verseCount: _verseCount,
                    isLoadingChapter: _isLoadingChapter,
                    isChapterLoadFailed: _chapterLoadFailed,
                    onChapterChanged: _selectChapter,
                    onVerseFromChanged: (verse) {
                      setState(() {
                        _verseFrom = verse;
                        if (_verseTo < verse) {
                          _verseTo = verse;
                        }
                      });
                    },
                    onVerseToChanged: (verse) {
                      setState(() => _verseTo = verse);
                    },
                    onInsert: () => widget.onInsertVerse(
                      _selectedBook!,
                      _MentionVerseRange(
                        bookQuery: _selectedBook!.koreanName,
                        chapter: _selectedChapter,
                        verseFrom: _verseFrom,
                        verseTo: _verseTo,
                      ),
                    ),
                  ),
          ),
        );
      },
    );
  }

  bool _matchesBook(BibleBook book, String bookQuery, String normalized) {
    if (bookQuery.isEmpty) {
      return true;
    }
    return book.koreanName.contains(bookQuery) ||
        book.englishName.toLowerCase().contains(normalized) ||
        book.code.toLowerCase().contains(normalized);
  }
}

class _MentionRangePicker extends StatelessWidget {
  final BibleBook book;
  final int selectedChapter;
  final int chapterCount;
  final int verseFrom;
  final int verseTo;
  final int verseCount;
  final bool isLoadingChapter;
  final bool isChapterLoadFailed;
  final ValueChanged<int> onChapterChanged;
  final ValueChanged<int> onVerseFromChanged;
  final ValueChanged<int> onVerseToChanged;
  final VoidCallback onInsert;

  const _MentionRangePicker({
    required this.book,
    required this.selectedChapter,
    required this.chapterCount,
    required this.verseFrom,
    required this.verseTo,
    required this.verseCount,
    required this.isLoadingChapter,
    required this.isChapterLoadFailed,
    required this.onChapterChanged,
    required this.onVerseFromChanged,
    required this.onVerseToChanged,
    required this.onInsert,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final safeChapterCount = chapterCount < 1 ? 1 : chapterCount;
    final safeVerseCount = verseCount < 1 ? 1 : verseCount;
    final endVerseItems = [
      for (var verse = verseFrom; verse <= safeVerseCount; verse++) verse,
    ];
    final displayText = verseFrom == verseTo
        ? '${book.koreanName} $selectedChapter:$verseFrom'
        : '${book.koreanName} $selectedChapter:$verseFrom-$verseTo';

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 6, 12, 6),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            height: 50,
            child: Row(
              children: [
                Expanded(
                  child: _MentionPickerColumn(
                    key: const Key('qt-note-mention-chapter-picker'),
                    label: '장',
                    selectedIndex: selectedChapter - 1,
                    items: [
                      for (var chapter = 1;
                          chapter <= safeChapterCount;
                          chapter++)
                        '$chapter'
                    ],
                    onSelectedIndexChanged: (index) =>
                        onChapterChanged(index + 1),
                  ),
                ),
                Expanded(
                  child: _MentionPickerColumn(
                    key: const Key('qt-note-mention-verse-from-picker'),
                    label: '시작절',
                    selectedIndex: verseFrom - 1,
                    items: [
                      for (var verse = 1; verse <= safeVerseCount; verse++)
                        '$verse'
                    ],
                    onSelectedIndexChanged: (index) =>
                        onVerseFromChanged(index + 1),
                  ),
                ),
                Expanded(
                  child: _MentionPickerColumn(
                    key: const Key('qt-note-mention-verse-to-picker'),
                    label: '끝절',
                    selectedIndex: endVerseItems.indexOf(verseTo).clamp(
                          0,
                          endVerseItems.length - 1,
                        ),
                    items: [for (final verse in endVerseItems) '$verse'],
                    onSelectedIndexChanged: (index) =>
                        onVerseToChanged(endVerseItems[index]),
                  ),
                ),
              ],
            ),
          ),
          Row(
            children: [
              Expanded(
                child: Text(
                  isLoadingChapter
                      ? '절 목록을 맞추는 중입니다.'
                      : isChapterLoadFailed
                          ? '절 목록을 불러오지 못했습니다.'
                          : displayText,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelLarge,
                ),
              ),
              const SizedBox(width: 10),
              FilledButton.icon(
                onPressed:
                    (isLoadingChapter || isChapterLoadFailed) ? null : onInsert,
                style: FilledButton.styleFrom(
                  minimumSize: const Size(0, 32),
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  visualDensity: VisualDensity.compact,
                ),
                icon: const Icon(Icons.add),
                label: Text(
                  '$displayText 삽입',
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _MentionPickerColumn extends StatelessWidget {
  final String label;
  final List<String> items;
  final int selectedIndex;
  final ValueChanged<int> onSelectedIndexChanged;

  const _MentionPickerColumn({
    super.key,
    required this.label,
    required this.items,
    required this.selectedIndex,
    required this.onSelectedIndexChanged,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      children: [
        Text(
          label,
          style: theme.textTheme.labelMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
            fontSize: 10,
          ),
        ),
        const SizedBox(height: 1),
        Expanded(
          child: Stack(
            alignment: Alignment.center,
            children: [
              Container(
                height: 24,
                decoration: BoxDecoration(
                  color: theme.colorScheme.primaryContainer.withValues(
                    alpha: 0.45,
                  ),
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              ListWheelScrollView.useDelegate(
                controller: FixedExtentScrollController(
                  initialItem: selectedIndex.clamp(0, items.length - 1),
                ),
                itemExtent: 24,
                diameterRatio: 1.35,
                physics: const FixedExtentScrollPhysics(),
                overAndUnderCenterOpacity: 0.38,
                onSelectedItemChanged: onSelectedIndexChanged,
                childDelegate: ListWheelChildBuilderDelegate(
                  childCount: items.length,
                  builder: (context, index) {
                    return Center(
                      child: Text(
                        items[index],
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        textAlign: TextAlign.center,
                        style: theme.textTheme.titleSmall?.copyWith(
                          fontWeight: index == selectedIndex
                              ? FontWeight.w700
                              : FontWeight.w500,
                        ),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _MentionVerseRange {
  final String bookQuery;
  final int chapter;
  final int verseFrom;
  final int verseTo;

  const _MentionVerseRange({
    required this.bookQuery,
    required this.chapter,
    required this.verseFrom,
    required this.verseTo,
  });

  String get displayText {
    if (verseFrom == verseTo) {
      return '$chapter:$verseFrom';
    }
    return '$chapter:$verseFrom-$verseTo';
  }

  static _MentionVerseRange? tryParse(String query) {
    final match =
        RegExp(r'^(.+?)\s+(\d+):(\d+)(?:[-~](\d+))?$').firstMatch(query.trim());
    if (match == null) {
      return null;
    }

    final chapter = int.tryParse(match.group(2)!);
    final verseFrom = int.tryParse(match.group(3)!);
    final verseTo = int.tryParse(match.group(4) ?? match.group(3)!);
    if (chapter == null ||
        verseFrom == null ||
        verseTo == null ||
        chapter < 1 ||
        verseFrom < 1 ||
        verseTo < verseFrom) {
      return null;
    }

    return _MentionVerseRange(
      bookQuery: match.group(1)!.trim(),
      chapter: chapter,
      verseFrom: verseFrom,
      verseTo: verseTo,
    );
  }
}

const _textColors = [
  Color(0xFF111827),
  Color(0xFF15803D),
  Color(0xFF7C3AED),
  Color(0xFF2563EB),
  Color(0xFF92400E),
  Color(0xFFEA580C),
  Color(0xFFD97706),
  Color(0xFF16A34A),
  Color(0xFF0EA5E9),
  Color(0xFF9333EA),
  Color(0xFFDB2777),
  Color(0xFFDC2626),
];

const _backgroundColors = [
  Colors.transparent,
  Color(0xFFF1F5F9),
  Color(0xFFE0F2FE),
  Color(0xFFFFEDD5),
  Color(0xFFFEF3C7),
  Color(0xFFDCFCE7),
  Color(0xFFDBEAFE),
  Color(0xFFEDE9FE),
  Color(0xFFFCE7F3),
  Color(0xFFFEE2E2),
];
