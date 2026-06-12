import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/theme/app_dimens.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../../note/providers/note_providers.dart';
import '../models/bible_chapter_counts.dart';
import '../models/bible_models.dart';
import '../models/verse_range_selection.dart';
import '../providers/bible_providers.dart';
import 'bible_passage_screen.dart';

/// 성경 본문 찾기 — 권/장/절 3단 선택 화면.
///
/// Calm Paper 테마(`context.appColors`)를 따른다. 위계는 색이 아니라
/// 명도·여백·굵기로 만든다. 선택 표시는 [AppColors.accentSoft] 면 + 본문색 굵게.
class BibleBrowserScreen extends ConsumerStatefulWidget {
  const BibleBrowserScreen({super.key});

  @override
  ConsumerState<BibleBrowserScreen> createState() => _BibleBrowserScreenState();
}

class _BibleBrowserScreenState extends ConsumerState<BibleBrowserScreen> {
  static const _defaultVerseCount = 1;

  late Future<List<BibleBook>> _booksFuture;
  String? _selectedBookCode;
  int _selectedChapter = 1;
  int _verseFrom = 1;
  int _verseTo = 1;
  // 첫 절을 찍어 범위 시작을 대기 중인지(true=다음 탭이 범위 끝).
  bool _verseRangeAnchored = false;
  int _verseCount = _defaultVerseCount;
  int _chapterRequestId = 0;

  Object? _error;
  bool _isSearching = false;
  bool _isLoadingChapter = false;
  bool _chapterLoadFailed = false;

  @override
  void initState() {
    super.initState();
    _booksFuture = ref.read(bibleRepositoryProvider).getBooks();
    _booksFuture.then((books) {
      if (!mounted || books.isEmpty) {
        return;
      }
      final book = books.first;
      setState(() => _selectedBookCode ??= book.code);
      _loadChapterVerseCount(book.code, _selectedChapter);
    });
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
        _error = null;
      });
    } catch (error) {
      if (!mounted || requestId != _chapterRequestId) {
        return;
      }
      setState(() {
        _verseCount = _defaultVerseCount;
        _verseFrom = _verseFrom.clamp(1, _verseCount);
        _verseTo = _verseTo.clamp(_verseFrom, _verseCount);
        _isLoadingChapter = false;
        _chapterLoadFailed = true;
        _error = error;
      });
    }
  }

  Future<void> _search(String bookCode) async {
    setState(() {
      _isSearching = true;
      _error = null;
    });

    try {
      // 본문은 장 전체를 불러오고, 목차에서 고른 절([_verseFrom])을 포커스로 넘긴다.
      final chapterRange =
          await ref.read(bibleRepositoryProvider).getChapterVerses(
                bookCode: bookCode,
                chapter: _selectedChapter,
              );
      if (!mounted) {
        return;
      }
      // 작은 바텀시트 대신 전체 페이지로 본문을 보여준다(해설 진입점·노트 작성 포함).
      await Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => BiblePassageScreen(
            chapter: chapterRange,
            focusVerseNo: _verseFrom,
          ),
        ),
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _error = error;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('성경본문을 불러오지 못했습니다. 다시 시도해 주세요.')),
      );
    } finally {
      if (mounted) {
        setState(() => _isSearching = false);
      }
    }
  }

  void _selectBook(BibleBook book) {
    setState(() {
      _selectedBookCode = book.code;
      _selectedChapter = 1;
      _verseFrom = 1;
      _verseTo = 1;
      _verseRangeAnchored = false;
      _verseCount = _defaultVerseCount;
      _chapterLoadFailed = false;
      _error = null;
    });
    _loadChapterVerseCount(book.code, _selectedChapter);
  }

  void _selectChapter(String bookCode, int chapter) {
    setState(() {
      _selectedChapter = chapter;
      _verseFrom = 1;
      _verseTo = 1;
      _verseRangeAnchored = false;
      _verseCount = _defaultVerseCount;
      _chapterLoadFailed = false;
      _error = null;
    });
    _loadChapterVerseCount(bookCode, chapter);
  }

  // 절 선택 — 범위(시작~끝) 지정 지원.
  // 첫 탭: 그 절로 단일 선택(범위 시작 앵커). 둘째 탭: 범위 끝 지정(앞/뒤 자동 정렬).
  // 셋째 탭: 다시 새 단일 선택으로 시작. → "탭-탭"으로 범위, "탭" 한 번이면 단일.
  void _selectVerse(int verse) {
    final next = VerseRangeSelection(
      from: _verseFrom,
      to: _verseTo,
      anchored: _verseRangeAnchored,
    ).tap(verse);
    setState(() {
      _verseFrom = next.from;
      _verseTo = next.to;
      _verseRangeAnchored = next.anchored;
      _error = null;
    });
  }

  void _clearVerseSelection() {
    setState(() {
      _verseFrom = 1;
      _verseTo = 1;
      _verseRangeAnchored = false;
      _error = null;
    });
  }

  int _chapterCountFor(BibleBook book) {
    return bibleChapterCountForDisplayOrder(book.displayOrder);
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    return Scaffold(
      backgroundColor: colors.bg,
      body: FutureBuilder<List<BibleBook>>(
        future: _booksFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const _BibleTocShell(
              child: LoadingView(message: '성경 권 목록을 불러오는 중입니다.'),
            );
          }

          if (snapshot.hasError) {
            return _BibleTocShell(
              child: ErrorView(
                message: '성경 권 목록을 불러오지 못했습니다.\n${snapshot.error}',
                onRetry: () {
                  setState(() {
                    _booksFuture = ref.read(bibleRepositoryProvider).getBooks();
                    _error = null;
                  });
                },
              ),
            );
          }

          final books = snapshot.data ?? const <BibleBook>[];
          if (books.isEmpty) {
            return const _BibleTocShell(
              child: EmptyView(message: '성경 권 목록이 없습니다.'),
            );
          }

          final selectedBookCode = _selectedBookCode ?? books.first.code;
          final selectedBook = books.firstWhere(
            (book) => book.code == selectedBookCode,
            orElse: () => books.first,
          );

          return _BibleBrowserContent(
            books: books,
            selectedBook: selectedBook,
            selectedChapter: _selectedChapter,
            chapterCount: _chapterCountFor(selectedBook),
            verseFrom: _verseFrom,
            verseTo: _verseTo,
            verseCount: _verseCount,
            error: _error,
            isSearching: _isSearching,
            isLoadingChapter: _isLoadingChapter,
            isChapterLoadFailed: _chapterLoadFailed,
            onBookChanged: _selectBook,
            onChapterChanged: (chapter) =>
                _selectChapter(selectedBook.code, chapter),
            onVerseChanged: _selectVerse,
            onClearSelection: _clearVerseSelection,
            onSearch: () => _search(selectedBook.code),
          );
        },
      ),
    );
  }
}

