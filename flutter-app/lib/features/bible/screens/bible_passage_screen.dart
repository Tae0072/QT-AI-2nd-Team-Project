import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_dimens.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/calm_paper.dart';
import '../../../routes/app_router.dart';
import '../../note/models/note_models.dart';
import '../../note/screens/note_edit_screen.dart';
import '../../study/screens/qt_study_content_screen.dart';
import '../models/bible_models.dart';
import '../models/passage_view_logic.dart';
import '../models/verse_range_selection.dart';
import '../providers/bible_providers.dart';

/// 성경 본문 전체 페이지 — 목차에서 권/장/절을 선택해 '조회'하면 진입한다.
///
/// 변경(2026-06): 선택 범위만 보여주던 방식 → **장 전체**를 보여주고, 진입 시
/// 선택한 절([focusVerseNo])을 첫 화면에 보이도록 스크롤·하이라이트한다. 본문에서
/// 절을 탭-탭으로 범위 지정한 뒤 '노트 작성하기'를 누르면 설교 노트 작성 화면으로
/// 선택 절을 동봉해 이동한다(오늘의 QT 노트 흐름과 동일).
///
/// 해당 범위에 승인된 해설이 있으면 해설 진입점을 노출한다(`/qt/passage-study`).
class BiblePassageScreen extends ConsumerStatefulWidget {
  /// 장 전체 본문(book + 그 장의 모든 절).
  final BibleVerseRange chapter;

  /// 진입 시 포커스(첫 화면 노출·하이라이트)할 절 번호. null이면 첫 절.
  final int? focusVerseNo;

  const BiblePassageScreen({
    super.key,
    required this.chapter,
    this.focusVerseNo,
  });

  @override
  ConsumerState<BiblePassageScreen> createState() => _BiblePassageScreenState();
}

class _BiblePassageScreenState extends ConsumerState<BiblePassageScreen> {
  bool _showEnglish = false;

  /// 본문에서의 절 범위 선택(탭-탭). 진입 시 포커스 절을 단일 선택으로 시작한다.
  late VerseRangeSelection _selection;

  /// 진입 포커스 절(첫 화면 스크롤 대상 + 해설 가용성 기준).
  late final int _focusVerseNo;

  /// 해설 가용성 조회용 고정 ref — 진입 포커스 기준으로 **1회만** 조회한다.
  /// (절 선택마다 재조회하지 않도록 live 선택 _selection이 아닌 고정값을 쓴다.)
  late final BiblePassageRef _studyRef;

  final _scrollController = ScrollController();
  final Map<int, GlobalKey> _verseKeys = {};

  BibleVerseRange get _chapter => widget.chapter;
  List<BibleVerse> get _verses => _chapter.verses;

