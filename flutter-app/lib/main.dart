import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/config/app_config.dart';
import 'features/auth/providers/auth_providers.dart';
import 'features/onboarding/providers/onboarding_providers.dart';
import 'routes/app_router.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  AppConfig.initialize();

  // 카카오 SDK 초기화
  KakaoSdk.init(nativeAppKey: AppConfig.instance.kakaoNativeAppKey);

  final prefs = await SharedPreferences.getInstance();

  runApp(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
      ],
      child: const QTAIApp(),
    ),
  );
}

class QTAIApp extends ConsumerWidget {
  const QTAIApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final onboardingComplete = ref.watch(onboardingCompleteProvider);
    final authStatus = ref.watch(authStatusProvider);

    // 인증 상태 확인 중이면 스플래시(로딩) 표시
    if (authStatus == AuthStatus.unknown) {
      return MaterialApp(
        title: 'QT AI',
        debugShowCheckedModeBanner: AppConfig.instance.isDev,
        theme: ThemeData(
          colorSchemeSeed: const Color(0xFF6750A4),
          useMaterial3: true,
          fontFamily: 'Pretendard',
        ),
        home: const Scaffold(
          body: Center(child: CircularProgressIndicator()),
        ),
      );
    }

    // 라우트 분기: 온보딩 미완료 → 온보딩, 토큰 있음 → 홈, 토큰 없음 → 로그인
    final String initialRoute;
    if (!onboardingComplete) {
      initialRoute = AppRouter.onboarding;
    } else if (authStatus == AuthStatus.authenticated) {
      initialRoute = AppRouter.home;
    } else {
      initialRoute = AppRouter.login;
    }

    return MaterialApp(
      // key를 initialRoute에 연동 — authStatus 변경 시 Navigator를 새로 생성
      key: ValueKey(initialRoute),
      title: 'QT AI',
      debugShowCheckedModeBanner: AppConfig.instance.isDev,
      theme: ThemeData(
        colorSchemeSeed: const Color(0xFF6750A4),
        useMaterial3: true,
        fontFamily: 'Pretendard',
      ),
      initialRoute: initialRoute,
      onGenerateRoute: AppRouter.onGenerateRoute,
    );
  }
}
