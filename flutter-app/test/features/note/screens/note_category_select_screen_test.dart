import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/note_category_select_screen.dart';
import 'package:qtai_app/features/note/services/note_repository.dart';
import 'package:qtai_app/l10n/app_localizations.dart';

class _FakeNoteRepository extends NoteRepository {
  _FakeNoteRepository(this.categories) : super(Dio());

  final List<NoteCategoryOption> categories;

  @override
  Future<List<NoteCategoryOption>> getNoteCategories() async {
    return categories;
  }
}

void main() {
  testWidgets('renders writable note categories from API response',
      (tester) async {
    Object? pushedArgs;

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          noteRepositoryProvider.overrideWithValue(
            _FakeNoteRepository([
              const NoteCategoryOption(
                category: 'PRAYER',
                label: 'Server Prayer',
                requiresQtPassage: false,
                supportsVerseSelection: false,
                writableFromList: true,
              ),
              const NoteCategoryOption(
                category: 'SERMON',
                label: 'Server Sermon',
                requiresQtPassage: false,
                supportsVerseSelection: true,
                writableFromList: false,
              ),
            ]),
          ),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: const NoteCategorySelectScreen(),
          onGenerateRoute: (settings) {
            pushedArgs = settings.arguments;
            return MaterialPageRoute<void>(
              builder: (_) => const SizedBox.shrink(),
              settings: settings,
            );
          },
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Server Prayer'), findsOneWidget);
    expect(find.text('Server Sermon'), findsNothing);

    await tester.tap(find.text('Server Prayer'));
    await tester.pumpAndSettle();

    expect(pushedArgs, isA<NoteEditArgs>());
    expect((pushedArgs as NoteEditArgs).category, 'PRAYER');
  });

  testWidgets('falls back to local writable categories when API fails',
      (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          noteCategoriesProvider.overrideWith(
            (ref) => Future<List<NoteCategoryOption>>.error(Exception('boom')),
          ),
        ],
        child: const MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: Locale('ko'),
          home: NoteCategorySelectScreen(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text(noteCategoryLabel('PRAYER')), findsOneWidget);
    expect(find.text(noteCategoryLabel('REPENTANCE')), findsOneWidget);
    expect(find.text(noteCategoryLabel('GRATITUDE')), findsOneWidget);
  });
}