class _BibleTocShell extends StatelessWidget {
  final Widget child;

  const _BibleTocShell({
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const _BibleTocHeader(),
        Expanded(child: child),
      ],
    );
  }
}

class _BibleBrowserContent extends StatelessWidget {
  final List<BibleBook> books;
  final BibleBook selectedBook;
  final int selectedChapter;
  final int chapterCount;
  final int verseFrom;
  final int verseTo;
  final int verseCount;
  final Object? error;
  final bool isSearching;
  final bool isLoadingChapter;
  final bool isChapterLoadFailed;
  final ValueChanged<BibleBook> onBookChanged;
  final ValueChanged<int> onChapterChanged;
  final ValueChanged<int> onVerseChanged;
  final VoidCallback onClearSelection;
  final VoidCallback onSearch;

  const _BibleBrowserContent({
    required this.books,
    required this.selectedBook,
    required this.selectedChapter,
    required this.chapterCount,
    required this.verseFrom,
    required this.verseTo,
    required this.verseCount,
    required this.error,
    required this.isSearching,
    required this.isLoadingChapter,
    required this.isChapterLoadFailed,
    required this.onBookChanged,
    required this.onChapterChanged,
    required this.onVerseChanged,
    required this.onClearSelection,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const _BibleTocHeader(),
        Expanded(
          child: _BibleTocPicker(
            books: books,
            selectedBook: selectedBook,
            selectedChapter: selectedChapter,
            chapterCount: chapterCount,
            verseFrom: verseFrom,
            verseTo: verseTo,
            verseCount: verseCount,
            isLoadingChapter: isLoadingChapter,
            onBookChanged: onBookChanged,
            onChapterChanged: onChapterChanged,
            onVerseChanged: onVerseChanged,
          ),
        ),
        if (error != null || isChapterLoadFailed)
          const _BibleStatusStrip(message: '절 목록을 불러오지 못했습니다.'),
        _BibleSelectionBar(
          selectedBook: selectedBook,
          selectedChapter: selectedChapter,
          verseFrom: verseFrom,
          verseTo: verseTo,
          isBusy: isSearching || isLoadingChapter,
          isDisabled: isChapterLoadFailed,
          onClear: onClearSelection,
          onSearch: onSearch,
        ),
      ],
    );
  }
}

