import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
  late Future<List<BibleBook>> _booksFuture;
  final _chapterController = TextEditingController(text: '1');
  final _verseFromController = TextEditingController(text: '1');
  final _verseToController = TextEditingController();

  String? _selectedBookCode;
  BibleVerseRange? _range;
  Object? _error;
  bool _isSearching = false;

  @override
  void initState() {
    super.initState();
    _booksFuture = ref.read(bibleRepositoryProvider).getBooks();
  }

  @override
  void dispose() {
    _chapterController.dispose();
    _verseFromController.dispose();
    _verseToController.dispose();
    super.dispose();
  }

  Future<void> _search(String bookCode) async {
    final chapter = _parsePositiveInt(_chapterController.text);
    final verseFrom = _parsePositiveInt(_verseFromController.text);
    final verseTo = _verseToController.text.trim().isEmpty
        ? verseFrom
        : _parsePositiveInt(_verseToController.text);

    if (chapter == null || verseFrom == null || verseTo == null) {
      setState(() {
        _error = StateError('장과 절은 1 이상의 숫자로 입력해 주세요.');
        _range = null;
      });
      return;
    }

    setState(() {
      _isSearching = true;
      _error = null;
    });

    try {
      final range = await ref.read(bibleRepositoryProvider).getVerses(
            bookCode: bookCode,
            chapter: chapter,
            verseFrom: verseFrom,
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

  int? _parsePositiveInt(String value) {
    final parsed = int.tryParse(value.trim());
    if (parsed == null || parsed < 1) {
      return null;
    }
    return parsed;
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
          return _BibleBrowserContent(
            books: books,
            selectedBookCode: selectedBookCode,
            chapterController: _chapterController,
            verseFromController: _verseFromController,
            verseToController: _verseToController,
            range: _range,
            error: _error,
            isSearching: _isSearching,
            onBookChanged: (value) {
              if (value == null) {
                return;
              }
              setState(() => _selectedBookCode = value);
            },
            onSearch: () => _search(selectedBookCode),
          );
        },
      ),
    );
  }
}

class _BibleBrowserContent extends StatelessWidget {
  final List<BibleBook> books;
  final String selectedBookCode;
  final TextEditingController chapterController;
  final TextEditingController verseFromController;
  final TextEditingController verseToController;
  final BibleVerseRange? range;
  final Object? error;
  final bool isSearching;
  final ValueChanged<String?> onBookChanged;
  final VoidCallback onSearch;

  const _BibleBrowserContent({
    required this.books,
    required this.selectedBookCode,
    required this.chapterController,
    required this.verseFromController,
    required this.verseToController,
    required this.range,
    required this.error,
    required this.isSearching,
    required this.onBookChanged,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
      children: [
        DropdownButtonFormField<String>(
          initialValue: selectedBookCode,
          decoration: const InputDecoration(labelText: '성경'),
          items: [
            for (final book in books)
              DropdownMenuItem(
                value: book.code,
                child: Text(book.koreanName),
              ),
          ],
          onChanged: onBookChanged,
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _NumberField(
                key: const Key('bible-chapter-input'),
                controller: chapterController,
                labelText: '장',
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _NumberField(
                controller: verseFromController,
                labelText: '시작절',
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _NumberField(
                controller: verseToController,
                labelText: '끝절',
                hintText: '동일',
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Align(
          alignment: Alignment.centerRight,
          child: FilledButton.icon(
            onPressed: isSearching ? null : onSearch,
            icon: isSearching
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.search),
            label: const Text('조회'),
          ),
        ),
        const SizedBox(height: 24),
        if (error != null)
          ErrorView(
            message: '성경본문을 불러오지 못했습니다.\n$error',
            onRetry: onSearch,
          )
        else if (range == null)
          const EmptyView(
            message: '조회할 성경본문을 선택해 주세요.',
            icon: Icons.menu_book_outlined,
          )
        else ...[
          Text(
            '${range!.book.koreanName} ${range!.book.chapter}장',
            style: theme.textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            range!.book.englishName,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 18),
          for (final verse in range!.verses) _BibleVerseTile(verse: verse),
        ],
      ],
    );
  }
}

class _NumberField extends StatelessWidget {
  final TextEditingController controller;
  final String labelText;
  final String? hintText;

  const _NumberField({
    super.key,
    required this.controller,
    required this.labelText,
    this.hintText,
  });

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      keyboardType: TextInputType.number,
      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
      decoration: InputDecoration(
        labelText: labelText,
        hintText: hintText,
      ),
    );
  }
}

class _BibleVerseTile extends StatelessWidget {
  final BibleVerse verse;

  const _BibleVerseTile({required this.verse});

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
          if (englishText != null && englishText.isNotEmpty) ...[
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
