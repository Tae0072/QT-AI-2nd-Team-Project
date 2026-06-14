import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/config/app_config.dart';
import 'core/dev/web_dev_access.dart'; // [WEB_DEV_ACCESS] 개발 종료 시 삭제
import 'core/theme/app_theme.dart';
import 'core/theme/font_scale_provider.dart';
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

class QTAIApp extends ConsumerStatefulWidget {
  const QTAIApp({super.key});

  @override
  ConsumerState<QTAIApp> createState() => _QTAIAppState();
}

class _QTAIAppState extends ConsumerState<QTAIApp> {
  bool _mainAppStarted = false;

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

    // 인증 상태 확인 중이면 최초 진입에서만 스플래시(로딩)를 표시한다.
    // main app이 한 번 시작된 뒤에는 refresh/auth 상태가 잠깐 unknown으로
    // 흔들려도 Navigator를 새로 만들지 않아 push된 화면을 유지한다.
    if (!_mainAppStarted && authStatus == AuthStatus.unknown && !forceHome) {
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
                      style: TextStyle(fontSize: 17, color: colors.textMuted),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      );
    }

    _mainAppStarted = true;

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
