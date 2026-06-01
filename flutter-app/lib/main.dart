import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/config/app_config.dart';
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

    return MaterialApp(
      title: 'QT AI',
      debugShowCheckedModeBanner: AppConfig.instance.isDev,
      theme: ThemeData(
        colorSchemeSeed: const Color(0xFF6750A4),
        useMaterial3: true,
        fontFamily: 'Pretendard',
      ),
      initialRoute: onboardingComplete ? AppRouter.login : AppRouter.onboarding,
      onGenerateRoute: AppRouter.onGenerateRoute,
    );
  }
}
