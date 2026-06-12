import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_dimens.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/calm_paper.dart';
import '../../../routes/app_router.dart';
import '../../study/screens/qt_study_content_screen.dart';
import '../models/bible_models.dart';
import '../providers/bible_providers.dart';

/// 성경 본문 전체 페이지 — 목차에서 권/장/절을 선택해 '조회'하면 진입한다.
///
/// 기존 바텀시트(작은 슬라이드) 대신 전체 화면으로 본문을 보여준다.
/// 해당 범위에 승인된 해설이 있으면, 오늘의 QT와 동일하게 해설 진입점을 노출한다.
/// 해설 가용성은 qt 도메인의 `/qt/passage-study`에서 별도 조회한다(모듈 순환 방지).
class BiblePassageScreen extends ConsumerStatefulWidget {
  final BibleVerseRange range;

  const BiblePassageScreen({super.key, required this.range});

  @override
  ConsumerState<BiblePassageScreen> createState() => _BiblePassageScreenState();
}

class _BiblePassageScreenState extends ConsumerState<BiblePassageScreen> {
  bool _showEnglish = false;

  BibleVerseRange get _range => widget.range;

  String get _referenceText {
    final book = _range.book;
    final verses = _range.verses;
    if (verses.isEmpty) {
      return '${book.koreanName} ${book.chapter}장';
    }
    final first = verses.first.verseNo;
    final last = verses.last.verseNo;
    final verseLabel = first == last ? '$first' : '$first-$last';
    return '${book.koreanName} ${book.chapter}:$verseLabel';
  }

  BiblePassageRef? get _passageRef {
    final verses = _range.verses;
    if (verses.isEmpty) {
      return null;
    }
    return (
      bookCode: _range.book.code,
      chapter: _range.book.chapter,
      verseFrom: verses.first.verseNo,
      verseTo: verses.last.verseNo,
    );
  }

  void _openExplanation(int qtPassageId) {
    Navigator.of(context).pushNamed(
      AppRouter.qtStudyContent,
      arguments: QtStudyContentArgs(
        qtPassageId: qtPassageId,
        referenceText: _referenceText,
        verseLabels: {
          for (final verse in _range.verses) verse.id: '${verse.verseNo}',
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.appColors;
    final l = AppLocalizations.of(context);
    final book = _range.book;

    // 해설 가용성 — 조회 실패/로딩 시에는 진입점을 숨긴다(서버 미배포 시에도 안전).
    final ref0 = _passageRef;
    final study = ref0 == null
        ? const AsyncValue<BiblePassageStudy>.data(BiblePassageStudy.none)
        : ref.watch(biblePassageStudyProvider(ref0));
    final explanation = study.valueOrNull ?? BiblePassageStudy.none;
    final explanationReady = explanation.explanationReady;

    return Scaffold(
      backgroundColor: colors.bg,
      appBar: AppBar(
        title: Text('${book.koreanName} ${book.chapter}장'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(
          AppGap.xl,
          AppGap.md,
          AppGap.xl,
          AppGap.xxl,
        ),
        children: [
          Wrap(
            spacing: AppGap.sm,
            runSpacing: AppGap.sm,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              FilledButton.icon(
                onPressed: explanationReady
                    ? () => _openExplanation(explanation.qtPassageId!)
                    : null,
                icon: const Icon(Icons.menu_book_outlined),
                label: Text(l.bibleExplanation),
              ),
              FilterChip(
                key: const Key('bible-browser-english-toggle'),
                selected: _showEnglish,
                onSelected: (selected) =>
                    setState(() => _showEnglish = selected),
                label: const Text('영어'),
              ),
            ],
          ),
          if (_showEnglish) ...[
            const SizedBox(height: AppGap.sm),
            Text(
              book.englishName,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colors.textMuted,
              ),
            ),
          ],
          const SizedBox(height: AppGap.lg),
          Divider(color: colors.hairline, height: 1),
          const SizedBox(height: AppGap.lg),
          for (final verse in _range.verses)
            _PassageVerseTile(verse: verse, showEnglish: _showEnglish),
        ],
      ),
    );
  }
}

class _PassageVerseTile extends StatelessWidget {
  final BibleVerse verse;
  final bool showEnglish;

  const _PassageVerseTile({required this.verse, required this.showEnglish});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.appColors;
    final koreanText = verse.koreanText?.trim();
    final englishText = verse.englishText?.trim();

    return Padding(
      padding: const EdgeInsets.only(bottom: AppGap.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${verse.chapterNo}:${verse.verseNo}',
            style: theme.textTheme.labelSmall?.copyWith(
              color: colors.text2,
              fontWeight: FontWeight.w700,
              letterSpacing: 0.2,
            ),
          ),
          const SizedBox(height: AppGap.xs),
          if (koreanText != null && koreanText.isNotEmpty)
            Text(koreanText, style: theme.textTheme.bodyLarge),
          // 영어 본문은 오늘의 QT(_VerseTile)와 동일하게 sunken sub-box로 분리한다.
          if (showEnglish && englishText != null && englishText.isNotEmpty)
            CpSubBox(
              margin: const EdgeInsets.only(top: 10),
              child: Text(
                englishText,
                style: TextStyle(
                  fontFamily: 'GowunDodum',
                  fontSize: 14,
                  height: 1.55,
                  color: colors.text2,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
