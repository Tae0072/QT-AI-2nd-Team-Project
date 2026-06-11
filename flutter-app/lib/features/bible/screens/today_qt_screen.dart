import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../../note/screens/qt_note_editor_screen.dart';
import '../../study/screens/qt_study_content_screen.dart';
import '../../music/widgets/music_toggle_button.dart';
import '../../tts/widgets/qt_tts_button.dart';
import '../models/bible_models.dart';
import '../providers/bible_providers.dart';
import '../widgets/qt_video_player.dart';

/// QT 본문 전체 텍스트 (TTS 입력용).
String _fullTextOf(TodayQtPassage data) {
  final buf = StringBuffer();
  for (final v in data.verses) {
    final t = v.koreanText?.trim();
    if (t != null && t.isNotEmpty) buf.writeln(t);
  }
  return buf.toString().trim();
}

/// QT 날짜 (TTS 캐시 키용).
String _qtDateOf(TodayQtPassage data) =>
    data.passageDate ?? DateTime.now().toIso8601String().substring(0, 10);

class TodayQtScreen extends ConsumerWidget {
  const TodayQtScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final passage = ref.watch(todayQtPassageProvider);
    final data = passage.valueOrNull;
    final fullText = data == null ? '' : _fullTextOf(data);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.bibleTodayQt),
        actions: [
          // 본문 읽기(TTS) — 아이콘 하나로 재생/정지 토글
          if (data != null && fullText.isNotEmpty)
            QtTtsButton(
              qtText: fullText,
              qtDate: _qtDateOf(data),
              qtPassageId: data.qtPassageId,
            ),
          // 배경음악 켜기/끄기 — TTS 버튼 오른쪽 음표 토글
          const MusicToggleButton(),
          IconButton(
            tooltip: l.commonRefresh,
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.invalidate(todayQtPassageProvider),
          ),
        ],
      ),
      body: passage.whenOrDefault(
        loading: () => LoadingView(message: l.bibleTodayLoading),
        error: (error, _) => ErrorView(
          message: '${l.bibleTodayLoadError}\n$error',
          onRetry: () => ref.invalidate(todayQtPassageProvider),
        ),
        data: (data) => _TodayQtContent(
          data: data,
          // 당겨서 새로고침: 오늘 QT를 다시 불러오고 완료될 때까지 대기한다.
          onRefresh: () => ref.refresh(todayQtPassageProvider.future),
        ),
      ),
    );
  }
}

class _TodayQtContent extends StatefulWidget {
  final TodayQtPassage data;
  final Future<void> Function() onRefresh;

  const _TodayQtContent({required this.data, required this.onRefresh});

  @override
  State<_TodayQtContent> createState() => _TodayQtContentState();
}

class _TodayQtContentState extends State<_TodayQtContent> {
  final _scrollController = ScrollController();
  final _videoSectionKey = GlobalKey();
  bool _showEnglish = false;

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _scrollToVideo() async {
    if (!_scrollController.hasClients) {
      return;
    }

    for (var attempt = 0; attempt < 3; attempt++) {
      if (!mounted || !_scrollController.hasClients) {
        return;
      }

      final targetContext = _videoSectionKey.currentContext;
      if (targetContext == null || !targetContext.mounted) {
        await _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: Duration(milliseconds: attempt == 0 ? 420 : 220),
          curve: Curves.easeOutCubic,
        );
      } else {
        await Scrollable.ensureVisible(
          targetContext,
          duration: Duration(milliseconds: attempt == 0 ? 320 : 180),
          curve: Curves.easeOutCubic,
          alignment: 0.04,
        );
      }

      await WidgetsBinding.instance.endOfFrame;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final data = widget.data;

    return RefreshIndicator(
      onRefresh: widget.onRefresh,
      child: ListView(
        controller: _scrollController,
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        children: [
          Text(
            data.reference.displayText,
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 6),
          if (data.title != null && data.title!.isNotEmpty) ...[
            Text(
              data.title!,
              style: theme.textTheme.titleMedium,
            ),
            const SizedBox(height: 6),
          ],
          if (_showEnglish) ...[
            Text(
              data.book.englishName,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 6),
          ],
          const SizedBox(height: 16),
          _ActionRow(
            data: data,
            showEnglish: _showEnglish,
            onShowEnglishChanged: (selected) {
              setState(() => _showEnglish = selected);
            },
            onVideoRequested: _scrollToVideo,
          ),
          const SizedBox(height: 20),
          for (final verse in data.verses)
            _VerseTile(
              verse: verse,
              showEnglish: _showEnglish,
            ),
          if (data.qtPassageId != null) ...[
            const SizedBox(height: 12),
            KeyedSubtree(
              key: _videoSectionKey,
              child: QtVideoSection(
                key: const Key('today-qt-video-section'),
                qtPassageId: data.qtPassageId!,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _ActionRow extends StatelessWidget {
  final TodayQtPassage data;
  final bool showEnglish;
  final ValueChanged<bool> onShowEnglishChanged;
  final VoidCallback onVideoRequested;

  const _ActionRow({
    required this.data,
    required this.showEnglish,
    required this.onShowEnglishChanged,
    required this.onVideoRequested,
  });

  @override
  Widget build(BuildContext context) {
    final qtPassageId = data.qtPassageId;
    final hasQtVideoTarget = qtPassageId != null;
    final explanationReady = qtPassageId != null && data.hasExplanation;
    final l = AppLocalizations.of(context);

    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        FilledButton.icon(
          onPressed: !explanationReady
              ? null
              : () => Navigator.of(context).pushNamed(
                    AppRouter.qtStudyContent,
                    arguments: QtStudyContentArgs(
                      qtPassageId: qtPassageId,
                      referenceText: data.reference.displayText,
                      verseLabels: {
                        for (final verse in data.verses)
                          verse.id: '${verse.chapterNo}:${verse.verseNo}',
                      },
                    ),
                  ),
          icon: const Icon(Icons.menu_book_outlined),
          label: Text(l.bibleExplanation),
        ),
        OutlinedButton.icon(
          onPressed: hasQtVideoTarget ? onVideoRequested : null,
          icon: const Icon(Icons.movie_outlined),
          label: Text(l.bibleSimulator),
        ),
        OutlinedButton.icon(
          onPressed: qtPassageId == null
              ? null
              : () => Navigator.of(context).pushNamed(
                    AppRouter.qtNoteEditor,
                    arguments: QtNoteEditorArgs(passage: data),
                  ),
          icon: const Icon(Icons.edit_note_outlined),
          label: Text(l.navNote),
        ),
        FilterChip(
          key: const Key('today-qt-english-toggle'),
          selected: showEnglish,
          onSelected: onShowEnglishChanged,
          label: const Text('영어'),
        ),
      ],
    );
  }
}

class _VerseTile extends StatelessWidget {
  final BibleVerse verse;
  final bool showEnglish;

  const _VerseTile({
    required this.verse,
    required this.showEnglish,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final koreanText = verse.koreanText?.trim();
    final englishText = verse.englishText?.trim();

    return Padding(
      padding: const EdgeInsets.only(bottom: 18),
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
          const SizedBox(height: 6),
          if (koreanText != null && koreanText.isNotEmpty)
            Text(
              koreanText,
              style: theme.textTheme.bodyLarge?.copyWith(height: 1.6),
            ),
          if (showEnglish && englishText != null && englishText.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              englishText,
              style: theme.textTheme.bodyMedium?.copyWith(
                height: 1.5,
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ],
      ),
    );
  }
}
