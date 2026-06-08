import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../../note/providers/note_providers.dart';
import '../models/bible_models.dart';
import '../providers/bible_providers.dart';

class BibleBrowserScreen extends ConsumerStatefulWidget {
  final VoidCallback? onOpenSermonNotes;

  const BibleBrowserScreen({super.key, this.onOpenSermonNotes});

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

  BibleVerseRange? _range;
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
        _range = null;
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
      setState(() => _range = range);
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _error = error;
        _range = null;
      });
    } finally {
      if (mounted) {
        setState(() => _isSearching = false);
      }
    }
  }

  void _openSermonNotes(BibleVerseRange range) {
    if (range.verses.isEmpty) {
      return;
    }
    ref.read(noteCategoryFilterProvider.notifier).state = 'SERMON';

    final onOpenSermonNotes = widget.onOpenSermonNotes;
    if (onOpenSermonNotes != null) {
      onOpenSermonNotes();
      return;
    }

    Navigator.of(context).pushNamed(AppRouter.noteList);
  }

  void _selectBook(BibleBook book) {
    final chapterCount = _chapterCountFor(book);
    setState(() {
      _selectedBookCode = book.code;
      _selectedChapter = _selectedChapter.clamp(1, chapterCount);
      _verseFrom = 1;
      _verseTo = 1;
      _verseCount = _defaultVerseCount;
      _chapterLoadFailed = false;
      _range = null;
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
      _range = null;
      _error = null;
    });
    _loadChapterVerseCount(bookCode, chapter);
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
      appBar: AppBar(title: const Text('성경본문')),
      body: FutureBuilder<List<BibleBook>>(
        future: _booksFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const LoadingView(message: '성경 권 목록을 불러오는 중입니다.');
          }

          if (snapshot.hasError) {
            return ErrorView(
              message: '성경 권 목록을 불러오지 못했습니다.\n${snapshot.error}',
              onRetry: () {
                setState(() {
                  _booksFuture = ref.read(bibleRepositoryProvider).getBooks();
                  _error = null;
                  _range = null;
                });
              },
            );
          }

          final books = snapshot.data ?? const <BibleBook>[];
          if (books.isEmpty) {
            return const EmptyView(message: '성경 권 목록이 없습니다.');
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
            range: _range,
            error: _error,
            isSearching: _isSearching,
            isLoadingChapter: _isLoadingChapter,
            isChapterLoadFailed: _chapterLoadFailed,
            showEnglish: _showEnglish,
            onBookChanged: _selectBook,
            onChapterChanged: (chapter) =>
                _selectChapter(selectedBook.code, chapter),
            onVerseFromChanged: (verse) {
              setState(() {
                _verseFrom = verse;
                if (_verseTo < verse) {
                  _verseTo = verse;
                }
              });
            },
            onVerseToChanged: (verse) => setState(() => _verseTo = verse),
            onShowEnglishChanged: (selected) {
              setState(() => _showEnglish = selected);
            },
            onSearch: () => _search(selectedBook.code),
            onOpenSermonNotes: _openSermonNotes,
          );
        },
      ),
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
  final BibleVerseRange? range;
  final Object? error;
  final bool isSearching;
  final bool isLoadingChapter;
  final bool isChapterLoadFailed;
  final bool showEnglish;
  final ValueChanged<BibleBook> onBookChanged;
  final ValueChanged<int> onChapterChanged;
  final ValueChanged<int> onVerseFromChanged;
  final ValueChanged<int> onVerseToChanged;
  final ValueChanged<bool> onShowEnglishChanged;
  final VoidCallback onSearch;
  final ValueChanged<BibleVerseRange> onOpenSermonNotes;

  const _BibleBrowserContent({
    required this.books,
    required this.selectedBook,
    required this.selectedChapter,
    required this.chapterCount,
    required this.verseFrom,
    required this.verseTo,
    required this.verseCount,
    required this.range,
    required this.error,
    required this.isSearching,
    required this.isLoadingChapter,
    required this.isChapterLoadFailed,
    required this.showEnglish,
    required this.onBookChanged,
    required this.onChapterChanged,
    required this.onVerseFromChanged,
    required this.onVerseToChanged,
    required this.onShowEnglishChanged,
    required this.onSearch,
    required this.onOpenSermonNotes,
  });

  @override
  Widget build(BuildContext context) {
    final selectorFlex = range == null ? 6 : 4;
    final resultFlex = range == null ? 4 : 6;

    return SafeArea(
      child: Column(
        children: [
          Expanded(
            flex: selectorFlex,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 6),
              child: _BibleRangePicker(
                books: books,
                selectedBook: selectedBook,
                selectedChapter: selectedChapter,
                chapterCount: chapterCount,
                verseFrom: verseFrom,
                verseTo: verseTo,
                verseCount: verseCount,
                isLoadingChapter: isLoadingChapter,
                isChapterLoadFailed: isChapterLoadFailed,
                onBookChanged: onBookChanged,
                onChapterChanged: onChapterChanged,
                onVerseFromChanged: onVerseFromChanged,
                onVerseToChanged: onVerseToChanged,
                onSearch: onSearch,
                isSearching: isSearching,
              ),
            ),
          ),
          const Divider(height: 1),
          Expanded(
            flex: resultFlex,
            child: _BibleResultPane(
              range: range,
              error: error,
              showEnglish: showEnglish,
              onShowEnglishChanged: onShowEnglishChanged,
              onOpenSermonNotes: onOpenSermonNotes,
              onRetry: onSearch,
            ),
          ),
        ],
      ),
    );
  }
}

