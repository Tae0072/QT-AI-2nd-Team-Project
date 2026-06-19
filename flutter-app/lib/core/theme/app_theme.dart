import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// QT·AI "Calm Paper" 테마 (DESIGN_PROMPT.md, 2026-06-11 적용 / 다크 모드 동시 지원).
///
/// 컨셉: 따뜻한 종이 위의 미니멀 묵상 — 위계는 색이 아니라 명도·크기·여백으로 만든다.
/// 라이트 = 종이 위의 잉크, 다크 = 잉크 위의 종이(동일 8토큰의 무채색 반전).
/// 유일한 유채색 포인트(#E0492F)는 [AppColors.accentDot] — 활성 탭 도트 전용.
///
/// 화면에서 색이 필요하면 정적 상수 대신 **`Theme.of(context).extension<AppColors>()!`**를
/// 쓴다 — 다크 모드에서 자동으로 올바른 토큰을 받는다. (기존 `AppTheme.x` 정적 상수는
/// 라이트 값으로 유지하되 신규 코드에서는 사용하지 않는다.)
class AppTheme {
  AppTheme._();

  // ── 라이트 토큰 (기존 코드 호환용 정적 상수 — 신규 코드는 AppColors 사용) ──
  static const Color pageBg = Color(0xFFF0EEEA);
  static const Color bg = Color(0xFFF7F5F2);
  static const Color bgSunken = Color(0xFFF0EEEA);
  static const Color bgElevated = Color(0xFFFFFFFF);
  static const Color accent = Color(0xFF1F1F1F);
  static const Color accentSoft = Color(0xFFF0EEEA);
  static const Color accentDot = Color(0xFFE0492F);
  static const Color text = Color(0xFF1F1F1F);
  static const Color text2 = Color(0xFF8A8A8E);
  static const Color textMuted = Color(0xFFB8B5B0);
  static const Color hairline = Color(0xFFE8E6E2);
  static const Color divider = Color(0xFFE8E6E2);
  static const Color heroBg = Color(0xFFF0EEEA);
  static const Color heroText = Color(0xFF1F1F1F);
  static const Color heroAccent = Color(0xFF1F1F1F);

  // ── 토큰 세트 ──
  /// 라이트 토큰 세트 — 테마 미지정 컨텍스트(테스트 등)의 fallback으로도 쓴다.
  static const AppColors lightColors = AppColors(
    pageBg: Color(0xFFF0EEEA),
    bg: Color(0xFFF7F5F2),
    bgSunken: Color(0xFFF0EEEA),
    bgElevated: Color(0xFFFFFFFF),
    accent: Color(0xFF1F1F1F),
    accentSoft: Color(0xFFF0EEEA),
    accentDot: Color(0xFFE0492F),
    text: Color(0xFF1F1F1F),
    text2: Color(0xFF8A8A8E),
    textMuted: Color(0xFFB8B5B0),
    hairline: Color(0xFFE8E6E2),
    onAccent: Color(0xFFFFFFFF),
    // 라이트: 종이 위에서 또렷한 파랑(해설 본문 강조 전용, F-08).
    explanationBlue: Color(0xFF2563EB),
  );

  /// 다크 = "잉크 위의 종이" — 웜 다크 차콜 배경 + 종이색 텍스트, 도트는 동일.
  static const AppColors darkColors = AppColors(
    pageBg: Color(0xFF181715),
    bg: Color(0xFF1B1A18),
    bgSunken: Color(0xFF21201D),
    bgElevated: Color(0xFF242220),
    accent: Color(0xFFF0EEEA),
    accentSoft: Color(0xFF2C2A27),
    accentDot: Color(0xFFE0492F),
    text: Color(0xFFF0EEEA),
    text2: Color(0xFFA5A29C),
    textMuted: Color(0xFF6E6B66),
    hairline: Color(0xFF383531),
    onAccent: Color(0xFF1F1F1F),
    // 다크: 차콜 배경에서 가독성 높은 밝은 파랑(해설 본문 강조 전용, F-08).
    explanationBlue: Color(0xFF8AB4F8),
  );

  // ── 테마 ──
  static ThemeData get theme => _build(lightColors, Brightness.light);
  static ThemeData get darkTheme => _build(darkColors, Brightness.dark);

