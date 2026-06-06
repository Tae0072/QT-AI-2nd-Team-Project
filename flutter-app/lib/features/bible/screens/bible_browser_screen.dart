import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../models/bible_models.dart';
import '../providers/bible_providers.dart';

class BibleBrowserScreen extends ConsumerStatefulWidget {
  const BibleBrowserScreen({super.key});

  @override
  ConsumerState<BibleBrowserScreen> createState() => _BibleBrowserScreenState();
}

class _BibleBrowserScreenState extends ConsumerState<BibleBrowserScreen> {
  final _chapterController = TextEditingController(text: '1');
  final _verseFromController = TextEditingController(text: '1');
  final _verseToController = TextEditingController();

  String? _selectedBookCode;
  BibleVerseRange? _range;
  Object? _error;
  bool _isSearching = false;

  @override
  void dispose() {
    _chapterController.dispose();
    _verseFromController.dispose();
    _verseToController.dispose();
    super.dispose();
  }

  Future<void> _search(String bookCode) async {
    final l = AppLocalizations.of(context);
    final chapter = _parsePositiveInt(_chapterController.text);
    final verseFrom = _parsePositiveInt(_verseFromController.text);
    final verseTo = _verseToController.text.trim().isEmpty
        ? verseFrom
        : _parsePositiveInt(_verseToController.text);

    if (chapter == null || verseFrom == null || verseTo == null) {
      setState(() {
        _error = StateError(l.bibleChapterError);
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
    final l = AppLocalizations.of(context);
    final booksAsync = ref.watch(bibleBooksProvider);
    return Scaffold(
      appBar: AppBar(title: Text(l.bibleBrowserTitle)),
      body: booksAsync.whenOrDefault(
        loading: () => LoadingView(message: l.bibleBooksLoading),
        error: (e, _) => ErrorView(
          message: '${l.bibleBooksLoadError}\n$e',
          onRetry: () {
            ref.invalidate(bibleBooksProvider);
            setState(() {
              _error = null;
              _range = null;
            });
          },
        ),
        data: (books) {
          if (books.isEmpty) {
            return EmptyView(message: l.bibleBooksEmpty);
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
    final l = AppLocalizations.of(context);

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
      children: [
        DropdownButtonFormField<String>(
          initialValue: selectedBookCode,
          decoration: InputDecoration(labelText: l.navBible),
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
                labelText: l.bibleChapter,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _NumberField(
                controller: verseFromController,
                labelText: l.bibleVerseFrom,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _NumberField(
                controller: verseToController,
                labelText: l.bibleVerseTo,
                hintText: l.bibleSame,
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
            label: Text(l.bibleSearch),
          ),
        ),
        const SizedBox(height: 24),
        if (error != null)
          ErrorView(
            message: '${l.bibleVersesLoadError}\n$error',
            onRetry: onSearch,
          )
        else if (range == null)
          EmptyView(
            message: l.bibleSelectPrompt,
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
