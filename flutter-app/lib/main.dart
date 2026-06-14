import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/config/app_config.dart';
import 'core/dev/web_dev_access.dart'; // [WEB_DEV_ACCESS] 개발 종료 시 삭제
import 'core/notifications/notification_poller.dart';
import 'core/theme/app_theme.dart';
import 'core/theme/font_scale_provider.dart';
import 'core/theme/theme_providers.dart';
import 'features/auth/providers/auth_providers.dart';
import 'features/bible/providers/bible_providers.dart' show todayQtPassageProvider;
import 'features/onboarding/providers/onboarding_providers.dart';
import 'features/onboarding/screens/intro_splash_screen.dart';
import 'routes/app_router.dart';

const bool _devForceHome =
    bool.fromEnvironment('DEV_FORCE_HOME', defaultValue: false);

// 실제 앱 구동(main 진입)에서만 true. 위젯 테스트는 main()을 거치지 않고 QTAIApp을
// 직접 pump하므로 false로 남아, 알림 폴러(주기 Timer)가 테스트에서 시작되지 않는다.
bool _launchedFromMain = false;

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  _launchedFromMain = true;
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

class QTAIApp extends ConsumerStatefulWidget {
  const QTAIApp({super.key});

  @override
  ConsumerState<QTAIApp> createState() => _QTAIAppState();
}

class _QTAIAppState extends ConsumerState<QTAIApp> {
  bool _mainAppStarted = false;
  // 콜드스타트 인트로(로딩) 재생 중 플래그 — 한번 시작하면 애니메이션이 끝날 때까지 유지한다.
  bool _introPlaying = false;

  @override
  Widget build(BuildContext context) {
    final onboardingComplete = ref.watch(onboardingCompleteProvider);
    final authStatus = ref.watch(authStatusProvider);
    // 다크 모드 — 마이페이지 설정 토글만 따른다(시스템 설정 비추종, theme_providers.dart).
    final themeMode = ref.watch(themeModeProvider);
    // 폰트 크기 — 설정 화면의 SMALL/MEDIUM/LARGE를 앱 전역 글자 배율로 적용한다.
    // OS 글자 크기는 추종하지 않고(다크 모드와 같은 철학) 앱 설정이 단일 진실이다.
    final fontScale = fontScaleOf(ref.watch(fontSizeProvider));
    // [WEB_DEV_ACCESS] 웹 개발용 로그인 우회 (개발 종료 시 이 두 줄과 web_dev_access.dart 삭제)
    final webBypass = webDevNoLogin;
    final forceHome = (AppConfig.instance.isDev && _devForceHome) || webBypass;

    // 콜드스타트 인트로(로딩) 애니메이션. 인증 미확정(unknown)으로 시작하면 한 번 재생하고,
    // 애니메이션이 끝날 때까지(_introPlaying) 유지한다. 그동안 백그라운드로 인증·오늘 QT를 준비한다.
    // (테스트는 authStatus를 authenticated로 주입하므로 인트로가 뜨지 않는다 → 기존 흐름 유지.)
    if (!_mainAppStarted &&
        !forceHome &&
        (authStatus == AuthStatus.unknown || _introPlaying)) {
      _introPlaying = true;
      return MaterialApp(
        key: const ValueKey('qtai-splash-app'),
        title: 'QT AI',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.theme,
        darkTheme: AppTheme.darkTheme,
        themeMode: themeMode,
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        home: IntroSplashScreen(
          onComplete: () {
            if (mounted) {
              setState(() {
                _introPlaying = false;
                _mainAppStarted = true;
              });
            }
          },
          preload: () async {
            // 로딩 동안 미리 받아둘 것들 — 로그인돼 있으면 오늘 QT를 미리 받아 둔다(실패 무시).
            try {
              if (await ref.read(authRepositoryProvider).hasToken()) {
                await ref
                    .read(todayQtPassageProvider.future)
                    .timeout(const Duration(seconds: 6));
              }
            } catch (_) {}
          },
        ),
      );
    }

    _mainAppStarted = true;

    // 서버 알림 → 기기 배너 브리지(폴러)는 실제 앱 구동 + 로그인 상태에서만 살린다.
    // (위젯 테스트는 main()을 안 거치므로 _launchedFromMain=false → 폴러 미시작.)
    if (_launchedFromMain && authStatus == AuthStatus.authenticated) {
      ref.watch(notificationPollerProvider);
    }

    // 라우트 분기: 온보딩 미완료 → 온보딩, 토큰 있음 → 홈, 토큰 없음 → 로그인
    final initialRoute = resolveInitialRoute(
      onboardingComplete: onboardingComplete,
      authStatus: authStatus,
      isDev: AppConfig.instance.isDev,
      devForceHome: _devForceHome || webBypass, // [WEB_DEV_ACCESS]
    );

    return MaterialApp(
      // splash → main 전환 때만 Navigator를 새로 만들고,
      // main app 내부에서는 auth/initialRoute 변화로 push된 화면을 잃지 않는다.
      key: const ValueKey('qtai-main-app'),
      title: 'QT AI',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.theme,
      // 다크 모드 — Calm Paper 다크 토큰("잉크 위의 종이"), 설정 화면 토글만 따른다.
      darkTheme: AppTheme.darkTheme,
      themeMode: themeMode,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      locale: const Locale('ko'),
      // 폰트 크기 설정을 앱 전역에 적용 + 상단 기기 상태바 아이콘 색을 테마와 통일한다.
      // AppBar가 없는 화면(홈 대시보드 등)도 AnnotatedRegion으로 동일 스타일을 받는다.
      // 시스템 모드면 기기 밝기(platformBrightness)를 그대로 따른다.
      builder: (context, child) {
        final isDark = themeMode == ThemeMode.dark ||
            (themeMode == ThemeMode.system &&
                MediaQuery.platformBrightnessOf(context) == Brightness.dark);
        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: SystemUiOverlayStyle(
            statusBarColor: Colors.transparent,
            statusBarIconBrightness:
                isDark ? Brightness.light : Brightness.dark,
            statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
          ),
          child: MediaQuery(
            data: MediaQuery.of(context).copyWith(
              textScaler: TextScaler.linear(fontScale),
            ),
            child: child ?? const SizedBox.shrink(),
          ),
        );
      },
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
