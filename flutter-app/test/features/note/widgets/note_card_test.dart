import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/widgets/note_card.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        home: Scaffold(body: child),
      );

  testWidgets('제목·카테고리 배지·날짜·나눔 배지를 표시하고 탭을 전달한다',
      (tester) async {
    var tapped = false;
    final item = NoteListItem(
      id: 1,
      category: 'MEDITATION',
      title: '부끄러운 송사',
      status: 'SAVED',
      visibility: 'PUBLIC',
      qtDate: '2026-06-10',
      shared: true,
      updatedAt: DateTime(2026, 6, 10),
    );

    await tester.pumpWidget(
        wrap(NoteCard(item: item, onTap: () => tapped = true)));
    await tester.pumpAndSettle();

    expect(find.text('부끄러운 송사'), findsOneWidget);
    expect(find.text('QT'), findsOneWidget); // MEDITATION → 'QT'
    expect(find.text('6월 10일'), findsOneWidget); // qtDate 변환
    expect(find.text('나눔'), findsOneWidget); // shared 배지

    await tester.tap(find.byType(NoteCard));
    expect(tapped, isTrue);
  });

  testWidgets('제목이 비면 기본 라벨, 임시저장은 표시, 미공개는 나눔 배지 없음',
      (tester) async {
    final item = NoteListItem(
      id: 2,
      category: 'PRAYER',
      title: '',
      status: 'DRAFT',
      visibility: 'PRIVATE',
      shared: false,
      updatedAt: DateTime(2026, 6, 8),
    );

    await tester.pumpWidget(wrap(NoteCard(item: item, onTap: () {})));
    await tester.pumpAndSettle();

    expect(find.text('기도'), findsOneWidget); // PRAYER → '기도'
    expect(find.text('6월 8일'), findsOneWidget); // updatedAt
    expect(find.text('나눔'), findsNothing); // 미공개
  });
}
