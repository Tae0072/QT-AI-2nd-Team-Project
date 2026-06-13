import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/note/models/note_drawing.dart';
import 'package:qtai_app/features/note/widgets/note_drawing_layer.dart';
import 'package:qtai_app/features/note/widgets/note_rich_text_editor.dart';

void main() {
  group('모델', () {
    test('DrawingStroke는 JSON으로 저장/복원해도 좌표·색·두께가 보존된다', () {
      const stroke = DrawingStroke(
        points: [Offset(0.1, 0.2), Offset(0.3, 0.45)],
        colorValue: 0xFF112233,
        width: 4,
      );

      // 실제 저장 경로(jsonEncode→decode)와 동일하게 왕복시킨다.
      final back = DrawingStroke.fromJson(
        jsonDecode(jsonEncode(stroke.toJson())) as Map<String, dynamic>,
      );

      expect(back.colorValue, 0xFF112233);
      expect(back.width, 4);
      expect(back.points, hasLength(2));
      expect(back.points.first.dx, closeTo(0.1, 1e-9));
      expect(back.points.last.dy, closeTo(0.45, 1e-9));
    });

    test('NotePageMode.fromStorage는 모르는 값/없음이면 일반(plain)으로 떨어진다', () {
      expect(NotePageMode.fromStorage('manuscript'), NotePageMode.manuscript);
      expect(NotePageMode.fromStorage('plain'), NotePageMode.plain);
      expect(NotePageMode.fromStorage(null), NotePageMode.plain);
      expect(NotePageMode.fromStorage('xxx'), NotePageMode.plain);
    });
  });

  group('에디터 통합', () {
    Future<void> pumpEditor(
      WidgetTester tester, {
      required NotePageMode mode,
      required ValueChanged<NotePageMode> onMode,
      required List<DrawingStroke> strokes,
      required ValueChanged<List<DrawingStroke>> onStrokes,
    }) async {
      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: Scaffold(
              body: NoteRichTextEditor(
                controller: NoteRichBodyController(),
                bodyLabel: '본문',
                pageMode: mode,
                onPageModeChanged: onMode,
                strokes: strokes,
                onStrokesChanged: onStrokes,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
    }

    testWidgets('원고/일반 전환 버튼을 누르면 모드 콜백이 불리고 줄 배경이 그려진다',
        (tester) async {
      var mode = NotePageMode.plain;

      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: StatefulBuilder(
              builder: (context, setState) => Scaffold(
                body: NoteRichTextEditor(
                  controller: NoteRichBodyController(),
                  bodyLabel: '본문',
                  pageMode: mode,
                  onPageModeChanged: (m) => setState(() => mode = m),
                  strokes: const [],
                  onStrokesChanged: (_) {},
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // 일반 모드에선 줄 배경(RuledLinesPainter)이 없다.
      expect(_ruledLinesFinder, findsNothing);

      await tester.ensureVisible(find.byTooltip('원고/일반 전환'));
      await tester.tap(find.byTooltip('원고/일반 전환'));
      await tester.pumpAndSettle();

      expect(mode, NotePageMode.manuscript);
      // 원고 모드로 바뀌면 줄 배경이 그려진다.
      expect(_ruledLinesFinder, findsOneWidget);
    });

    testWidgets('펜으로 그으면 손그림 획이 하나 추가되어 콜백으로 전달된다',
        (tester) async {
      var strokes = <DrawingStroke>[];

      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: StatefulBuilder(
              builder: (context, setState) => Scaffold(
                body: NoteRichTextEditor(
                  controller: NoteRichBodyController(),
                  bodyLabel: '본문',
                  pageMode: NotePageMode.plain,
                  onPageModeChanged: (_) {},
                  strokes: strokes,
                  onStrokesChanged: (s) => setState(() => strokes = s),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // 펜 켜기.
      await tester.ensureVisible(find.byTooltip('펜으로 그리기'));
      await tester.tap(find.byTooltip('펜으로 그리기'));
      await tester.pumpAndSettle();

      // 그리기 레이어 위에서 한 획을 긋는다.
      final layer = find.byType(NoteDrawingLayer);
      expect(layer, findsOneWidget);
      final center = tester.getCenter(layer);
      final gesture = await tester.startGesture(center);
      await gesture.moveBy(const Offset(20, 10));
      await gesture.moveBy(const Offset(20, 30));
      await gesture.up();
      await tester.pumpAndSettle();

      expect(strokes, hasLength(1));
      expect(strokes.first.points.length, greaterThanOrEqualTo(2));
    });

    testWidgets('지우개로 획 위를 지나가면 그 획이 지워진다', (tester) async {
      // 가운데(0.5,0.5)를 지나는 획 하나로 시작 → 레이어 중앙을 지우면 사라진다.
      var strokes = <DrawingStroke>[
        const DrawingStroke(
          // 가운데 점(0.5,0.5)을 포함 → 레이어 중앙을 지우면 닿는다.
          points: [Offset(0.45, 0.5), Offset(0.5, 0.5), Offset(0.55, 0.5)],
          colorValue: 0xFF111827,
          width: 3,
        ),
      ];

      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: StatefulBuilder(
              builder: (context, setState) => Scaffold(
                body: NoteRichTextEditor(
                  controller: NoteRichBodyController(),
                  bodyLabel: '본문',
                  pageMode: NotePageMode.plain,
                  onPageModeChanged: (_) {},
                  strokes: strokes,
                  onStrokesChanged: (s) => setState(() => strokes = s),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byTooltip('지우개'));
      await tester.tap(find.byTooltip('지우개'));
      await tester.pumpAndSettle();

      final layer = find.byType(NoteDrawingLayer);
      final center = tester.getCenter(layer);
      final gesture = await tester.startGesture(center);
      await gesture.moveBy(const Offset(2, 0));
      await gesture.up();
      await tester.pumpAndSettle();

      expect(strokes, isEmpty);
    });

    testWidgets('기존 손그림이 있으면 펜을 켜지 않아도 그림 레이어가 보인다',
        (tester) async {
      await pumpEditor(
        tester,
        mode: NotePageMode.plain,
        onMode: (_) {},
        strokes: const [
          DrawingStroke(
            points: [Offset(0.1, 0.1), Offset(0.5, 0.5)],
            colorValue: 0xFF111827,
            width: 3,
          ),
        ],
        onStrokes: (_) {},
      );

      expect(find.byType(NoteDrawingLayer), findsOneWidget);
    });
  });
}

final Finder _ruledLinesFinder = find.byWidgetPredicate(
  (w) => w is CustomPaint && w.painter is RuledLinesPainter,
);
