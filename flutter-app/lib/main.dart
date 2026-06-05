import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/config/app_config.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/providers/auth_providers.dart';
import 'features/onboarding/providers/onboarding_providers.dart';
import 'routes/app_router.dart';

const bool _devForceHome =
    bool.fromEnvironment('DEV_FORCE_HOME', defaultValue: false);

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
    final forceHome = AppConfig.instance.isDev && _devForceHome;

    // 인증 상태 확인 중이면 스플래시(로딩) 표시
    if (authStatus == AuthStatus.unknown && !forceHome) {
      return MaterialApp(
        title: 'QT AI',
        debugShowCheckedModeBanner: AppConfig.instance.isDev,
        theme: AppTheme.theme,
        home: Scaffold(
          backgroundColor: AppTheme.bgSunken,
          body: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text.rich(
                  TextSpan(children: [
                    const TextSpan(text: 'QT'),
                    TextSpan(text: '·', style: TextStyle(color: AppTheme.accent)),
                    const TextSpan(text: 'AI'),
                  ]),
                  style: const TextStyle(
                    fontFamily: 'GowunDodum',
                    fontSize: 60,
                    fontWeight: FontWeight.w400,
                    color: AppTheme.text,
                    letterSpacing: -1.4,
                  ),
                ),
                const SizedBox(height: 14),
                const Text('매일, 말씀 앞에 머무는 시간',
                    style: TextStyle(fontSize: 17, color: AppTheme.textMuted)),
              ],
            ),
          ),
        ),
      );
    }

    // 라우트 분기: 온보딩 미완료 → 온보딩, 토큰 있음 → 홈, 토큰 없음 → 로그인
    final initialRoute = resolveInitialRoute(
      onboardingComplete: onboardingComplete,
      authStatus: authStatus,
      isDev: AppConfig.instance.isDev,
      devForceHome: _devForceHome,
    );

    return MaterialApp(
      // key를 initialRoute에 연동 — authStatus 변경 시 Navigator를 새로 생성
      key: ValueKey(initialRoute),
      title: 'QT AI',
      debugShowCheckedModeBanner: AppConfig.instance.isDev,
      theme: AppTheme.theme,
      initialRoute: initialRoute,
      onGenerateRoute: AppRouter.onGenerateRoute,
    );
  }
}

String resolveInitialRoute({
  required bool onboardingComplete,
  required AuthStatus authStatus,
  required bool isDev,
  bool devForceHome = false,
}) {
  if (isDev && devForceHome) {
    return AppRouter.home;
  }
  if (!onboardingComplete) {
    return AppRouter.onboarding;
  }
  if (authStatus == AuthStatus.authenticated) {
    return AppRouter.home;
  }
  return AppRouter.login;
}
