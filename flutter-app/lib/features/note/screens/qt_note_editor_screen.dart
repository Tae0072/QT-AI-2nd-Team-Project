import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';

import '../../bible/models/bible_models.dart';
import '../../bible/providers/bible_providers.dart';
import '../providers/note_providers.dart';
import '../widgets/qt_note_format_toolbar.dart';

class QtNoteEditorArgs {
  final TodayQtPassage passage;

  const QtNoteEditorArgs({required this.passage});
}

class QtNoteEditorScreen extends ConsumerStatefulWidget {
  final QtNoteEditorArgs args;

  const QtNoteEditorScreen({super.key, required this.args});

  @override
  ConsumerState<QtNoteEditorScreen> createState() => _QtNoteEditorScreenState();
}

class _QtNoteEditorScreenState extends ConsumerState<QtNoteEditorScreen> {
  static const double _defaultEmojiFontSize = 16;
  static const double _minFontSize = 10;
  static const double _maxFontSize = 32;

  final _titleController = TextEditingController();
  final _bodyController = _NoteBodyTextEditingController(
    emojiFontSize: _defaultEmojiFontSize,
  );
  final _passageScrollController = ScrollController();
  final _editorScrollController = ScrollController();
  Future<List<BibleBook>>? _booksFuture;
  String? _mentionQuery;
  String _lastBody = '';
  bool _applyingAutoPrefix = false;
  bool _saving = false;
  double _fontSize = 16;

  TodayQtPassage get _passage => widget.args.passage;

  @override
  void initState() {
    super.initState();
    _titleController.text = _passage.title ?? _passage.reference.displayText;
    _bodyController.addListener(_handleBodyChanged);
  }

