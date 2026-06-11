import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/home/widgets/double_back_exit_scope.dart';
import 'package:qtai_app/l10n/app_localizations.dart';

/// [DoubleBackExitScope] 위젯 테스트 — 실제 시스템 뒤로가기(handlePopRoute)로 검증한다.
///
/// 보장하는 동작:
/// 1. 스코프 위에 push된 화면(알림·설정·노트 작성 등)의 뒤로가기는 일반 pop —
///    부모 화면으로 복귀하며 종료 가드(스낵바/종료)가 개입하지 않는다.
/// 2. 루트에서 첫 뒤로가기는 안내 스낵바만, 2초 내 두 번째에만 SystemNavigator.pop.
void main() {
  final platformCalls = <MethodCall>[];

  Future<void> pumpHarness(WidgetTester tester) async {
    platformCalls.clear();
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      SystemChannels.platform,
      (call) async {
        platformCalls.add(call);
        return null;
      },
    );
    await tester.pumpWidget(MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: DoubleBackExitScope(
        child: Scaffold(
          body: Builder(
            builder: (context) => Center(
              child: TextButton(
                onPressed: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => const Scaffold(body: Text('detail')),
                  ),
                ),
                child: const Text('go-detail'),
              ),
            ),
          ),
        ),
      ),
    ));
  }

  bool exitRequested() =>
      platformCalls.any((c) => c.method == 'SystemNavigator.pop');

  testWidgets('push된 화면의 뒤로가기는 부모로 복귀하고 종료 가드가 개입하지 않는다', (tester) async {
    await pumpHarness(tester);

    await tester.tap(find.text('go-detail'));
    await tester.pumpAndSettle();
    expect(find.text('detail'), findsOneWidget);

    // 시스템 뒤로가기 — 일반 pop으로 부모(루트) 복귀
    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();

    expect(find.text('detail'), findsNothing);
    expect(find.text('go-detail'), findsOneWidget); // 부모 화면 복귀
    expect(find.byType(SnackBar), findsNothing); // 안내 스낵바 미개입
    expect(exitRequested(), isFalse); // 종료 미발생
  });

  testWidgets('루트 첫 뒤로가기는 안내 스낵바만 띄우고 종료하지 않는다', (tester) async {
    await pumpHarness(tester);

    await tester.binding.handlePopRoute();
    await tester.pump();

    expect(find.byType(SnackBar), findsOneWidget);
    expect(exitRequested(), isFalse);

    await tester.pump(const Duration(seconds: 3)); // 스낵바 타이머 정리
  });

  testWidgets('루트에서 2초 내 두 번째 뒤로가기는 SystemNavigator.pop으로 종료한다', (tester) async {
    await pumpHarness(tester);

    await tester.binding.handlePopRoute();
    await tester.pump(const Duration(milliseconds: 300));
    await tester.binding.handlePopRoute();
    await tester.pump();

    expect(exitRequested(), isTrue);

    await tester.pump(const Duration(seconds: 3)); // 스낵바 타이머 정리
  });
}
