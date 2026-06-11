import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../models/bible_models.dart';
import '../providers/bible_providers.dart';

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
        _error = StateError('절 목록을 불러오지 못했습니다. 다시 시도해 주세요. ($error)');
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
        SnackBar(content: Text('성경본문을 불러오지 못했습니다.\n$error')),
      );
    } finally {
      if (mounted) {
        setState(() => _isSearching = false);
      }
    }
  }

  void _showResultSheet(BibleVerseRange range) {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      isScrollControlled: true,
      useSafeArea: true,
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
    final index = book.displayOrder - 1;
    if (index < 0 || index >= _chapterCounts.length) {
      return 150;
    }
    return _chapterCounts[index];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFE9EBEF),
      body: FutureBuilder<List<BibleBook>>(
        future: _booksFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return _BibleTocShell(
              onClose: () => Navigator.of(context).maybePop(),
              child: const LoadingView(message: '성경 권 목록을 불러오는 중입니다.'),
            );
          }

          if (snapshot.hasError) {
            return _BibleTocShell(
              onClose: () => Navigator.of(context).maybePop(),
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
            return _BibleTocShell(
              onClose: () => Navigator.of(context).maybePop(),
              child: const EmptyView(message: '성경 권 목록이 없습니다.'),
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
            onClose: () => Navigator.of(context).maybePop(),
            onSearch: () => _search(selectedBook.code),
          );
        },
      ),
    );
  }
}

class _BibleTocShell extends StatelessWidget {
  final VoidCallback onClose;
  final Widget child;

  const _BibleTocShell({
    required this.onClose,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _BibleTocHeader(onClose: onClose),
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
  final VoidCallback onClose;
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
    required this.onClose,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _BibleTocHeader(onClose: onClose),
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
          _BibleStatusStrip(
            message: error == null ? '절 목록을 불러오지 못했습니다.' : '절 목록을 불러오지 못했습니다.',
          ),
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
  final VoidCallback onClose;

  const _BibleTocHeader({required this.onClose});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF506984),
      child: SafeArea(
        bottom: false,
        child: SizedBox(
          height: 56,
          child: Row(
            children: [
              const SizedBox(width: 16),
              const Text(
                '목차검색 :: 성경본문',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(width: 4),
              const Icon(
                Icons.info_outline,
                color: Colors.white70,
                size: 14,
              ),
              const Spacer(),
              IconButton(
                tooltip: '닫기',
                onPressed: onClose,
                icon: const Icon(Icons.close, color: Colors.white),
              ),
              const SizedBox(width: 8),
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
    final safeChapterCount = chapterCount < 1 ? 1 : chapterCount;
    final safeVerseCount = verseCount < 1 ? 1 : verseCount;

    return ColoredBox(
      color: const Color(0xFFF5F6F7),
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
                  const Align(
                    alignment: Alignment.topCenter,
                    child: LinearProgressIndicator(minHeight: 2),
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
    return Container(
      height: 18,
      alignment: Alignment.centerLeft,
      padding: const EdgeInsets.symmetric(horizontal: 4),
      color: const Color(0xFFE2E5EA),
      child: Text(
        label,
        style: const TextStyle(
          color: Color(0xFF667085),
          fontSize: 10,
          fontWeight: FontWeight.w700,
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
    return Material(
      color: isSelected ? const Color(0xFFB9BAA6) : Colors.white,
      child: InkWell(
        onTap: onTap,
        child: Container(
          height: 45,
          padding: const EdgeInsets.only(left: 12, right: 8),
          decoration: const BoxDecoration(
            border: Border(
              bottom: BorderSide(color: Color(0xFFE1E4EA), width: 1),
            ),
          ),
          child: Row(
            children: [
              SizedBox(
                width: 20,
                child: Text(
                  _bookShortcut(book),
                  style: const TextStyle(
                    color: Color(0xFF2F6FE4),
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      book.koreanName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: Color(0xFF323A45),
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    Text(
                      book.englishName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: Color(0xFF848B95),
                        fontSize: 11,
                        height: 1.1,
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
    return ListView.builder(
      padding: EdgeInsets.zero,
      itemExtent: 34,
      itemCount: itemCount,
      itemBuilder: (context, index) {
        final value = index + 1;
        final isSelected = value == selectedValue;

        return Material(
          color: isSelected ? const Color(0xFFB9BAA6) : Colors.white,
          child: InkWell(
            onTap: () => onSelected(value),
            child: Container(
              alignment: Alignment.centerRight,
              padding: const EdgeInsets.only(right: 18),
              decoration: const BoxDecoration(
                border: Border(
                  bottom: BorderSide(color: Color(0xFFE1E4EA), width: 1),
                ),
              ),
              child: Text(
                '$value$suffix',
                style: TextStyle(
                  color: isSelected ? Colors.white : const Color(0xFF2F3640),
                  fontSize: 15,
                  fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
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
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      color: const Color(0xFFFFF4E5),
      child: Text(
        message,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: const TextStyle(color: Color(0xFF9A5B00), fontSize: 12),
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
    final canSearch = !isBusy && !isDisabled;

    return SafeArea(
      top: false,
      child: Container(
        height: 46,
        padding: const EdgeInsets.fromLTRB(8, 6, 10, 6),
        color: const Color(0xFFE9EBEF),
        child: Row(
          children: [
            Expanded(
              child: Material(
                color: Colors.white,
                child: InkWell(
                  key: const Key('bible-selection-bar'),
                  onTap: canSearch ? onSearch : null,
                  child: Row(
                    children: [
                      Expanded(
                        child: Center(
                          child: isBusy
                              ? const SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                  ),
                                )
                              : Text(
                                  '${selectedBook.koreanName} $selectedChapter장',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    color: Color(0xFF006DFF),
                                    fontSize: 14,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                        ),
                      ),
                      IconButton(
                        tooltip: '선택 초기화',
                        visualDensity: VisualDensity.compact,
                        onPressed: isBusy ? null : onClear,
                        icon: const Icon(
                          Icons.cancel,
                          color: Color(0xFFB4B8BE),
                          size: 18,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(width: 10),
            IconButton(
              tooltip: '본문 조회',
              onPressed: canSearch ? onSearch : null,
              icon: const Icon(Icons.keyboard_alt_outlined),
              color: const Color(0xFF4E5968),
            ),
          ],
        ),
      ),
    );
  }
}

class _TocDivider extends StatelessWidget {
  const _TocDivider();

  @override
  Widget build(BuildContext context) {
    return const SizedBox(
      width: 1,
      child: ColoredBox(color: Color(0xFFD5DAE1)),
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

    if (error != null) {
      return ErrorView(
        message: '성경본문을 불러오지 못했습니다.\n$error',
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
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
      children: [
        Text(
          '${range!.book.koreanName} ${range!.book.chapter}장',
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 6),
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
          const SizedBox(height: 6),
          Text(
            range!.book.englishName,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
        const SizedBox(height: 18),
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

const _chapterCounts = [
  50,
  40,
  27,
  36,
  34,
  24,
  21,
  4,
  31,
  24,
  22,
  25,
  29,
  36,
  10,
  13,
  10,
  42,
  150,
  31,
  12,
  8,
  66,
  52,
  5,
  48,
  12,
  14,
  3,
  9,
  1,
  4,
  7,
  3,
  3,
  3,
  2,
  14,
  4,
  28,
  16,
  24,
  21,
  28,
  16,
  16,
  13,
  6,
  6,
  4,
  4,
  5,
  3,
  6,
  4,
  3,
  1,
  13,
  5,
  5,
  3,
  5,
  1,
  1,
  1,
  22,
];