class _BibleRangePicker extends StatelessWidget {
  final List<BibleBook> books;
  final BibleBook selectedBook;
  final int selectedChapter;
  final int chapterCount;
  final int verseFrom;
  final int verseTo;
  final int verseCount;
  final bool isLoadingChapter;
  final bool isChapterLoadFailed;
  final ValueChanged<BibleBook> onBookChanged;
  final ValueChanged<int> onChapterChanged;
  final ValueChanged<int> onVerseFromChanged;
  final ValueChanged<int> onVerseToChanged;
  final VoidCallback onSearch;
  final bool isSearching;

  const _BibleRangePicker({
    required this.books,
    required this.selectedBook,
    required this.selectedChapter,
    required this.chapterCount,
    required this.verseFrom,
    required this.verseTo,
    required this.verseCount,
    required this.isLoadingChapter,
    required this.isChapterLoadFailed,
    required this.onBookChanged,
    required this.onChapterChanged,
    required this.onVerseFromChanged,
    required this.onVerseToChanged,
    required this.onSearch,
    required this.isSearching,
  });

  @override
  Widget build(BuildContext context) {
    final selectedBookIndex = books.indexWhere(
      (book) => book.code == selectedBook.code,
    );
    final safeChapterCount = chapterCount < 1 ? 1 : chapterCount;
    final safeVerseCount = verseCount < 1 ? 1 : verseCount;
    final endVerseItems = [
      for (var verse = verseFrom; verse <= safeVerseCount; verse++) verse,
    ];

    return Column(
      children: [
        Expanded(
          child: Row(
            children: [
              Expanded(
                flex: 3,
                child: _PickerColumn(
                  key: const Key('bible-book-picker'),
                  label: '성경',
                  selectedIndex: selectedBookIndex < 0 ? 0 : selectedBookIndex,
                  items: [for (final book in books) book.koreanName],
                  onSelectedIndexChanged: (index) =>
                      onBookChanged(books[index]),
                ),
              ),
              Expanded(
                flex: 2,
                child: _PickerColumn(
                  key: const Key('bible-chapter-picker'),
                  label: '장',
                  selectedIndex: selectedChapter - 1,
                  items: [
                    for (var chapter = 1;
                        chapter <= safeChapterCount;
                        chapter++)
                      '$chapter'
                  ],
                  onSelectedIndexChanged: (index) =>
                      onChapterChanged(index + 1),
                ),
              ),
              Expanded(
                flex: 2,
                child: _PickerColumn(
                  key: const Key('bible-verse-from-picker'),
                  label: '시작절',
                  selectedIndex: verseFrom - 1,
                  items: [
                    for (var verse = 1; verse <= safeVerseCount; verse++)
                      '$verse'
                  ],
                  onSelectedIndexChanged: (index) =>
                      onVerseFromChanged(index + 1),
                ),
              ),
              Expanded(
                flex: 2,
                child: _PickerColumn(
                  key: const Key('bible-verse-to-picker'),
                  label: '끝절',
                  selectedIndex: endVerseItems.indexOf(verseTo).clamp(
                        0,
                        endVerseItems.length - 1,
                      ),
                  items: [for (final verse in endVerseItems) '$verse'],
                  onSelectedIndexChanged: (index) =>
                      onVerseToChanged(endVerseItems[index]),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: Text(
                isLoadingChapter
                    ? '절 목록을 맞추는 중입니다.'
                    : isChapterLoadFailed
                        ? '절 목록을 불러오지 못했습니다.'
                        : '${selectedBook.koreanName} $selectedChapter:$verseFrom-$verseTo',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.labelLarge,
              ),
            ),
            const SizedBox(width: 10),
            FilledButton.icon(
              onPressed:
                  (isSearching || isLoadingChapter || isChapterLoadFailed)
                      ? null
                      : onSearch,
              icon: isSearching
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.search),
              label: const Text('조회'),
            ),
          ],
        ),
      ],
    );
  }
}

class _PickerColumn extends StatelessWidget {
  final String label;
  final List<String> items;
  final int selectedIndex;
  final ValueChanged<int> onSelectedIndexChanged;