  /// 라이트/다크 공용 빌더 — 컴포넌트 규격(§2·§3)은 동일, 색 토큰만 교체된다.
  static ThemeData _build(AppColors c, Brightness brightness) => ThemeData(
    useMaterial3: true,
    fontFamily: 'GowunDodum',
    scaffoldBackgroundColor: c.bg,
    // Calm Paper: M3가 near-neutral 시드(#1F1F1F)에서 primary를 청록빛으로 파생시키는
    // 문제를 막기 위해 primary·secondary를 잉크 토큰으로 고정한다. 이렇게 하면 화면들이
    // 쓰는 colorScheme.primary(절번호·통계 아이콘·달력 헤더 등)가 모두 무채색 잉크로 나온다.
    colorScheme: ColorScheme.fromSeed(seedColor: c.accent, brightness: brightness)
        .copyWith(
      primary: c.accent,
      onPrimary: c.onAccent,
      primaryContainer: c.accentSoft,
      onPrimaryContainer: c.text,
      secondary: c.accent,
      onSecondary: c.onAccent,
      secondaryContainer: c.accentSoft,
      onSecondaryContainer: c.text,
      tertiary: c.accent,
      onTertiary: c.onAccent,
      tertiaryContainer: c.accentSoft,
      onTertiaryContainer: c.text,
      surface: c.bgElevated,
      onSurface: c.text,
    ),
    brightness: brightness,
    extensions: [c],
    // 앱바: 투명 배경, elevation 0, 17 w600 중앙(§3 titleBar).
    appBarTheme: AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      // 상단 기기 상태바(시계·배터리·와이파이) 아이콘 색을 테마에 고정한다.
      // 라이트=검은 아이콘(밝은 배경 가독), 다크=흰 아이콘. 페이지마다 배경색이 달라도 통일된다.
      systemOverlayStyle: SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness:
            brightness == Brightness.dark ? Brightness.light : Brightness.dark,
        statusBarBrightness:
            brightness == Brightness.dark ? Brightness.dark : Brightness.light,
      ),
      titleTextStyle: TextStyle(
        fontFamily: 'GowunDodum',
        fontSize: 17,
        fontWeight: FontWeight.w600,
        color: c.text,
        letterSpacing: -0.2,
      ),
      iconTheme: IconThemeData(color: c.text),
    ),
    // 카드 = 면 + 헤어라인 + 반경 14(보조박스 규격). 그림자 금지(§2).
    cardTheme: CardThemeData(
      color: c.bgElevated,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: c.hairline),
      ),
      shadowColor: Colors.transparent,
    ),
    // 칩: stadium 필. 선택 = 면 + 헤어라인, 비선택 = 배경 + 보조색(§3).
    chipTheme: ChipThemeData(
      backgroundColor: c.bg,
      selectedColor: c.bgElevated,
      labelStyle: TextStyle(fontFamily: 'GowunDodum', fontSize: 14, color: c.text2),
      secondaryLabelStyle:
          TextStyle(fontFamily: 'GowunDodum', fontSize: 14, color: c.text),
      checkmarkColor: c.text,
      shape: StadiumBorder(side: BorderSide(color: c.hairline)),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: c.bgElevated,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(color: c.hairline),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(color: c.hairline),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(color: c.accent, width: 1.5),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
    ),
    // 주 버튼: 잉크 필(라이트) / 종이 필(다크).
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: c.accent,
        foregroundColor: c.onAccent,
        elevation: 0,
        shape: const StadiumBorder(),
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 16),
        textStyle: const TextStyle(
          fontFamily: 'GowunDodum',
          fontSize: 16,
          fontWeight: FontWeight.w600,
        ),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: c.text,
        shape: const StadiumBorder(),
        side: BorderSide(color: c.hairline),
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 13),
      ),
    ),
    floatingActionButtonTheme: FloatingActionButtonThemeData(
      backgroundColor: c.accent,
      foregroundColor: c.onAccent,
      shape: const CircleBorder(),
      elevation: 0,
    ),
    // 탭바: 면 배경(상단 헤어라인은 화면측), 활성 = 본문색, 비활성 = 3차색.
    // 활성 표시는 filled 아이콘 전환 금지 — 빨간 도트([AppColors.accentDot])로(§3).
    bottomNavigationBarTheme: BottomNavigationBarThemeData(
      backgroundColor: c.bgElevated,
      selectedItemColor: c.text,
      unselectedItemColor: c.textMuted,
      type: BottomNavigationBarType.fixed,
      elevation: 0,
      selectedLabelStyle: const TextStyle(
          fontFamily: 'GowunDodum', fontSize: 11, fontWeight: FontWeight.w600),
      unselectedLabelStyle:
          const TextStyle(fontFamily: 'GowunDodum', fontSize: 11),
    ),
    dividerTheme: DividerThemeData(color: c.hairline, thickness: 1),
    // 타이포(§2): titleXL 26 w700 / body 16·1.65 / bodySub 14·1.55 / caption 13 / label 12.
    textTheme: TextTheme(
      headlineLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 26, fontWeight: FontWeight.w700, color: c.text, letterSpacing: -0.5),
      headlineMedium: TextStyle(fontFamily: 'GowunDodum', fontSize: 22, fontWeight: FontWeight.w700, color: c.text, letterSpacing: -0.4),
      titleLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 17, fontWeight: FontWeight.w600, color: c.text),
      titleMedium: TextStyle(fontFamily: 'GowunDodum', fontSize: 16, fontWeight: FontWeight.w600, color: c.text),
      titleSmall: TextStyle(fontFamily: 'GowunDodum', fontSize: 13, fontWeight: FontWeight.w600, color: c.text2),
      bodyLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 16, color: c.text, height: 1.65, letterSpacing: -0.2),
      bodyMedium: TextStyle(fontFamily: 'GowunDodum', fontSize: 14, color: c.text2, height: 1.55),
      bodySmall: TextStyle(fontFamily: 'GowunDodum', fontSize: 13, color: c.text2),
      labelLarge: const TextStyle(fontFamily: 'GowunDodum', fontSize: 16, fontWeight: FontWeight.w600),
      labelSmall: TextStyle(fontFamily: 'GowunDodum', fontSize: 12, fontWeight: FontWeight.w500, color: c.textMuted),
    ),
  );
}

