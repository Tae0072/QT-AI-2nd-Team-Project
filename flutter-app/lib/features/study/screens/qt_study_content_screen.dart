import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../../bible/models/bible_models.dart';
import '../../bible/providers/bible_providers.dart';

class QtStudyContentArgs {
  final int qtPassageId;
  final String referenceText;
  final Map<int, String> verseLabels;

  /// 절 본문(verseId → 한글 본문). 절별 해설에서 절 번호 옆에 본문을 함께 보여준다.
  final Map<int, String> verseTexts;

  const QtStudyContentArgs({
    required this.qtPassageId,
    required this.referenceText,
    required this.verseLabels,
    this.verseTexts = const {},
  });
}

class QtStudyContentScreen extends ConsumerWidget {
  final QtStudyContentArgs args;

  const QtStudyContentScreen({
    super.key,
    required this.args,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final studyContent = ref.watch(qtStudyContentProvider(args.qtPassageId));

    return Scaffold(
      appBar: AppBar(title: const Text('해설')),
      body: studyContent.whenOrDefault(
        loading: () => const LoadingView(message: '해설을 불러오는 중입니다.'),
        error: (error, _) => ErrorView(
          message: '해설을 불러오지 못했습니다.\n$error',
          onRetry: () => ref.invalidate(
            qtStudyContentProvider(args.qtPassageId),
          ),
        ),
        data: (data) => _QtStudyContentBody(
          content: data,
          referenceText: args.referenceText,
          verseLabels: args.verseLabels,
          verseTexts: args.verseTexts,
        ),
      ),
    );
  }
}

class _QtStudyContentBody extends StatelessWidget {
  final QtStudyContent content;
  final String referenceText;
  final Map<int, String> verseLabels;
  final Map<int, String> verseTexts;

  const _QtStudyContentBody({
    required this.content,
    required this.referenceText,
    required this.verseLabels,
    required this.verseTexts,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    if (!content.hasVisibleContent) {
      return const EmptyView(
        message: '아직 준비된 해설이 없습니다.',
        icon: Icons.menu_book_outlined,
      );
    }

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
      children: [
        Text(
          referenceText,
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        if (content.summary?.trim().isNotEmpty ?? false) ...[
          const SizedBox(height: 16),
          Text(
            content.summary!.trim(),
            style: theme.textTheme.bodyLarge?.copyWith(height: 1.6),
          ),
        ],
        if (content.explanations.isNotEmpty) ...[
          const SizedBox(height: 24),
          Text(
            '절별 해설',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 10),
          for (final explanation in content.explanations)
            _ExplanationItem(
              explanation: explanation,
              verseLabel: verseLabels[explanation.verseId],
              verseText: verseTexts[explanation.verseId],
            ),
        ],
        if (content.glossaryTerms.isNotEmpty) ...[
          const SizedBox(height: 24),
          Text(
            '단어 풀이',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 10),
          for (final term in content.glossaryTerms)
            _GlossaryTermItem(
              term: term,
              verseLabel: verseLabels[term.verseId],
            ),
        ],
      ],
    );
  }
}

class _ExplanationItem extends StatelessWidget {
  final QtStudyExplanation explanation;
  final String? verseLabel;
  final String? verseText;

  const _ExplanationItem({
    required this.explanation,
    required this.verseLabel,
    this.verseText,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final summary = explanation.summary?.trim();
    final verse = verseText?.trim();

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 절 번호(장:절) + 그 절 본문을 한 줄로: "9:1  태초에 ..."
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  verseLabel ?? '절',
                  style: theme.textTheme.labelLarge?.copyWith(
                    color: theme.colorScheme.primary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                if (verse != null && verse.isNotEmpty) ...[
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      verse,
                      style: theme.textTheme.bodyMedium?.copyWith(height: 1.5),
                    ),
                  ),
                ],
              ],
            ),
            if (summary != null && summary.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(
                summary,
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
            if (explanation.explanation.trim().isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(
                explanation.explanation.trim(),
                style: theme.textTheme.bodyMedium?.copyWith(height: 1.55),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _GlossaryTermItem extends StatelessWidget {
  final QtStudyGlossaryTerm term;
  final String? verseLabel;

  const _GlossaryTermItem({
    required this.term,
    required this.verseLabel,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              verseLabel == null ? term.term : '$verseLabel · ${term.term}',
              style: theme.textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
            if (term.meaning.trim().isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(
                term.meaning.trim(),
                style: theme.textTheme.bodyMedium?.copyWith(height: 1.55),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