  @override
  void dispose() {
    _titleController.dispose();
    _bodyController.removeListener(_handleBodyChanged);
    _bodyController.dispose();
    _passageScrollController.dispose();
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
    _updateMentionQuery(text, selection.start);

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

  Future<void> _openFontSizeDialog() async {
    final result = await showDialog<double>(
      context: context,
      builder: (context) => _FontSizeDialog(
        currentFontSize: _fontSize,
        minFontSize: _minFontSize,
        maxFontSize: _maxFontSize,
      ),
    );
    if (result == null) {
      return;
    }
    setState(() => _fontSize = result);
  }

  void _wrapSelection(String marker) {
    final text = _bodyController.text;
    final selection = _bodyController.selection;
    final start = selection.start < 0 ? text.length : selection.start;
    final end = selection.end < 0 ? text.length : selection.end;
    final selected = text.substring(start, end);
    final newText = text.replaceRange(start, end, '$marker$selected$marker');
    final cursor =
        selected.isEmpty ? start + marker.length : end + marker.length * 2;
    _applyText(newText, cursor);
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
    _applyingAutoPrefix = true;
    _bodyController.value = TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: cursor.clamp(0, text.length)),
    );
    _lastBody = text;
    _applyingAutoPrefix = false;
  }

  Future<void> _save(String status) async {
    final body = _bodyController.text.trim();
    if (body.isEmpty) {
      _showMessage('노트 내용을 입력해 주세요');
      return;
    }

    setState(() => _saving = true);
    try {
      await ref.read(noteRepositoryProvider).createQtNote(
            qtPassageId: _passage.qtPassageId,
            title: _titleController.text.trim(),
            body: body,
            status: status,
            verseIds: _passage.verses.map((verse) => verse.id).toList(),
          );
      if (!mounted) return;
      _showMessage(status == 'SAVED' ? '저장되었습니다' : '임시저장되었습니다');
      Navigator.of(context).pop();
    } catch (_) {
      if (!mounted) return;
      setState(() => _saving = false);
      _showMessage('저장에 실패했습니다. 다시 시도해 주세요');
    }
  }

  void _completeMentionBook(BibleBook book) {
    _replaceMentionWith('@${book.koreanName} ');
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
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('QT 노트'),
        centerTitle: true,
      ),
      body: AbsorbPointer(
        absorbing: _saving,
        child: SafeArea(
          child: Column(
            children: [
              Expanded(
                flex: 5,
                child: Scrollbar(
                  key: const ValueKey('qt-note-passage-scroll'),
                  controller: _passageScrollController,
                  thumbVisibility: true,
                  child: ListView(
                    controller: _passageScrollController,
                    padding: const EdgeInsets.fromLTRB(20, 12, 20, 16),
                    children: [
                      Text(
                        _passage.reference.displayText,
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      if ((_passage.title ?? '').isNotEmpty) ...[
                        const SizedBox(height: 6),
                        Text(_passage.title!,
                            style: theme.textTheme.bodyMedium),
                      ],
                      const SizedBox(height: 12),
                      for (final verse in _passage.verses)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '${verse.chapterNo}:${verse.verseNo}',
                                style: theme.textTheme.labelLarge?.copyWith(
                                  color: theme.colorScheme.primary,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              const SizedBox(height: 4),
                              if ((verse.koreanText ?? '').trim().isNotEmpty)
                                Text(
                                  verse.koreanText!.trim(),
                                  style: theme.textTheme.bodyMedium?.copyWith(
                                    height: 1.55,
                                  ),
                                ),
                              if ((verse.englishText ?? '')
                                  .trim()
                                  .isNotEmpty) ...[
                                const SizedBox(height: 4),
                                Text(
                                  verse.englishText!.trim(),
                                  style: theme.textTheme.bodySmall?.copyWith(
                                    color: theme.colorScheme.onSurfaceVariant,
                                    height: 1.45,
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
              const Divider(height: 1),
              Expanded(
                flex: 6,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                  child: Column(
                    children: [
                      TextField(
                        controller: _titleController,
                        hintLocales: const [Locale('ko', 'KR')],
                        enableSuggestions: false,
                        autocorrect: false,
                        enableIMEPersonalizedLearning: false,
                        smartDashesType: SmartDashesType.disabled,
                        smartQuotesType: SmartQuotesType.disabled,
                        decoration: const InputDecoration(
                          labelText: '제목',
                          isDense: true,
                        ),
                      ),
                      QtNoteFormatToolbar(
                        fontSize: _fontSize,
                        onFontSize: _openFontSizeDialog,
                        onBold: () => _wrapSelection('**'),
                        onHighlight: () => _wrapSelection('=='),
                        onIndent: () => _prefixCurrentLine('    '),
                        onBullet: () => _prefixCurrentLine('• '),
                        onParenNumber: () => _prefixCurrentLine('(1) '),
                        onPlainNumber: () => _prefixCurrentLine('1) '),
                        onVerseMention: () => _insertAtCursor('@'),
                      ),
                      if (_mentionQuery != null)
                        _MentionSuggestions(
                          query: _mentionQuery!,
                          booksFuture: _booksFuture!,
                          onSelectBook: _completeMentionBook,
                          onInsertVerse: _insertMentionVerse,
                        ),
                      Expanded(
                        child: Scrollbar(
                          key: const ValueKey('qt-note-editor-scroll'),
                          controller: _editorScrollController,
                          thumbVisibility: true,
                          child: TextField(
                            key: const ValueKey('qt-note-body-input'),
                            controller: _bodyController,
                            scrollController: _editorScrollController,
                            hintLocales: const [Locale('ko', 'KR')],
                            enableSuggestions: false,
                            autocorrect: false,
                            enableIMEPersonalizedLearning: false,
                            smartDashesType: SmartDashesType.disabled,
                            smartQuotesType: SmartQuotesType.disabled,
                            decoration: const InputDecoration(
                              labelText: '노트 작성',
                              alignLabelWithHint: true,
                              border: OutlineInputBorder(),
                            ),
                            style: TextStyle(fontSize: _fontSize, height: 1.55),
                            maxLines: null,
                            expands: true,
                            textAlignVertical: TextAlignVertical.top,
                            keyboardType: TextInputType.multiline,
                          ),
                        ),
                      ),
                      const SizedBox(height: 10),
                      Row(
                        children: [
                          Expanded(
                            child: OutlinedButton(
                              onPressed: _saving ? null : () => _save('DRAFT'),
                              child: const Text('임시저장'),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: FilledButton(
                              onPressed: _saving ? null : () => _save('SAVED'),
                              child: _saving
                                  ? const SizedBox(
                                      width: 18,
                                      height: 18,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                      ),
                                    )
                                  : const Text('저장'),
                            ),
                          ),
                        ],
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
}

class _NoteBodyTextEditingController extends TextEditingController {
  static const _highlightColor = Color(0xFFFFF2A8);

  final double emojiFontSize;

  _NoteBodyTextEditingController({required this.emojiFontSize});

  @override
  TextSpan buildTextSpan({
    required BuildContext context,
    TextStyle? style,
    required bool withComposing,
  }) {
    if (text.isEmpty) {
      return TextSpan(style: style, text: '');
    }

    final children = <TextSpan>[];
    var index = 0;
    while (index < text.length) {
      if (text.startsWith('==', index)) {
        final end = text.indexOf('==', index + 2);
        if (end > index + 2) {
          _addHiddenMarker(children, style, '==');
          _addStyledClusters(
            children,
            text.substring(index + 2, end),
            style?.copyWith(backgroundColor: _highlightColor),
          );
          _addHiddenMarker(children, style, '==');
          index = end + 2;
          continue;
        }
      }

      if (text.startsWith('**', index)) {
        final end = text.indexOf('**', index + 2);
        if (end > index + 2) {
          _addHiddenMarker(children, style, '**');
          _addStyledClusters(
            children,
            text.substring(index + 2, end),
            style?.copyWith(fontWeight: FontWeight.w700),
          );
          _addHiddenMarker(children, style, '**');
          index = end + 2;
          continue;
        }
      }

      final cluster = text.substring(index).characters.first;
      _addStyledClusters(children, cluster, style);
      index += cluster.length;
    }

    return TextSpan(style: style, children: children);
  }

  void _addHiddenMarker(
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

  void _addStyledClusters(
    List<TextSpan> children,
    String value,
    TextStyle? baseStyle,
  ) {
    for (final cluster in value.characters) {
      children.add(TextSpan(
        text: cluster,
        style: _isEmojiCluster(cluster)
            ? baseStyle?.copyWith(fontSize: emojiFontSize)
            : baseStyle,
      ));
    }
  }

  bool _isEmojiCluster(String cluster) {
    return cluster.runes.any((rune) =>
        (rune >= 0x1F300 && rune <= 0x1FAFF) ||
        (rune >= 0x2600 && rune <= 0x27BF));
  }
}

class _FontSizeDialog extends StatefulWidget {
  final double currentFontSize;
  final double minFontSize;
  final double maxFontSize;

  const _FontSizeDialog({
    required this.currentFontSize,
    required this.minFontSize,
    required this.maxFontSize,
  });

  @override
  State<_FontSizeDialog> createState() => _FontSizeDialogState();
}

class _FontSizeDialogState extends State<_FontSizeDialog> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(
      text: widget.currentFontSize.round().toString(),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('글씨 크기'),
      content: TextField(
        key: const ValueKey('note-font-size-input'),
        controller: _controller,
        autofocus: true,
        keyboardType: TextInputType.number,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        decoration: InputDecoration(
          labelText: '크기',
          helperText:
              '${widget.minFontSize.round()}~${widget.maxFontSize.round()} 사이 숫자',
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('취소'),
        ),
        FilledButton(
          onPressed: () {
            final parsed = double.tryParse(_controller.text);
            if (parsed == null) {
              return;
            }
            final clamped = parsed.clamp(
              widget.minFontSize,
              widget.maxFontSize,
            );
            Navigator.of(context).pop(clamped.toDouble());
          },
          child: const Text('적용'),
        ),
      ],
    );
  }
}

class _MentionSuggestions extends StatelessWidget {
  final String query;
  final Future<List<BibleBook>> booksFuture;
  final ValueChanged<BibleBook> onSelectBook;
  final void Function(BibleBook book, _MentionVerseRange range) onInsertVerse;

  const _MentionSuggestions({
    required this.query,
    required this.booksFuture,
    required this.onSelectBook,
    required this.onInsertVerse,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return FutureBuilder<List<BibleBook>>(
      future: booksFuture,
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const LinearProgressIndicator();
        }
        final parsedRange = _MentionVerseRange.tryParse(query);
        final bookQuery = parsedRange?.bookQuery ?? query.trim();
        final normalized = bookQuery.toLowerCase();
        final books = snapshot.data!
            .where((book) => _matchesBook(book, bookQuery, normalized))
            .take(8)
            .toList();
        if (books.isEmpty) return const SizedBox.shrink();

        return Container(
          key: const ValueKey('qt-note-mention-suggestions'),
          margin: const EdgeInsets.only(bottom: 8),
          constraints: const BoxConstraints(maxHeight: 188),
          decoration: BoxDecoration(
            border: Border.all(color: theme.colorScheme.outlineVariant),
            borderRadius: BorderRadius.circular(8),
            color: theme.colorScheme.surface,
          ),
          child: ListView.separated(
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
                  onTap: () => onInsertVerse(book, parsedRange),
                );
              }
              return ListTile(
                dense: true,
                leading: const Icon(Icons.menu_book_outlined),
                title: Text(book.koreanName),
                subtitle: Text(book.englishName),
                onTap: () => onSelectBook(book),
              );
            },
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
