import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/widgets/note_day_checklist_sheet.dart';

/// 시트를 띄우는 버튼 하나만 있는 호스트 위젯으로 감싸 바텀시트를 연다.
Future<void> _openSheet(WidgetTester tester, DateTime date, CalendarDay? day) async {
  await tester.pumpWidget(
    MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      locale: const Locale('ko'),
      home: Scaffold(
        body: Builder(
          builder: (context) => ElevatedButton(
            onPressed: () => showNoteDayChecklistSheet(context, date, day),
            child: const Text('open'),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text('open'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('작성된 카테고리는 ✓, 나머지는 미체크로 표시된다', (tester) async {
    final day = CalendarDay(
      date: DateTime.utc(2026, 6, 8),
      saved: true,
      savedNoteCount: 2,
      categories: const ['MEDITATION', 'PRAYER'],
    );

    await _openSheet(tester, day.date, day);

    // 5종 라벨이 모두 보인다.
    for (final label in ['묵상', '설교', '기도', '회개', '감사']) {
      expect(find.text(label), findsOneWidget);
    }
    // 작성된 2종(묵상·기도) = 체크, 나머지 3종 = 미체크.
    expect(find.byIcon(Icons.check_circle), findsNWidgets(2));
    expect(find.byIcon(Icons.radio_button_unchecked), findsNWidgets(3));
  });

  testWidgets('기록 없는 날(null)은 5종 전부 미체크', (tester) async {
    await _openSheet(tester, DateTime.utc(2026, 6, 8), null);

    expect(find.byIcon(Icons.check_circle), findsNothing);
    expect(find.byIcon(Icons.radio_button_unchecked), findsNWidgets(5));
  });

  test('노트 진입 시 달력이 기본 화면이다(noteCalendarViewProvider 기본 true)', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    expect(container.read(noteCalendarViewProvider), isTrue);
  });
}
