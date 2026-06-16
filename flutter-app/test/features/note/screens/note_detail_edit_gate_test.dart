import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/note_detail_screen.dart';

/// N-04 상세: 전 카테고리가 단일 body라 수정 버튼이 노출되고 body가 표시된다(QA ⑫, QT 단일 body 확정).
void main() {
  NoteDetail detail(String category) => NoteDetail(
        id: 7,
        category: category,
        title: '노트',
        body: '오늘의 묵상 기록',
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

  for (final category in ['SERMON', 'PRAYER', 'MEDITATION']) {
    testWidgets('$category 노트는 수정 버튼이 노출된다', (tester) async {
      await pump(tester, category);
      expect(find.byIcon(Icons.edit_outlined), findsOneWidget);
    });
  }

  testWidgets('QT(MEDITATION) 상세는 4섹션이 아니라 body를 표시한다', (tester) async {
    await pump(tester, 'MEDITATION');
    // body가 렌더된다(과거엔 빈 4섹션을 표시해 본문이 안 보였음).
    expect(find.textContaining('오늘의 묵상 기록'), findsWidgets);
  });
}
