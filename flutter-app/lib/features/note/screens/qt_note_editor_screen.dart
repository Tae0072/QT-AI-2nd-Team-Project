import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../bible/models/bible_models.dart';
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
  final _passageScrollController = ScrollController();
  bool _saving = false;

  TodayQtPassage get _passage => widget.args.passage;

  @override
  void initState() {
    super.initState();
    _titleController.text = _passage.title ?? _passage.reference.displayText;
  }

  @override
  void dispose() {
    _titleController.dispose();
    _bodyController.dispose();
    _passageScrollController.dispose();
    super.dispose();
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
                      // 본문 편집(툴바·@멘션·서식)은 자유 노트 N-03과 공유하는 위젯에 위임한다.
                      Expanded(
                        child: NoteRichTextEditor(
                          controller: _bodyController,
                          bodyLabel: '노트 작성',
                          bodyFieldKey: const ValueKey('qt-note-body-input'),
                          bodyScrollKey: const ValueKey('qt-note-editor-scroll'),
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
