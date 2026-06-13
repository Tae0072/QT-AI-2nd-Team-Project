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
      bodyPreview: '오늘 묵상하며 마음에 남은 것을 적었다.',
      status: 'SAVED',
      visibility: 'PUBLIC',
      qtDate: '2026-06-10',
      shared: true,
      updatedAt: DateTime(2026, 6, 10), // 00:00 → 오전
    );

    await tester.pumpWidget(
        wrap(NoteCard(item: item, onTap: () => tapped = true)));
    await tester.pumpAndSettle();

    expect(find.text('부끄러운 송사'), findsOneWidget);
    expect(find.text('QT'), findsOneWidget); // MEDITATION → 'QT'
    // 날짜·시각이 위아래 두 줄로 분리
    expect(find.text('2026.06.10'), findsOneWidget);
    expect(find.text('12:00 AM'), findsOneWidget);
    expect(find.text('오전'), findsOneWidget); // 시간대 배지
    expect(find.text('나눔공개'), findsOneWidget); // shared 배지
    expect(find.text('오늘 묵상하며 마음에 남은 것을 적었다.'), findsOneWidget); // 미리보기
    // 더 이상 카테고리 아이콘은 없다.
    expect(find.byIcon(Icons.wb_sunny_outlined), findsNothing);

    await tester.tap(find.byType(NoteCard));
    expect(tapped, isTrue);
  });

  testWidgets('제목이 비면 기본 라벨, 임시저장 표시, 미공개는 나눔 배지 없음, 미리보기 없으면 생략',
      (tester) async {
    final item = NoteListItem(
      id: 2,
      category: 'PRAYER',
      title: '',
      status: 'DRAFT',
      visibility: 'PRIVATE',
      shared: false,
      updatedAt: DateTime(2026, 6, 8, 14, 5), // 14시 → 오후
    );

    await tester.pumpWidget(wrap(NoteCard(item: item, onTap: () {})));
    await tester.pumpAndSettle();

    expect(find.text('(제목 없음)'), findsOneWidget); // 빈 제목 기본 라벨
    expect(find.text('기도'), findsOneWidget); // PRAYER → '기도'
    expect(find.text('2026.06.08'), findsOneWidget);
    expect(find.text('02:05 PM'), findsOneWidget);
    expect(find.text('오후'), findsOneWidget); // 시간대 배지
    expect(find.text('임시저장'), findsOneWidget); // DRAFT 라벨
    expect(find.text('나눔공개'), findsNothing); // 미공개
  });
}