class _BibleTocHeader extends StatelessWidget {
  const _BibleTocHeader();

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    return Container(
      decoration: BoxDecoration(
        color: colors.bg,
        border: Border(bottom: BorderSide(color: colors.hairline)),
      ),
      child: SafeArea(
        bottom: false,
        child: SizedBox(
          height: 56,
          child: Row(
            children: [
              const SizedBox(width: AppGap.lg),
              Expanded(
                child: Text(
                  '성경 본문',
                  style: TextStyle(
                    color: colors.text,
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    letterSpacing: -0.3,
                  ),
                ),
              ),
              // 노트 버튼 — 클릭 시 기록(노트) 화면으로 이동(설교 노트 맥락).
              // 실제 기록은 기록 화면이 담당. (이전 #505에 있던 진입점 복원)
              Consumer(
                builder: (context, ref, _) => TextButton.icon(
                  onPressed: () {
                    ref.read(noteCategoryFilterProvider.notifier).state =
                        'SERMON';
                    Navigator.of(context).pushNamed(AppRouter.noteList);
                  },
                  icon: Icon(Icons.edit_note_outlined,
                      size: 18, color: colors.text),
                  label: Text('노트',
                      style: TextStyle(
                          color: colors.text,
                          fontSize: 14,
                          fontWeight: FontWeight.w600)),
                  style: TextButton.styleFrom(
                    foregroundColor: colors.text,
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    minimumSize: const Size(0, 36),
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                ),
              ),
              const SizedBox(width: 12),
            ],
          ),
        ),
      ),
    );
  }
}

class _BibleTocPicker extends StatelessWidget {
  final List<BibleBook> books;
  final BibleBook selectedBook;
  final int selectedChapter;
  final int chapterCount;
  final int verseFrom;
  final int verseTo;
  final int verseCount;
  final bool isLoadingChapter;
  final ValueChanged<BibleBook> onBookChanged;
  final ValueChanged<int> onChapterChanged;
  final ValueChanged<int> onVerseChanged;

