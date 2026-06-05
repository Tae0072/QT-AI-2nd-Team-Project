import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../../note/screens/qt_note_editor_screen.dart';
import '../../study/screens/qt_study_content_screen.dart';
import '../models/bible_models.dart';
import '../providers/bible_providers.dart';

class TodayQtScreen extends ConsumerWidget {
  const TodayQtScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final passage = ref.watch(todayQtPassageProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('오늘 QT'),
        actions: [
          IconButton(
            tooltip: '새로고침',
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.invalidate(todayQtPassageProvider),
          ),
        ],
      ),
      body: passage.whenOrDefault(
        loading: () => const LoadingView(message: '오늘 본문을 불러오는 중입니다.'),
        error: (error, _) => ErrorView(
          message: '오늘 본문을 불러오지 못했습니다.\n$error',
          onRetry: () => ref.invalidate(todayQtPassageProvider),
        ),
        data: (data) => _TodayQtContent(data: data),
      ),
    );
  }
}

class _TodayQtContent extends StatefulWidget {
  final TodayQtPassage data;

  const _TodayQtContent({required this.data});

  @override
  State<_TodayQtContent> createState() => _TodayQtContentState();
}

class _TodayQtContentState extends State<_TodayQtContent> {
  bool _showEnglish = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final data = widget.data;

    return RefreshIndicator(
      onRefresh: () async {},
      child: ListView(
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
          ),
          const SizedBox(height: 20),
          for (final verse in data.verses)
            _VerseTile(
              verse: verse,
              showEnglish: _showEnglish,
            ),
        ],
      ),
    );
  }
}

class _ActionRow extends StatelessWidget {
  final TodayQtPassage data;
  final bool showEnglish;
  final ValueChanged<bool> onShowEnglishChanged;

  const _ActionRow({
    required this.data,
    required this.showEnglish,
    required this.onShowEnglishChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        FilledButton.icon(
          onPressed: data.qtPassageId == null
              ? null
              : () => Navigator.of(context).pushNamed(
                    AppRouter.qtStudyContent,
                    arguments: QtStudyContentArgs(
                      qtPassageId: data.qtPassageId!,
                      referenceText: data.reference.displayText,
                      verseLabels: {
                        for (final verse in data.verses)
                          verse.id: '${verse.chapterNo}:${verse.verseNo}',
                      },
                    ),
                  ),
          icon: const Icon(Icons.menu_book_outlined),
          label: const Text('해설'),
        ),
        OutlinedButton.icon(
          onPressed: data.qtPassageId == null ? null : () {},
          icon: const Icon(Icons.movie_outlined),
          label: const Text('시뮬레이터'),
        ),
        OutlinedButton.icon(
          onPressed: () => Navigator.of(context).pushNamed(
            AppRouter.qtNoteEditor,
            arguments: QtNoteEditorArgs(passage: data),
          ),
          icon: const Icon(Icons.edit_note_outlined),
          label: const Text('노트'),
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
