import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/theme/app_dimens.dart';
import '../../../core/widgets/common_widgets.dart';
import '../models/bible_chapter_counts.dart';
import '../models/bible_models.dart';
import '../providers/bible_providers.dart';

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
  int _verseCount = _defaultVerseCount;
  int _chapterRequestId = 0;

  Object? _error;
  bool _isSearching = false;
  bool _isLoadingChapter = false;
  bool _chapterLoadFailed = false;
  bool _showEnglish = false;

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
    final verseTo = _verseTo < _verseFrom ? _verseFrom : _verseTo;

    setState(() {
      _isSearching = true;
      _error = null;
    });

    try {
      final range = await ref.read(bibleRepositoryProvider).getVerses(
            bookCode: bookCode,
            chapter: _selectedChapter,
            verseFrom: _verseFrom,
            verseTo: verseTo,
          );
      if (!mounted) {
        return;
      }
      _showResultSheet(range);
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

  void _showResultSheet(BibleVerseRange range) {
    final colors = context.appColors;
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: colors.bgElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppRadius.sheet)),
      ),
      builder: (sheetContext) {
        return StatefulBuilder(
          builder: (context, setSheetState) {
            return SizedBox(
              height: MediaQuery.sizeOf(context).height * 0.72,
              child: _BibleResultPane(
                range: range,
                error: null,
                showEnglish: _showEnglish,
                onShowEnglishChanged: (selected) {
                  setState(() => _showEnglish = selected);
                  setSheetState(() {});
                },
                onRetry: () {
                  Navigator.of(sheetContext).pop();
                  _search(_selectedBookCode ?? range.book.code);
                },
              ),
            );
          },
        );
      },
    );
  }

  void _selectBook(BibleBook book) {
    setState(() {
      _selectedBookCode = book.code;
      _selectedChapter = 1;
      _verseFrom = 1;
      _verseTo = 1;
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
      _verseCount = _defaultVerseCount;
      _chapterLoadFailed = false;
      _error = null;
    });
    _loadChapterVerseCount(bookCode, chapter);
  }

  void _selectVerse(int verse) {
    setState(() {
      _verseFrom = verse;
      _verseTo = verse;
      _error = null;
    });
  }

  void _clearVerseSelection() {
    setState(() {
      _verseFrom = 1;
      _verseTo = 1;
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
            selectedVerse: _verseFrom,
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
  final int selectedVerse;
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
    required this.selectedVerse,
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
            selectedVerse: selectedVerse,
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
  final int selectedVerse;
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
    required this.selectedVerse,
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
                  selectedValue: selectedVerse,
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
  final String suffix;
  final ValueChanged<int> onSelected;

  const _BibleNumberColumn({
    super.key,
    required this.itemCount,
    required this.selectedValue,
    required this.suffix,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    return ListView.builder(
      padding: EdgeInsets.zero,
      itemCount: itemCount,
      itemBuilder: (context, index) {
        final value = index + 1;
        final isSelected = value == selectedValue;

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
                  fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
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
  final bool isBusy;
  final bool isDisabled;
  final VoidCallback onClear;
  final VoidCallback onSearch;

  const _BibleSelectionBar({
    required this.selectedBook,
    required this.selectedChapter,
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
                                    '${selectedBook.koreanName} $selectedChapter장',
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

class _BibleResultPane extends StatelessWidget {
  final BibleVerseRange? range;
  final Object? error;
  final bool showEnglish;
  final ValueChanged<bool> onShowEnglishChanged;
  final VoidCallback onRetry;

  const _BibleResultPane({
    required this.range,
    required this.error,
    required this.showEnglish,
    required this.onShowEnglishChanged,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.appColors;

    if (error != null) {
      return ErrorView(
        message: '성경본문을 불러오지 못했습니다. 다시 시도해 주세요.',
        onRetry: onRetry,
      );
    }

    if (range == null) {
      return const EmptyView(
        message: '조회할 성경본문을 선택해 주세요.',
        icon: Icons.menu_book_outlined,
      );
    }

    return ListView(
      padding: const EdgeInsets.fromLTRB(AppGap.xl, AppGap.sm, AppGap.xl, AppGap.xxl),
      children: [
        Text(
          '${range!.book.koreanName} ${range!.book.chapter}장',
          style: theme.textTheme.headlineMedium,
        ),
        const SizedBox(height: AppGap.md),
        Row(
          children: [
            FilterChip(
              key: const Key('bible-browser-english-toggle'),
              selected: showEnglish,
              onSelected: onShowEnglishChanged,
              label: const Text('영어'),
            ),
          ],
        ),
        if (showEnglish) ...[
          const SizedBox(height: AppGap.sm),
          Text(
            range!.book.englishName,
            style: theme.textTheme.bodyMedium?.copyWith(color: colors.textMuted),
          ),
        ],
        const SizedBox(height: AppGap.lg),
        Divider(color: colors.hairline, height: 1),
        const SizedBox(height: AppGap.lg),
        for (final verse in range!.verses)
          _BibleVerseTile(
            verse: verse,
            showEnglish: showEnglish,
          ),
      ],
    );
  }
}

class _BibleVerseTile extends StatelessWidget {
  final BibleVerse verse;
  final bool showEnglish;

  const _BibleVerseTile({
    required this.verse,
    required this.showEnglish,
  });

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
            Text(
              koreanText,
              style: theme.textTheme.bodyLarge,
            ),
          if (showEnglish && englishText != null && englishText.isNotEmpty) ...[
            const SizedBox(height: AppGap.sm),
            Text(
              englishText,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colors.textMuted,
              ),
            ),
          ],
        ],
      ),
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
