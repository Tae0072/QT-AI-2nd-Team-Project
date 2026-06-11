import 'package:flutter/material.dart';

/// QT·AI "Calm Paper" 테마 (DESIGN_PROMPT.md, 2026-06-11 적용).
///
/// 컨셉: 따뜻한 종이 위의 미니멀 묵상 — 웜 오프화이트 배경 + 잉크 차콜 텍스트,
/// 위계는 색이 아니라 명도·크기·여백으로 만든다. 8색 토큰 외 유채색·그라데이션 금지.
///
/// 디자인 토큰(§2):
/// - 배경 #F7F5F2 / 카드·선택칩·시트 #FFFFFF / 보조박스 #F0EEEA
/// - 주텍스트 #1F1F1F / 보조 #8A8A8E / 3차 #B8B5B0 / 헤어라인 #E8E6E2
/// - 포인트 #E0492F 는 **활성 탭 도트 전용**([accentDot]) — 일반 강조([accent])는
///   잉크 차콜로 통일해 "한 화면에 유채색 1곳 이하" 규칙을 지킨다.
/// - 반경: 칩 stadium / 보조박스 14 / 시트 상단 24. 그림자는 선택 칩·시트만.
class AppTheme {
  AppTheme._();

  // ── 색상 (Calm Paper 8토큰) ──
  static const Color pageBg = Color(0xFFF0EEEA);
  static const Color bg = Color(0xFFF7F5F2);
  static const Color bgSunken = Color(0xFFF0EEEA);
  static const Color bgElevated = Color(0xFFFFFFFF);

  /// 일반 강조(버튼·선택·포커스) — 잉크 차콜. 유채색 강조는 쓰지 않는다.
  static const Color accent = Color(0xFF1F1F1F);
  static const Color accentSoft = Color(0xFFF0EEEA);

  /// 활성 탭 도트 **전용** 포인트 색 — 화면당 1곳 이하로만 사용한다(§2).
  static const Color accentDot = Color(0xFFE0492F);

  static const Color text = Color(0xFF1F1F1F);
  static const Color text2 = Color(0xFF8A8A8E);
  static const Color textMuted = Color(0xFFB8B5B0);
  static const Color hairline = Color(0xFFE8E6E2);
  static const Color divider = Color(0xFFE8E6E2);

  // 히어로 영역도 무채색 토큰으로 통일(어두운 블록 금지).
  static const Color heroBg = Color(0xFFF0EEEA);
  static const Color heroText = Color(0xFF1F1F1F);
  static const Color heroAccent = Color(0xFF1F1F1F);

  // ── 테마 ──
  static ThemeData get theme => ThemeData(
    useMaterial3: true,
    fontFamily: 'GowunDodum',
    scaffoldBackgroundColor: bg,
    colorSchemeSeed: accent,
    brightness: Brightness.light,
    // 앱바: 투명 배경, elevation 0, 17 w600 중앙(§3 titleBar).
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      titleTextStyle: TextStyle(
        fontFamily: 'GowunDodum',
        fontSize: 17,
        fontWeight: FontWeight.w600,
        color: text,
        letterSpacing: -0.2,
      ),
      iconTheme: IconThemeData(color: text),
    ),
    // 카드 = 흰 면 + 헤어라인 + 반경 14(보조박스 규격). 그림자 금지(§2).
    cardTheme: CardThemeData(
      color: bgElevated,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: hairline),
      ),
      shadowColor: Colors.transparent,
    ),
    // 칩: stadium 필. 선택 = 흰 배경 + 헤어라인(+w600은 화면측), 비선택 = 투명 + 보조색(§3).
    chipTheme: ChipThemeData(
      backgroundColor: bg,
      selectedColor: bgElevated,
      labelStyle: const TextStyle(fontFamily: 'GowunDodum', fontSize: 14, color: text2),
      secondaryLabelStyle:
          const TextStyle(fontFamily: 'GowunDodum', fontSize: 14, color: text),
      checkmarkColor: text,
      shape: const StadiumBorder(side: BorderSide(color: hairline)),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: bgElevated,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: hairline),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: hairline),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: accent, width: 1.5),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
    ),
    // 주 버튼: 잉크 필 + 흰 글자(프로토타입의 검정 필 버튼).
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: accent,
        foregroundColor: Colors.white,
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
        foregroundColor: text,
        shape: const StadiumBorder(),
        side: const BorderSide(color: hairline),
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 13),
      ),
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: accent,
      foregroundColor: Colors.white,
      shape: CircleBorder(),
      elevation: 0,
    ),
    // 탭바: 흰 면(상단 헤어라인은 화면측), 활성 = 잉크, 비활성 = 3차색.
    // 활성 표시는 filled 아이콘 전환 금지 — 빨간 도트([accentDot])로 한다(§3, home_screen).
    bottomNavigationBarTheme: const BottomNavigationBarThemeData(
      backgroundColor: bgElevated,
      selectedItemColor: text,
      unselectedItemColor: textMuted,
      type: BottomNavigationBarType.fixed,
      elevation: 0,
      selectedLabelStyle: TextStyle(
          fontFamily: 'GowunDodum', fontSize: 11, fontWeight: FontWeight.w600),
      unselectedLabelStyle: TextStyle(fontFamily: 'GowunDodum', fontSize: 11),
    ),
    dividerTheme: const DividerThemeData(color: divider, thickness: 1),
    // 타이포(§2): titleXL 26 w700 / body 16·1.65 / bodySub 14·1.55 / caption 13 / label 12.
    textTheme: const TextTheme(
      headlineLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 26, fontWeight: FontWeight.w700, color: text, letterSpacing: -0.5),
      headlineMedium: TextStyle(fontFamily: 'GowunDodum', fontSize: 22, fontWeight: FontWeight.w700, color: text, letterSpacing: -0.4),
      titleLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 17, fontWeight: FontWeight.w600, color: text),
      titleMedium: TextStyle(fontFamily: 'GowunDodum', fontSize: 16, fontWeight: FontWeight.w600, color: text),
      titleSmall: TextStyle(fontFamily: 'GowunDodum', fontSize: 13, fontWeight: FontWeight.w600, color: text2),
      bodyLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 16, color: text, height: 1.65, letterSpacing: -0.2),
      bodyMedium: TextStyle(fontFamily: 'GowunDodum', fontSize: 14, color: text2, height: 1.55),
      bodySmall: TextStyle(fontFamily: 'GowunDodum', fontSize: 13, color: text2),
      labelLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 16, fontWeight: FontWeight.w600),
      labelSmall: TextStyle(fontFamily: 'GowunDodum', fontSize: 12, fontWeight: FontWeight.w500, color: textMuted),
    ),
  );
}
