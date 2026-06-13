import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../bible/models/bible_chapter_counts.dart';
import '../../bible/models/bible_models.dart';
import '../../bible/providers/bible_providers.dart';
import '../models/note_markup_quill_codec.dart';
import 'qt_note_format_toolbar.dart';

/// 노트 본문 컨트롤러 — 내부는 flutter_quill의 [QuillController]지만, 저장 형식은
/// 기존 "마커 평문"(`**`·`//`·`__`·`~~`·`[fs/fg/bg=]`)을 그대로 유지한다.
///
/// 부모 화면은 예전처럼 `controller.text`로 마커 평문을 읽고 쓰며(저장·불러오기),
/// 편집은 Quill이 네이티브로 처리한다. 변환은 [NoteMarkupQuillCodec]가 담당한다.
class NoteRichBodyController {
  /// 이모지 표시용 기준 폰트(에디터 기본 글자 크기 초기값으로 쓴다).
  final double emojiFontSize;
  final QuillController quill;

  NoteRichBodyController({this.emojiFontSize = 16, String text = ''})
      : quill = QuillController(
          document: _docFromMarkers(text),
          selection: const TextSelection.collapsed(offset: 0),
        );

  static Document _docFromMarkers(String markers) {
    if (markers.isEmpty) return Document();
    return Document.fromDelta(NoteMarkupQuillCodec.markersToDelta(markers));
  }

  /// 저장 형식(마커 평문) 읽기.
  String get text =>
      NoteMarkupQuillCodec.deltaToMarkers(quill.document.toDelta());

  /// 저장 형식(마커 평문)으로 본문 교체(불러오기).
  set text(String markers) {
    quill.document = _docFromMarkers(markers);
    quill.updateSelection(
      const TextSelection.collapsed(offset: 0),
      ChangeSource.local,
    );
  }

  void dispose() => quill.dispose();
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

  /// 본문 입력 영역 키(테스트 호환용, 선택).
  final Key? bodyFieldKey;

  /// 본문 스크롤 영역 키(테스트 호환용, 선택).
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
  // 핀치 줌으로 조절하는 에디터 기본 글자 크기 허용 범위.
  static const double _minEditorFontSize = 10;
  static const double _maxEditorFontSize = 48;
  static const Color _defaultTextColor = Color(0xFF111827);
  static const Color _defaultBackgroundColor = Colors.transparent;

  static const String _defaultFontFamilyLabel = '기본서체';
  static const List<String> _fontFamilyOptions = <String>[
    _defaultFontFamilyLabel,
    '나눔고딕',
    '나눔명조',
    '손글씨',
  ];

  final _editorScrollController = ScrollController();
  final _bodyFocusNode = FocusNode();

  Future<List<BibleBook>>? _booksFuture;
  String? _mentionQuery;

  // 툴바의 글씨 크기 숫자(마지막 선택/핀치 값).
  double _fontSize = 16;
  // 에디터 기본 표시 글자 크기(핀치 줌으로 조절).
  double _editorFontSize = 16;
  String _fontFamilyLabel = _defaultFontFamilyLabel;

  // 두 손가락 핀치 추적.
  final Map<int, Offset> _activePointers = <int, Offset>{};
  double? _pinchStartDistance;
  double _pinchStartFontSize = 16;

  Color _textColor = _defaultTextColor;
  Color _backgroundColor = _defaultBackgroundColor;
  final List<Color> _recentColors = <Color>[
    Color(0xFFDC2626),
    Color(0xFF15803D),
    Color(0xFF7C3AED),
    Color(0xFF2563EB),
  ];

  QuillController get _quill => widget.controller.quill;

  @override
  void initState() {
    super.initState();
    _editorFontSize = widget.controller.emojiFontSize;
    // 커서 이동·텍스트 변경마다 @멘션 감지와 툴바 활성 표시를 갱신한다.
    _quill.addListener(_onQuillChanged);
  }

  @override
  void dispose() {
    _quill.removeListener(_onQuillChanged);
    _bodyFocusNode.dispose();
    _editorScrollController.dispose();
    super.dispose();
  }

  void _onQuillChanged() {
    final text = _quill.document.toPlainText();
    final selection = _quill.selection;
    final cursor = selection.isValid ? selection.baseOffset : text.length;
    final nextQuery = _computeMentionQuery(text, cursor);
    if (nextQuery != null) {
      _booksFuture ??= ref.read(bibleRepositoryProvider).getBooks();
    }
    if (!mounted) return;
    setState(() => _mentionQuery = nextQuery);
  }

  /// 커서 앞의 `@질의`를 찾아 멘션 질의를 만든다(공백/줄바꿈/길이 제한).
  String? _computeMentionQuery(String text, int cursor) {
    if (cursor < 0 || cursor > text.length) return null;
    final before = text.substring(0, cursor);
    final at = before.lastIndexOf('@');
    if (at < 0) return null;
    final token = before.substring(at);
    if (token.contains('\n') || token.length > 32) return null;
    return before.substring(at + 1);
  }

