import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../bible/models/bible_models.dart';
import '../models/note_drawing.dart';
import '../providers/note_providers.dart';
import '../widgets/note_rich_text_editor.dart';

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
  final _titleController = TextEditingController();
  final _bodyController = NoteRichBodyController(emojiFontSize: 16);
  // 본문 미리보기 핀치 줌(확대/축소) 변환 컨트롤러.
  final _previewTransform = TransformationController();
  // 본문 미리보기 패널 높이 비율(0~100). 가운데 핸들을 위아래로 끌어 조절한다.
  int _previewFlex = 30;
  bool _saving = false;

  // 페이지 모드(일반/원고)·손그림 — 이 기기에만 로컬 저장한다.
  NotePageMode _pageMode = NotePageMode.plain;
  List<DrawingStroke> _strokes = const <DrawingStroke>[];

  TodayQtPassage get _passage => widget.args.passage;

  // QT 노트는 해당 QT 본문(qtPassageId)을 키로 로컬 저장한다.
  String get _canvasKey => 'qt:${_passage.qtPassageId}';

  void _persistCanvas() {
    final store = ref.read(noteCanvasStoreProvider);
    store.saveMode(_canvasKey, _pageMode);
    store.saveStrokes(_canvasKey, _strokes);
  }

  @override
  void initState() {
    super.initState();
    _titleController.text = _passage.title ?? _passage.reference.displayText;
    // 이미 임시저장한 노트가 있으면 그 내용을 불러와 이어서 수정할 수 있게 한다.
    final draftId = _passage.draftNoteId;
    if (draftId != null) {
      Future.microtask(() => _loadDraft(draftId));
    }
    // 이 QT에 저장해 둔 페이지 모드·손그림(로컬)을 불러온다.
    Future.microtask(_loadCanvas);
  }

  Future<void> _loadCanvas() async {
    final store = ref.read(noteCanvasStoreProvider);
    final mode = await store.loadMode(_canvasKey);
    final strokes = await store.loadStrokes(_canvasKey);
    if (!mounted) return;
    setState(() {
      _pageMode = mode;
      _strokes = strokes;
    });
  }

  Future<void> _loadDraft(int noteId) async {
    try {
      final detail = await ref.read(noteRepositoryProvider).getDetail(noteId);
      if (!mounted) return;
      setState(() {
        final title = detail.title;
        if (title.isNotEmpty) _titleController.text = title;
        _bodyController.text = detail.body ?? '';
      });
    } catch (_) {
      // 초안 로드 실패 시 빈 편집기로 진행한다(저장은 새로 만들기로 처리).
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _bodyController.dispose();
    _previewTransform.dispose();
    super.dispose();
  }

  Future<void> _save(String status) async {
    final body = _bodyController.text.trim();
    if (body.isEmpty) {
      _showMessage('노트 내용을 입력해 주세요');
      return;
    }

    setState(() => _saving = true);
    final repository = ref.read(noteRepositoryProvider);
    final verseIds = _passage.verses.map((verse) => verse.id).toList();
    final draftNoteId = _passage.draftNoteId;
    final title = _titleController.text.trim();
    try {
      if (draftNoteId != null) {
        // 이미 만들어진 노트(임시저장)는 새로 만들지 않고 수정한다 → 본문당 1개 제약(N0002) 회피.
        await repository.update(
          draftNoteId,
          category: 'MEDITATION',
          qtPassageId: _passage.qtPassageId,
          title: title,
          body: body,
          verseIds: verseIds,
          status: status,
        );
      } else {
        await repository.createQtNote(
          qtPassageId: _passage.qtPassageId,
          title: title,
          body: body,
          status: status,
          verseIds: verseIds,
        );
      }
      // 페이지 모드·손그림(로컬)을 이 QT 키로 저장한다.
      _persistCanvas();
      if (!mounted) return;
      _showMessage(status == 'SAVED' ? '저장되었습니다' : '임시저장되었습니다');
      Navigator.of(context).pop();
    } catch (error, stackTrace) {
      _logSaveError(error, stackTrace);
      if (!mounted) return;
      setState(() => _saving = false);
      if (_isAlreadyExistsError(error)) {
        // 본문당 노트 1개 제약(N0002): 이미 저장된 노트가 있으면 기록에서 수정하도록 안내.
        _showMessage('이미 저장한 노트가 있어요. ‘기록’에서 열어 수정해 주세요');
      } else {
        _showMessage('저장에 실패했습니다. 다시 시도해 주세요');
      }
    }
  }

  /// 본문당 노트 1개 제약(409 / N0002) 여부.
  bool _isAlreadyExistsError(Object error) {
    if (error is! DioException) return false;
    if (error.response?.statusCode == 409) return true;
    final data = error.response?.data;
    final root = data is Map ? data : null;
    final apiError = root?['error'];
    final errorBody = apiError is Map ? apiError : root;
    return (errorBody?['code'] as String?) == 'N0002';
  }

  void _logSaveError(Object error, StackTrace stackTrace) {
    if (error is DioException) {
      final response = error.response;
      final data = response?.data;
      final root = data is Map ? data : null;
      final apiError = root?['error'];
      final errorBody = apiError is Map ? apiError : root;
      debugPrint(
        '[QT_NOTE_SAVE_ERROR] '
        'status=${response?.statusCode} '
        'path=${error.requestOptions.path} '
        'code=${errorBody?['code']} '
        'message=${errorBody?['message']} '
        'traceId=${errorBody?['traceId']}',
      );
      return;
    }

    debugPrint('[QT_NOTE_SAVE_ERROR] $error');
    debugPrintStack(stackTrace: stackTrace);
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
      ));
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
                key: const ValueKey('qt-note-passage-panel'),
                flex: _previewFlex,
                child: Stack(
                  children: [
                    // 본문 미리보기: 두 손가락 핀치로 0.5~3배 확대/축소.
                    // boundaryMargin=0 → 콘텐츠 밖으로는 못 나가므로 기본 배율에선
                    // 좌상단에 고정되고, 확대했을 때만 상하좌우 어느 방향이든
                    // 콘텐츠 범위 안에서 이동할 수 있다.
                    LayoutBuilder(
                      builder: (context, constraints) {
                        return InteractiveViewer(
                          transformationController: _previewTransform,
                          minScale: 0.5,
                          maxScale: 3.0,
                          boundaryMargin: EdgeInsets.zero,
                          // constrained:false → 자식이 뷰포트보다 커질 수 있어
                          // 내부 스크롤뷰 없이 InteractiveViewer가 상하·좌우·대각선
                          // 이동과 확대를 모두 처리한다(세로 스크롤 제스처 충돌 제거).
                          constrained: false,
                          child: SizedBox(
                            key: const ValueKey('qt-note-passage-scroll'),
                            width: constraints.maxWidth,
                            child: SelectionArea(
                              child: Padding(
                                padding:
                                    const EdgeInsets.fromLTRB(20, 12, 20, 16),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      _passage.reference.displayText,
                                      style:
                                          theme.textTheme.titleLarge?.copyWith(
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
                                        padding:
                                            const EdgeInsets.only(bottom: 12),
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              '${verse.chapterNo}:${verse.verseNo}',
                                              style: theme.textTheme.labelLarge
                                                  ?.copyWith(
                                                color:
                                                    theme.colorScheme.primary,
                                                fontWeight: FontWeight.w700,
                                              ),
                                            ),
                                            const SizedBox(height: 4),
                                            if ((verse.koreanText ?? '')
                                                .trim()
                                                .isNotEmpty)
                                              Text(
                                                verse.koreanText!.trim(),
                                                style: theme
                                                    .textTheme.bodyMedium
                                                    ?.copyWith(
                                                  height: 1.55,
                                                ),
                                              ),
                                            if ((verse.englishText ?? '')
                                                .trim()
                                                .isNotEmpty) ...[
                                              const SizedBox(height: 4),
                                              Text(
                                                verse.englishText!.trim(),
                                                style: theme.textTheme.bodySmall
                                                    ?.copyWith(
                                                  color: theme.colorScheme
                                                      .onSurfaceVariant,
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
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
              // 드래그 핸들 — 위아래로 끌어 미리보기 영역 높이를 조절한다.
              GestureDetector(
                behavior: HitTestBehavior.opaque,
                onVerticalDragUpdate: (d) => setState(() {
                  _previewFlex =
                      (_previewFlex + d.delta.dy * 0.12).round().clamp(12, 70);
                }),
                child: Container(
                  height: 18,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    border: Border.symmetric(
                      horizontal: BorderSide(
                        color: Theme.of(context).dividerColor,
                        width: 0.5,
                      ),
                    ),
                  ),
                  child: Container(
                    width: 44,
                    height: 5,
                    decoration: BoxDecoration(
                      color: Colors.grey.shade400,
                      borderRadius: BorderRadius.circular(3),
                    ),
                  ),
                ),
              ),
              Expanded(
                key: const ValueKey('qt-note-editor-panel'),
                flex: 100 - _previewFlex,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                  child: Column(
                    children: [
                      // 본문 편집(툴바·@멘션·서식)은 자유 노트 N-03과 공유하는 위젯에 위임한다.
                      Expanded(
                        child: NoteRichTextEditor(
                          controller: _bodyController,
                          bodyLabel: '노트 작성',
                          pageMode: _pageMode,
                          onPageModeChanged: (mode) {
                            setState(() => _pageMode = mode);
                            _persistCanvas();
                          },
                          strokes: _strokes,
                          onStrokesChanged: (strokes) {
                            setState(() => _strokes = strokes);
                            _persistCanvas();
                          },
                          header: TextField(
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
                          toolbarPlacement: NoteRichTextToolbarPlacement.left,
                          bodyFieldKey: const ValueKey('qt-note-body-input'),
                          bodyScrollKey:
                              const ValueKey('qt-note-editor-scroll'),
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