  @override
  void initState() {
    super.initState();
    final verseNos = _verses.map((v) => v.verseNo).toList();
    _focusVerseNo = resolvePassageFocusVerse(verseNos, widget.focusVerseNo);
    _selection = VerseRangeSelection(from: _focusVerseNo, to: _focusVerseNo);
    _studyRef = (
      bookCode: _chapter.book.code,
      chapter: _chapter.book.chapter,
      verseFrom: _focusVerseNo,
      verseTo: _focusVerseNo,
    );
    for (final v in _verses) {
      _verseKeys[v.verseNo] = GlobalKey();
    }
    // 첫 프레임 후 포커스 절로 스크롤(첫 줄에 보이게).
    WidgetsBinding.instance
        .addPostFrameCallback((_) => _scrollToFocus(_focusVerseNo));
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollToFocus(int verseNo) {
    final key = _verseKeys[verseNo];
    final ctx = key?.currentContext;
    if (ctx != null) {
      Scrollable.ensureVisible(
        ctx,
        alignment: 0.0, // 화면 상단(첫 줄)에 맞춘다.
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  void _tapVerse(int verseNo) {
    setState(() => _selection = _selection.tap(verseNo));
  }

  List<BibleVerse> get _selectedVerses => _verses
      .where((v) => v.verseNo >= _selection.from && v.verseNo <= _selection.to)
      .toList();

  String get _selectionLabel =>
      '${_chapter.book.koreanName} ${_chapter.book.chapter}:'
      '${passageVerseLabel(_selection.from, _selection.to)}';

  /// 해설 진입 — 가용성 조회와 동일하게 진입 포커스 절 기준으로 연다.
  void _openExplanation(int qtPassageId) {
    final focusVerses =
        _verses.where((v) => v.verseNo == _focusVerseNo).toList();
    Navigator.of(context).pushNamed(
      AppRouter.qtStudyContent,
      arguments: QtStudyContentArgs(
        qtPassageId: qtPassageId,
        referenceText:
            '${_chapter.book.koreanName} ${_chapter.book.chapter}:$_focusVerseNo',
        verseLabels: {
          for (final verse in focusVerses) verse.id: '${verse.verseNo}',
        },
      ),
    );
  }

  /// 선택 범위로 설교 노트 작성 화면 진입 — 선택 절을 동봉(verseIds)한다.
  /// 오늘의 QT 노트와 같은 방식(절 참조 저장). 본문 텍스트는 인용 미리보기로 전달한다.
  void _writeNote() {
    final selected = _selectedVerses;
    if (selected.isEmpty) return;
    final previewText = selected
        .map((v) => (v.koreanText ?? '').trim())
        .where((t) => t.isNotEmpty)
        .join('\n');
    Navigator.of(context).pushNamed(
      AppRouter.noteEdit,
      arguments: NoteEditArgs(
        category: kNoteCatSermon,
        verseIds: selected.map((v) => v.id).toList(),
        referenceText: _selectionLabel,
        versePreview: previewText.isEmpty ? null : previewText,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.appColors;
    final l = AppLocalizations.of(context);
    final book = _chapter.book;

    // 해설 가용성 — 진입 포커스 기준 1회 조회(절 선택마다 재조회하지 않음).
    final study = ref.watch(biblePassageStudyProvider(_studyRef));
    final explanation = study.valueOrNull ?? BiblePassageStudy.none;
    final explanationReady = explanation.explanationReady;

    return Scaffold(
      backgroundColor: colors.bg,
      appBar: AppBar(
        title: Text('${book.koreanName} ${book.chapter}장'),
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView(
              controller: _scrollController,
              // 한 장의 모든 절을 미리 빌드해, 먼 절로의 포커스 스크롤
              // (Scrollable.ensureVisible)이 지연 빌드로 실패하지 않게 한다.
              cacheExtent: 100000,
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
                      key: const Key('bible-explanation-button'),
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
                const SizedBox(height: AppGap.sm),
                Text(
                  '절을 탭하면 단일 선택, 한 번 더 다른 절을 탭하면 범위로 지정됩니다.',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: colors.textMuted,
                  ),
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
                for (final verse in _verses)
                  _PassageVerseTile(
                    key: _verseKeys[verse.verseNo],
                    verse: verse,
                    showEnglish: _showEnglish,
                    selected: verse.verseNo >= _selection.from &&
                        verse.verseNo <= _selection.to,
                    onTap: () => _tapVerse(verse.verseNo),
                  ),
              ],
            ),
          ),
          _NoteActionBar(
            label: _selectionLabel,
            onWriteNote: _writeNote,
          ),
        ],
      ),
    );
  }
}

/// 하단 고정 액션바 — 현재 선택 범위 표시 + '노트 작성하기'.
class _NoteActionBar extends StatelessWidget {
  final String label;
  final VoidCallback onWriteNote;

  const _NoteActionBar({required this.label, required this.onWriteNote});

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    final theme = Theme.of(context);
    return Material(
      color: colors.bgElevated,
      elevation: 0,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            AppGap.xl,
            AppGap.md,
            AppGap.xl,
            AppGap.md,
          ),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      '선택 범위',
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: colors.textMuted,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      label,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: colors.text,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: AppGap.md),
              FilledButton.icon(
                onPressed: onWriteNote,
                icon: const Icon(Icons.edit_note_outlined),
                label: const Text('노트 작성하기'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PassageVerseTile extends StatelessWidget {
  final BibleVerse verse;
  final bool showEnglish;
  final bool selected;
  final VoidCallback onTap;

  const _PassageVerseTile({
    super.key,
    required this.verse,
    required this.showEnglish,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.appColors;
    final koreanText = verse.koreanText?.trim();
    final englishText = verse.englishText?.trim();

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(10),
      child: Container(
        margin: const EdgeInsets.only(bottom: AppGap.sm),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
        decoration: BoxDecoration(
          // 선택된 절은 accentSoft 면으로 강조(색 위계 대신 명도).
          color: selected ? colors.accentSoft : Colors.transparent,
          borderRadius: BorderRadius.circular(10),
        ),
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
      ),
    );
  }
}
