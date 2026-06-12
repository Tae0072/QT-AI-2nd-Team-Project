import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/note_detail_screen.dart';

/// N-04 상세에서 [수정] 버튼 노출 규칙(QA ⑫): 자유노트·설교는 수정 가능, QT(묵상)는 제외.
void main() {
  NoteDetail detail(String category) => NoteDetail(
        id: 7,
        category: category,
        title: '노트',
        body: '본문',
        status: 'SAVED',
        visibility: 'PRIVATE',
        shared: false,
        verses: const [],
      );

  Future<void> pump(WidgetTester tester, String category) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          noteDetailProvider(7).overrideWith((ref) async => detail(category)),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: const NoteDetailScreen(noteId: 7),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('설교(SERMON) 노트는 수정 버튼이 노출된다', (tester) async {
    await pump(tester, 'SERMON');
    expect(find.byIcon(Icons.edit_outlined), findsOneWidget);
  });

  testWidgets('자유노트(기도)는 수정 버튼이 노출된다', (tester) async {
    await pump(tester, 'PRAYER');
    expect(find.byIcon(Icons.edit_outlined), findsOneWidget);
  });

  testWidgets('QT(MEDITATION) 노트는 수정 버튼이 없다(4섹션/표시 정리 선행)',
      (tester) async {
    await pump(tester, 'MEDITATION');
    expect(find.byIcon(Icons.edit_outlined), findsNothing);
  });
}
