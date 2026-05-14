import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/router/app_router.dart';

void main() {
  runApp(const ProviderScope(child: QtAiApp()));
}

/// QT-AI 모바일 앱 진입점.
///
/// MVP 결정 (DECISIONS.md §3.1):
/// - 별도 홈 없이 오늘 QT(`/today`)로 바로 진입.
/// - 오늘 QT 본문 먼저 로딩, 나머지는 백그라운드.
/// - AI 질문/묵상 작성은 오늘 QT 본문에서만 가능.
class QtAiApp extends ConsumerWidget {
  const QtAiApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);
    return MaterialApp.router(
      title: 'QT-AI',
      debugShowCheckedModeBanner: false,
      routerConfig: router,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF4F46E5),
          brightness: Brightness.light,
        ),
        fontFamily: 'Pretendard',
      ),
    );
  }
}
