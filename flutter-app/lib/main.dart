import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'core/config/app_config.dart';
import 'features/onboarding/providers/onboarding_providers.dart';
import 'routes/app_router.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  AppConfig.initialize();

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
      initialRoute: onboardingComplete ? AppRouter.home : AppRouter.onboarding,
      onGenerateRoute: AppRouter.onGenerateRoute,
    );
  }
}