  const _BibleTocPicker({
    required this.books,
    required this.selectedBook,
    required this.selectedChapter,
    required this.chapterCount,
    required this.verseFrom,
    required this.verseTo,
    required this.verseCount,
    required this.isLoadingChapter,
    required this.onBookChanged,
    required this.onChapterChanged,
    required this.onVerseChanged,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    final safeChapterCount = chapterCount < 1 ? 1 : chapterCount;
    final safeVerseCount = verseCount < 1 ? 1 : verseCount;

    return ColoredBox(
      color: colors.bg,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(
            flex: 5,
            child: _BibleBookColumn(
              books: books,
              selectedBook: selectedBook,
              onBookChanged: onBookChanged,
            ),
          ),
          const _TocDivider(),
          Expanded(
            flex: 3,
            child: _BibleNumberColumn(
              key: const Key('bible-chapter-list'),
              itemCount: safeChapterCount,
              selectedValue: selectedChapter,
              suffix: '장',
              onSelected: onChapterChanged,
            ),
          ),
          const _TocDivider(),
          Expanded(
            flex: 3,
            child: Stack(
              children: [
                _BibleNumberColumn(
                  key: const Key('bible-verse-list'),
                  itemCount: safeVerseCount,
                  selectedValue: verseFrom,
                  rangeEnd: verseTo,
                  suffix: '절',
                  onSelected: onVerseChanged,
                ),
                if (isLoadingChapter)
                  Align(
                    alignment: Alignment.topCenter,
                    child: LinearProgressIndicator(
                      minHeight: 2,
                      backgroundColor: colors.hairline,
                      color: colors.accent,
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _BibleBookColumn extends StatelessWidget {
  final List<BibleBook> books;
  final BibleBook selectedBook;
  final ValueChanged<BibleBook> onBookChanged;

  const _BibleBookColumn({
    required this.books,
    required this.selectedBook,
    required this.onBookChanged,
  });

  @override
  Widget build(BuildContext context) {
    final entries = _bookEntries(books);

    return ListView.builder(
      key: const Key('bible-book-list'),
      padding: EdgeInsets.zero,
      itemCount: entries.length,
      itemBuilder: (context, index) {
        final entry = entries[index];
        if (entry.section != null) {
          return _BibleBookSectionLabel(label: entry.section!);
        }

        final book = entry.book!;
        final isSelected = book.code == selectedBook.code;

        return _BibleBookRow(
          book: book,
          isSelected: isSelected,
          onTap: () => onBookChanged(book),
        );
      },
    );
  }
}

class _BibleBookSectionLabel extends StatelessWidget {
  final String label;

  const _BibleBookSectionLabel({required this.label});

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    return Container(
      alignment: Alignment.centerLeft,
      padding: const EdgeInsets.fromLTRB(AppGap.md, AppGap.sm, AppGap.md, AppGap.xs),
      color: colors.bgSunken,
      child: Text(
        label,
        style: TextStyle(
          color: colors.textMuted,
          fontSize: 11,
          fontWeight: FontWeight.w700,
          letterSpacing: 0.4,
        ),
      ),
    );
  }
}

class _BibleBookRow extends StatelessWidget {
  final BibleBook book;
  final bool isSelected;
  final VoidCallback onTap;

  const _BibleBookRow({
    required this.book,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    return Material(
      color: isSelected ? colors.accentSoft : colors.bg,
      child: InkWell(
        onTap: onTap,
        child: Container(
          // 고정 height 대신 minHeight + 패딩 — 큰 글자 배율에서도 행이 늘어나
          // overflow가 발생하지 않는다.
          constraints: const BoxConstraints(minHeight: 52),
          padding: const EdgeInsets.symmetric(
            horizontal: AppGap.md,
            vertical: AppGap.sm,
          ),
          decoration: BoxDecoration(
            border: Border(
              left: BorderSide(
                color: isSelected ? colors.accent : Colors.transparent,
                width: 3,
              ),
              bottom: BorderSide(color: colors.hairline),
            ),
          ),
          child: Row(
            children: [
              SizedBox(
                width: 18,
                child: Text(
                  _bookShortcut(book),
                  style: TextStyle(
                    color: colors.text2,
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    height: 1.2,
                  ),
                ),
              ),
              const SizedBox(width: AppGap.sm),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      book.koreanName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: colors.text,
                        fontSize: 14,
                        fontWeight:
                            isSelected ? FontWeight.w700 : FontWeight.w600,
                        height: 1.25,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      book.englishName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: colors.textMuted,
                        fontSize: 11,
                        height: 1.2,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BibleNumberColumn extends StatelessWidget {
  final int itemCount;
  final int selectedValue;

  /// 범위 선택 끝(절 열에서 사용). null이면 단일 선택(장 열).
  final int? rangeEnd;
  final String suffix;
  final ValueChanged<int> onSelected;

  const _BibleNumberColumn({
    super.key,
    required this.itemCount,
    required this.selectedValue,
    this.rangeEnd,
    required this.suffix,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    final hi = rangeEnd ?? selectedValue;
    final start = selectedValue <= hi ? selectedValue : hi;
    final end = selectedValue <= hi ? hi : selectedValue;
    return ListView.builder(
      padding: EdgeInsets.zero,
      itemCount: itemCount,
      itemBuilder: (context, index) {
        final value = index + 1;
        final isSelected = value >= start && value <= end;
        final isEndpoint = value == start || value == end;

        return Material(
          color: isSelected ? colors.accentSoft : colors.bg,
          child: InkWell(
            onTap: () => onSelected(value),
            child: Container(
              // 고정 itemExtent 대신 minHeight — 글자 배율 증가 시 overflow 방지.
              constraints: const BoxConstraints(minHeight: 40),
              alignment: Alignment.center,
              padding: const EdgeInsets.symmetric(
                horizontal: AppGap.sm,
                vertical: AppGap.sm,
              ),
              decoration: BoxDecoration(
                border: Border(
                  bottom: BorderSide(color: colors.hairline),
                ),
              ),
              child: Text(
                '$value$suffix',
                style: TextStyle(
                  color: isSelected ? colors.text : colors.text2,
                  fontSize: 15,
                  fontWeight: isEndpoint
                      ? FontWeight.w700
                      : (isSelected ? FontWeight.w600 : FontWeight.w500),
                  height: 1.2,
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

class _BibleStatusStrip extends StatelessWidget {
  final String message;

  const _BibleStatusStrip({required this.message});

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: AppGap.md,
        vertical: AppGap.sm,
      ),
      decoration: BoxDecoration(
        color: colors.bgSunken,
        border: Border(top: BorderSide(color: colors.hairline)),
      ),
      child: Row(
        children: [
          Icon(Icons.info_outline, size: 16, color: colors.text2),
          const SizedBox(width: AppGap.sm),
          Expanded(
            child: Text(
              message,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(color: colors.text2, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}

class _BibleSelectionBar extends StatelessWidget {
  final BibleBook selectedBook;
  final int selectedChapter;
  final int verseFrom;
  final int verseTo;
  final bool isBusy;
  final bool isDisabled;
  final VoidCallback onClear;
  final VoidCallback onSearch;

  const _BibleSelectionBar({
    required this.selectedBook,
    required this.selectedChapter,
    required this.verseFrom,
    required this.verseTo,
    required this.isBusy,
    required this.isDisabled,
    required this.onClear,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    final canSearch = !isBusy && !isDisabled;

    return Container(
      decoration: BoxDecoration(
        color: colors.bgElevated,
        border: Border(top: BorderSide(color: colors.hairline)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            AppGap.md,
            AppGap.sm,
            AppGap.md,
            AppGap.sm,
          ),
          child: Row(
            children: [
              Expanded(
                child: Material(
                  color: colors.bg,
                  borderRadius: BorderRadius.circular(AppRadius.box),
                  child: InkWell(
                    key: const Key('bible-selection-bar'),
                    borderRadius: BorderRadius.circular(AppRadius.box),
                    onTap: canSearch ? onSearch : null,
                    child: Container(
                      height: 44,
                      padding: const EdgeInsets.only(left: AppGap.lg),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(AppRadius.box),
                        border: Border.all(color: colors.hairline),
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: isBusy
                                ? Align(
                                    alignment: Alignment.centerLeft,
                                    child: SizedBox(
                                      width: 18,
                                      height: 18,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                        color: colors.accent,
                                      ),
                                    ),
                                  )
                                : Text(
                                    '${selectedBook.koreanName} $selectedChapter장 '
                                    '$verseFrom${verseFrom == verseTo ? '' : '–$verseTo'}절',
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      color: colors.text,
                                      fontSize: 15,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                          ),
                          IconButton(
                            tooltip: '선택 초기화',
                            visualDensity: VisualDensity.compact,
                            onPressed: isBusy ? null : onClear,
                            icon: Icon(
                              Icons.cancel,
                              color: colors.textMuted,
                              size: 18,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: AppGap.md),
              ElevatedButton(
                onPressed: canSearch ? onSearch : null,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppGap.xl,
                    vertical: 0,
                  ),
                  minimumSize: const Size(0, 44),
                ),
                child: const Text('조회'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TocDivider extends StatelessWidget {
  const _TocDivider();

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 1,
      child: ColoredBox(color: context.appColors.hairline),
    );
  }
}

class _BookListEntry {
  final String? section;
  final BibleBook? book;

  const _BookListEntry.section(this.section) : book = null;
  const _BookListEntry.book(this.book) : section = null;
}

List<_BookListEntry> _bookEntries(List<BibleBook> books) {
  final entries = <_BookListEntry>[];
  String? currentSection;

  for (final book in books) {
    final section = _sectionForBook(book.displayOrder);
    if (section != currentSection) {
      entries.add(_BookListEntry.section(section));
      currentSection = section;
    }
    entries.add(_BookListEntry.book(book));
  }

  return entries;
}

String _sectionForBook(int displayOrder) {
  if (displayOrder <= 5) {
    return '율법서';
  }
  if (displayOrder <= 17) {
    return '역사서';
  }
  if (displayOrder <= 22) {
    return '시가서';
  }
  if (displayOrder <= 39) {
    return '예언서';
  }
  if (displayOrder <= 43) {
    return '복음서';
  }
  if (displayOrder == 44) {
    return '역사서';
  }
  if (displayOrder <= 65) {
    return '서신서';
  }
  return '예언서';
}

String _bookShortcut(BibleBook book) {
  if (book.koreanName.isEmpty) {
    return book.code.characters.firstOrNull ?? '';
  }
  return book.koreanName.characters.first;
}
