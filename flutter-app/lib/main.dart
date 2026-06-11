import 'package:flutter/material.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/config/app_config.dart';
import 'core/dev/web_dev_access.dart'; // [WEB_DEV_ACCESS] 개발 종료 시 삭제
import 'core/theme/app_theme.dart';
import 'core/theme/theme_providers.dart';
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
    // 다크 모드 — 마이페이지 설정 토글만 따른다(시스템 설정 비추종, theme_providers.dart).
    final themeMode = ref.watch(themeModeProvider);
    // [WEB_DEV_ACCESS] 웹 개발용 로그인 우회 (개발 종료 시 이 두 줄과 web_dev_access.dart 삭제)
    final webBypass = webDevNoLogin;
    final forceHome = (AppConfig.instance.isDev && _devForceHome) || webBypass;

    // 인증 상태 확인 중이면 스플래시(로딩) 표시
    if (authStatus == AuthStatus.unknown && !forceHome) {
      return MaterialApp(
        title: 'QT AI',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.theme,
        darkTheme: AppTheme.darkTheme,
        themeMode: themeMode,
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        // 스플래시도 다크 모드 대응 — Builder로 테마 컨텍스트의 AppColors를 받는다.
        home: Builder(
          builder: (context) {
            final colors = context.appColors;
            return Scaffold(
              backgroundColor: colors.bgSunken,
              body: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text.rich(
                      TextSpan(children: [
                        const TextSpan(text: 'QT'),
                        // 로고 가운뎃점 — 유일한 유채색 포인트(탭 도트와 동일 토큰).
                        TextSpan(
                            text: '·',
                            style: TextStyle(color: colors.accentDot)),
                        const TextSpan(text: 'AI'),
                      ]),
                      style: TextStyle(
                        fontFamily: 'GowunDodum',
                        fontSize: 60,
                        fontWeight: FontWeight.w400,
                        color: colors.text,
                        letterSpacing: -1.4,
                      ),
                    ),
                    const SizedBox(height: 14),
                    Text(
                      AppLocalizations.of(context).splashSubtitle,
                      style:
                          TextStyle(fontSize: 17, color: colors.textMuted),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      );
    }

    // 라우트 분기: 온보딩 미완료 → 온보딩, 토큰 있음 → 홈, 토큰 없음 → 로그인
    final initialRoute = resolveInitialRoute(
      onboardingComplete: onboardingComplete,
      authStatus: authStatus,
      isDev: AppConfig.instance.isDev,
      devForceHome: _devForceHome || webBypass, // [WEB_DEV_ACCESS]
    );

    return MaterialApp(
      // key를 initialRoute에 연동 — authStatus 변경 시 Navigator를 새로 생성
      key: ValueKey(initialRoute),
      title: 'QT AI',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.theme,
      // 다크 모드 — Calm Paper 다크 토큰("잉크 위의 종이"), 설정 화면 토글만 따른다.
      darkTheme: AppTheme.darkTheme,
      themeMode: themeMode,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      locale: const Locale('ko'),
      initialRoute: initialRoute,
      // 초기 스택을 단일 라우트로 생성한다.
      // 기본 동작은 '/home' → ['/', '/home'] 2단 스택을 만들어 루트 탭 화면에도
      // 뒤로가기(←)가 떠버린다. 루트 라우트 하나만 두어 canPop()=false로 만들고,
      // 마이페이지→설정 등 push된 하위 화면에서만 ←가 동작하게 한다.
      onGenerateInitialRoutes: (name) =>
          [AppRouter.onGenerateRoute(RouteSettings(name: name))],
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