  const _PickerColumn({
    super.key,
    required this.label,
    required this.items,
    required this.selectedIndex,
    required this.onSelectedIndexChanged,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      children: [
        Text(
          label,
          style: theme.textTheme.labelMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 4),
        Expanded(
          child: Stack(
            alignment: Alignment.center,
            children: [
              Container(
                height: 38,
                decoration: BoxDecoration(
                  color: theme.colorScheme.primaryContainer.withValues(
                    alpha: 0.45,
                  ),
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              ListWheelScrollView.useDelegate(
                controller: FixedExtentScrollController(
                  initialItem: selectedIndex.clamp(0, items.length - 1),
                ),
                itemExtent: 38,
                diameterRatio: 1.35,
                physics: const FixedExtentScrollPhysics(),
                overAndUnderCenterOpacity: 0.42,
                onSelectedItemChanged: onSelectedIndexChanged,
                childDelegate: ListWheelChildBuilderDelegate(
                  childCount: items.length,
                  builder: (context, index) {
                    return Center(
                      child: Text(
                        items[index],
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        textAlign: TextAlign.center,
                        style: theme.textTheme.titleSmall?.copyWith(
                          fontWeight: index == selectedIndex
                              ? FontWeight.w700
                              : FontWeight.w500,
                        ),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _BibleResultPane extends StatelessWidget {
  final BibleVerseRange? range;
  final Object? error;
  final bool showEnglish;
  final ValueChanged<bool> onShowEnglishChanged;
  final ValueChanged<BibleVerseRange> onOpenSermonNotes;
  final VoidCallback onRetry;

  const _BibleResultPane({
    required this.range,
    required this.error,
    required this.showEnglish,
    required this.onShowEnglishChanged,
    required this.onOpenSermonNotes,
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
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 28),
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
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            key: const Key('bible-browser-sermon-note-button'),
            onPressed:
                range!.verses.isEmpty ? null : () => onOpenSermonNotes(range!),
            icon: const Icon(Icons.edit_note_outlined),
            label: const Text('노트'),
          ),
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