  /// 서식 적용 후 입력 포커스를 유지한다.
  void _refocus() {
    if (!_bodyFocusNode.hasFocus) _bodyFocusNode.requestFocus();
  }

  /// 인라인 토글 서식(굵게/기울임/밑줄/취소선)을 켜고 끈다.
  void _toggleInline(Attribute<dynamic> attribute) {
    final isOn =
        _quill.getSelectionStyle().attributes.containsKey(attribute.key);
    _quill.formatSelection(isOn ? Attribute.clone(attribute, null) : attribute);
    _refocus();
  }

  Future<void> _openFontSizeSheet() async {
    final result = await showModalBottomSheet<double>(
      context: context,
      showDragHandle: true,
      builder: (context) => _FontSizeSheet(
        currentFontSize: _fontSize,
        minFontSize: _minFontSize,
        maxFontSize: _maxFontSize,
      ),
    );
    if (result == null) return;
    _applyFontSize(result);
  }

  void _applyFontSize(double fontSize) {
    final size = fontSize.round().clamp(
          _minFontSize.round(),
          _maxFontSize.round(),
        );
    setState(() => _fontSize = size.toDouble());
    _quill.formatSelection(SizeAttribute('$size'));
    _refocus();
  }

  Future<void> _openTextColorSheet() async {
    final color = await _openColorSheet(
      title: '텍스트 색상',
      selectedColor: _textColor,
      colors: _textColors,
    );
    if (color == null) return;
    _applyColor(color, isBackground: false);
  }

  Future<void> _openBackgroundColorSheet() async {
    final color = await _openColorSheet(
      title: '배경 색상',
      selectedColor: _backgroundColor,
      colors: _backgroundColors,
    );
    if (color == null) return;
    _applyColor(color, isBackground: true);
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

  void _applyColor(Color color, {required bool isBackground}) {
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
    final clear = color == Colors.transparent;
    final Attribute<dynamic> attribute;
    if (isBackground) {
      attribute = clear
          ? Attribute.clone(Attribute.background, null)
          : BackgroundAttribute(_hexColor(color));
    } else {
      attribute = clear
          ? Attribute.clone(Attribute.color, null)
          : ColorAttribute(_hexColor(color));
    }
    _quill.formatSelection(attribute);
    _refocus();
  }

  String _hexColor(Color color) {
    final value = color.toARGB32() & 0xFFFFFF;
    return '#${value.toRadixString(16).padLeft(6, '0').toUpperCase()}';
  }

  /// 현재 줄 맨 앞에 글머리표/번호/들여쓰기 접두어를 넣는다(저장 형식상 일반 글자).
  void _insertLinePrefix(String prefix) {
    final text = _quill.document.toPlainText();
    final selection = _quill.selection;
    final pos = (selection.isValid ? selection.baseOffset : text.length)
        .clamp(0, text.length);
    final lineStart = pos == 0 ? 0 : text.lastIndexOf('\n', pos - 1) + 1;
    _quill.replaceText(
      lineStart,
      0,
      prefix,
      TextSelection.collapsed(offset: pos + prefix.length),
    );
    _refocus();
  }

  /// 커서 위치에 문자열을 삽입한다(@ 멘션 시작 등).
  void _insertAtCursor(String value) {
    final selection = _quill.selection;
    final docLength = _quill.document.length;
    final pos = (selection.isValid ? selection.baseOffset : docLength - 1)
        .clamp(0, docLength - 1);
    _quill.replaceText(
      pos,
      0,
      value,
      TextSelection.collapsed(offset: pos + value.length),
    );
    _refocus();
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
      widget.onVerseInserted
          ?.call(range.verses.map((verse) => verse.id).toList());
    } catch (_) {
      if (!mounted) return;
      _showMessage('구절을 불러오지 못했습니다');
    }
  }

