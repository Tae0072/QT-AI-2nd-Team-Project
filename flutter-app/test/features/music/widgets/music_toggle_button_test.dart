import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/music/widgets/music_toggle_button.dart';
import 'package:qtai_app/features/music/widgets/music_track_sheet.dart';

/// 회귀 테스트: 오늘의 QT 음표 버튼을 "길게 누르면" 음원 목록 시트가 떠야 한다.
///
/// 과거 버그: IconButton의 tooltip이 모바일에서 길게 누르기 제스처를
/// 가로채(Tooltip 기본 트리거 = longPress) 시트가 열리지 않고 툴팁만 떴다.
/// 아래 테스트는 길게 누르기로 MusicTrackSheet가 실제로 열리는지 확인한다.
///
/// 주의: 길게 누르기 경로는 시트를 열어 상태를 "읽기"만 하므로
/// AudioPlayer(just_audio) 메서드를 호출하지 않는다. 따라서 별도 mock 없이 동작한다.
void main() {
  Widget harness() {
    return const ProviderScope(
      child: MaterialApp(
        home: Scaffold(
          body: Center(child: MusicToggleButton()),
        ),
      ),
    );
  }

  testWidgets('음표 버튼을 길게 누르면 음원 목록 시트가 열린다', (tester) async {
    await tester.pumpWidget(harness());
    await tester.pump();

    // 처음에는 시트가 없다.
    expect(find.byType(MusicTrackSheet), findsNothing);

    // 음표 아이콘을 길게 누른다.
    await tester.longPress(find.byType(MusicToggleButton));
    await tester.pumpAndSettle();

    // 음원 목록 시트가 떠야 한다(헤더 '배경음악' 포함).
    expect(find.byType(MusicTrackSheet), findsOneWidget);
    expect(find.text('배경음악'), findsOneWidget);
  });

  testWidgets('Tooltip은 길게 누르기를 가로채지 않도록 manual 트리거여야 한다', (tester) async {
    await tester.pumpWidget(harness());
    await tester.pump();

    // 버튼을 감싼 Tooltip이 manual 트리거여야 길게 누르기가 시트로 전달된다.
    final tooltip = tester.widget<Tooltip>(find.byType(Tooltip));
    expect(tooltip.triggerMode, TooltipTriggerMode.manual);
  });
}
