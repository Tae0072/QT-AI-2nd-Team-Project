import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/widgets/note_share_sheet.dart';

/// 4섹션 제거 후 공유 시트가 전 카테고리(QT 포함) 단일 body를 미리보기에 표시하는지 검증한다.
void main() {
  NoteDetail detail(String category) => NoteDetail(
        id: 1,
        category: category,
        title: '묵상 제목',
        body: '오늘의 묵상 본문입니다',
        status: 'SAVED',
        visibility: 'PRIVATE',
        shared: false,
        verses: const [],
      );

  Future<void> openSheet(WidgetTester tester, String category) async {
    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        home: Builder(
          builder: (context) => Scaffold(
            body: ElevatedButton(
              onPressed: () => showNoteShareSheet(context, detail(category)),
              child: const Text('open'),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
  }

  testWidgets('QT(MEDITATION) 공유 미리보기 카드는 body를 표시한다', (tester) async {
    await openSheet(tester, 'MEDITATION');
    expect(find.textContaining('오늘의 묵상 본문입니다'), findsWidgets);
  });

  testWidgets('설교(SERMON) 공유 미리보기 카드도 body를 표시한다', (tester) async {
    await openSheet(tester, 'SERMON');
    expect(find.textContaining('오늘의 묵상 본문입니다'), findsWidgets);
  });
}
