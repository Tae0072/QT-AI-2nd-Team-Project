import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/dev/dev_mode_screen.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  testWidgets('개발자 모드에 화면 허브·상태 토글·알림 테스트 버튼이 표시된다', (tester) async {
    // 화면이 길어 기본 뷰포트(600)에선 하단 항목이 지연 빌드로 안 잡힌다 → 충분히 크게.
    tester.view.physicalSize = const Size(1080, 5000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
        child: const MaterialApp(home: DevModeScreen()),
      ),
    );
    await tester.pumpAndSettle();

    // 화면 허브
    expect(find.text('화면 바로가기'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, '나눔 피드'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, '나를 태그한 글'), findsOneWidget);
    // 상태 토글
    expect(find.text('상태 토글'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, '로그아웃'), findsOneWidget);
    // 알림 테스트(기존)
    expect(find.text('알림 보내기 (테스트)'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, '좋아요 알림'), findsOneWidget);
    expect(
      find.widgetWithText(FilledButton, '테스트 알림 보내기'),
      findsOneWidget,
    );
  });
}