/// [AppColors] 접근 헬퍼 — 화면에서는 `context.appColors`로 쓴다.
///
/// 테마에 확장이 등록되지 않은 컨텍스트(테스트의 기본 MaterialApp 등)에서는
/// 라이트 토큰으로 fallback해 NPE 없이 동작한다.
extension AppColorsX on BuildContext {
  AppColors get appColors =>
      Theme.of(this).extension<AppColors>() ?? AppTheme.lightColors;
}

/// Calm Paper 색 토큰 — 라이트/다크에서 자동 교체되는 ThemeExtension.
///
/// 사용: `final colors = context.appColors;`
@immutable
class AppColors extends ThemeExtension<AppColors> {
  const AppColors({
    required this.pageBg,
    required this.bg,
    required this.bgSunken,
    required this.bgElevated,
    required this.accent,
    required this.accentSoft,
    required this.accentDot,
    required this.text,
    required this.text2,
    required this.textMuted,
    required this.hairline,
    required this.onAccent,
    required this.explanationBlue,
  });

  final Color pageBg;
  final Color bg;
  final Color bgSunken;
  final Color bgElevated;

  /// 일반 강조(버튼·포커스) — 라이트=잉크, 다크=종이. 유채색 아님.
  final Color accent;
  final Color accentSoft;

  /// 활성 탭 도트 **전용** 유채색 포인트(#E0492F) — 화면당 1곳 이하(§2).
  final Color accentDot;
  final Color text;
  final Color text2;
  final Color textMuted;
  final Color hairline;

  /// [accent] 면 위에 올라가는 글자색.
  final Color onAccent;

  /// 해설(절별 해설 본문) 강조 파랑 — 라이트/다크 각각 가독성에 맞춘 유채색.
  /// Lead 요청(2026-06-19, F-08)으로 해설 가독성을 위해 도입한 예외적 유채색이다.
  final Color explanationBlue;

  @override
  AppColors copyWith({
    Color? pageBg,
    Color? bg,
    Color? bgSunken,
    Color? bgElevated,
    Color? accent,
    Color? accentSoft,
    Color? accentDot,
    Color? text,
    Color? text2,
    Color? textMuted,
    Color? hairline,
    Color? onAccent,
    Color? explanationBlue,
  }) {
    return AppColors(
      pageBg: pageBg ?? this.pageBg,
      bg: bg ?? this.bg,
      bgSunken: bgSunken ?? this.bgSunken,
      bgElevated: bgElevated ?? this.bgElevated,
      accent: accent ?? this.accent,
      accentSoft: accentSoft ?? this.accentSoft,
      accentDot: accentDot ?? this.accentDot,
      text: text ?? this.text,
      text2: text2 ?? this.text2,
      textMuted: textMuted ?? this.textMuted,
      hairline: hairline ?? this.hairline,
      onAccent: onAccent ?? this.onAccent,
      explanationBlue: explanationBlue ?? this.explanationBlue,
    );
  }

  @override
  AppColors lerp(ThemeExtension<AppColors>? other, double t) {
    if (other is! AppColors) return this;
    return AppColors(
      pageBg: Color.lerp(pageBg, other.pageBg, t)!,
      bg: Color.lerp(bg, other.bg, t)!,
      bgSunken: Color.lerp(bgSunken, other.bgSunken, t)!,
      bgElevated: Color.lerp(bgElevated, other.bgElevated, t)!,
      accent: Color.lerp(accent, other.accent, t)!,
      accentSoft: Color.lerp(accentSoft, other.accentSoft, t)!,
      accentDot: Color.lerp(accentDot, other.accentDot, t)!,
      text: Color.lerp(text, other.text, t)!,
      text2: Color.lerp(text2, other.text2, t)!,
      textMuted: Color.lerp(textMuted, other.textMuted, t)!,
      hairline: Color.lerp(hairline, other.hairline, t)!,
      onAccent: Color.lerp(onAccent, other.onAccent, t)!,
      explanationBlue: Color.lerp(explanationBlue, other.explanationBlue, t)!,
    );
  }
}