  void _replaceMentionWith(String value) {
    final text = _quill.document.toPlainText();
    final selection = _quill.selection;
    final cursor = selection.isValid ? selection.baseOffset : text.length;
    final before = cursor < 0 ? text.length : cursor;
    final at = text.substring(0, before).lastIndexOf('@');
    final start = at < 0 ? before : at;
    _quill.replaceText(
      start,
      before - start,
      value,
      TextSelection.collapsed(offset: start + value.length),
    );
    setState(() => _mentionQuery = null);
    _refocus();
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
      ));
  }

  // ── 두 손가락 핀치로 에디터 기본 글자 크기 조절 ───────────────────────────
  void _onEditorPointerDown(PointerDownEvent event) {
    _activePointers[event.pointer] = event.position;
    if (_activePointers.length == 2) {
      final points = _activePointers.values.toList();
      _pinchStartDistance = (points[0] - points[1]).distance;
      _pinchStartFontSize = _editorFontSize;
    }
  }

  void _onEditorPointerMove(PointerMoveEvent event) {
    if (!_activePointers.containsKey(event.pointer)) return;
    _activePointers[event.pointer] = event.position;
    final startDistance = _pinchStartDistance;
    if (_activePointers.length != 2 ||
        startDistance == null ||
        startDistance <= 0) {
      return;
    }
    final points = _activePointers.values.toList();
    final distance = (points[0] - points[1]).distance;
    final next = (_pinchStartFontSize * (distance / startDistance))
        .clamp(_minEditorFontSize, _maxEditorFontSize);
    if (next != _editorFontSize) {
      setState(() {
        _editorFontSize = next.toDouble();
        _fontSize = next.toDouble();
      });
    }
  }

  void _onEditorPointerUp(int pointer) {
    _activePointers.remove(pointer);
    _pinchStartDistance = null;
  }

  /// 에디터 기본 단락 스타일(글꼴/크기). 핀치·서체 선택을 반영한다.
  DefaultStyles _quillStyles() {
    final base = TextStyle(
      fontSize: _editorFontSize,
      height: 1.55,
      leadingDistribution: TextLeadingDistribution.even,
      color: _defaultTextColor,
    );
    final styled = switch (_fontFamilyLabel) {
      '나눔고딕' => GoogleFonts.nanumGothic(textStyle: base),
      '나눔명조' => GoogleFonts.nanumMyeongjo(textStyle: base),
      '손글씨' => GoogleFonts.gaegu(textStyle: base),
      _ => GoogleFonts.notoSansKr(textStyle: base),
    };
    return DefaultStyles(
      paragraph: DefaultTextBlockStyle(
        styled,
        const HorizontalSpacing(0, 0),
        const VerticalSpacing(6, 0),
        const VerticalSpacing(0, 0),
        null,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final attributes = _quill.getSelectionStyle().attributes;
    final toolbar = QtNoteFormatToolbar(
      fontSize: _fontSize,
      textColor: _textColor,
      backgroundColor: _backgroundColor,
      fontFamilyLabel: _fontFamilyLabel,
      fontFamilyOptions: _fontFamilyOptions,
      onFontFamily: (label) => setState(() => _fontFamilyLabel = label),
      onFontSize: _openFontSizeSheet,
      boldActive: attributes.containsKey(Attribute.bold.key),
      italicActive: attributes.containsKey(Attribute.italic.key),
      underlineActive: attributes.containsKey(Attribute.underline.key),
      strikethroughActive: attributes.containsKey(Attribute.strikeThrough.key),
      onBold: () => _toggleInline(Attribute.bold),
      onItalic: () => _toggleInline(Attribute.italic),
      onUnderline: () => _toggleInline(Attribute.underline),
      onStrikethrough: () => _toggleInline(Attribute.strikeThrough),
      onTextColor: _openTextColorSheet,
      onBackgroundColor: _openBackgroundColorSheet,
      onIndent: () => _insertLinePrefix('    '),
      onBullet: () => _insertLinePrefix('• '),
      onParenNumber: () => _insertLinePrefix('(1) '),
      onPlainNumber: () => _insertLinePrefix('1) '),
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
        if (_mentionQuery != null && _booksFuture != null)
          _MentionSuggestions(
            query: _mentionQuery!,
            booksFuture: _booksFuture!,
            onInsertVerse: _insertMentionVerse,
          ),
        Expanded(
          child: Listener(
            behavior: HitTestBehavior.deferToChild,
            onPointerDown: _onEditorPointerDown,
            onPointerMove: _onEditorPointerMove,
            onPointerUp: (e) => _onEditorPointerUp(e.pointer),
            onPointerCancel: (e) => _onEditorPointerUp(e.pointer),
            child: Container(
              key: widget.bodyScrollKey,
              decoration: BoxDecoration(
                border: Border.all(color: Theme.of(context).colorScheme.outline),
                borderRadius: BorderRadius.circular(4),
              ),
              child: QuillEditor(
                key: widget.bodyFieldKey,
                focusNode: _bodyFocusNode,
                scrollController: _editorScrollController,
                controller: _quill,
                config: QuillEditorConfig(
                  placeholder: widget.bodyLabel,
                  padding: const EdgeInsets.all(12),
                  expands: true,
                  scrollable: true,
                  customStyles: _quillStyles(),
                ),
              ),
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
          // 책 목록·휠 picker가 약 2.5개 보이도록 박스를 키운다.
          constraints: const BoxConstraints(maxHeight: 168),
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
            // 휠을 키워 가운데 선택 숫자 + 위·아래 이전/다음 숫자가 반쯤 보이도록(약 2.5개).
            height: 84,
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
                label: const Text(
                  '문구 삽입',
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
  Color(0xFFFEF08A), // 노랑(형광)
  Color(0xFFBBF7D0), // 초록
  Color(0xFFBFDBFE), // 파랑
  Color(0xFFFBCFE8), // 분홍
  Color(0xFFFED7AA), // 주황
  Color(0xFFDDD6FE), // 보라
  Color(0xFF99F6E4), // 청록
  Color(0xFFFECACA), // 빨강
  Color(0xFFE5E7EB), // 회색
];
