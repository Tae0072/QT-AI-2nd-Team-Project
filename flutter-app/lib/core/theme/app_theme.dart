import 'package:flutter/material.dart';

/// QT·AI 웜 파스텔 (Calm) 테마.
///
/// 디자인 토큰:
/// - 배경: 크림(#fbf7f0), 페이지(#efe7da)
/// - 액센트: 세이지 그린(#7c9a7e)
/// - 텍스트: 다크 브라운(#4a443c)
/// - 폰트: Gowun Dodum
/// - 카드: 라운드 24px, 그림자
class AppTheme {
  AppTheme._();

  // ── 색상 ──
  static const Color pageBg = Color(0xFFEFE7DA);
  static const Color bg = Color(0xFFFBF7F0);
  static const Color bgSunken = Color(0xFFF4ECDF);
  static const Color bgElevated = Color(0xFFFFFDF8);
  static const Color accent = Color(0xFF7C9A7E);
  static const Color accentSoft = Color(0xFFE4ECDF);
  static const Color text = Color(0xFF4A443C);
  static const Color text2 = Color(0xFF6F675C);
  static const Color textMuted = Color(0xFFA59A89);
  static const Color hairline = Color(0xFFE8DDCA);
  static const Color divider = Color(0xFFEFE6D6);
  static const Color heroBg = Color(0xFFE4ECE1);
  static const Color heroText = Color(0xFF3F4A3C);
  static const Color heroAccent = Color(0xFF5E7D61);

  // ── 테마 ──
  static ThemeData get theme => ThemeData(
    useMaterial3: true,
    fontFamily: 'GowunDodum',
    scaffoldBackgroundColor: bg,
    colorSchemeSeed: accent,
    brightness: Brightness.light,
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      titleTextStyle: TextStyle(
        fontFamily: 'GowunDodum',
        fontSize: 21,
        fontWeight: FontWeight.w400,
        color: text,
        letterSpacing: -0.2,
      ),
      iconTheme: IconThemeData(color: text),
    ),
    cardTheme: CardThemeData(
      color: bgElevated,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(24),
      ),
      shadowColor: Color(0x14786E5A),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: bgElevated,
      selectedColor: accent,
      labelStyle: const TextStyle(fontFamily: 'GowunDodum', fontSize: 14, color: text),
      shape: const StadiumBorder(side: BorderSide(color: hairline)),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: bgElevated,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(18),
        borderSide: const BorderSide(color: hairline),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(18),
        borderSide: const BorderSide(color: hairline),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(18),
        borderSide: const BorderSide(color: accent, width: 2),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: accent,
        foregroundColor: Colors.white,
        shape: const StadiumBorder(),
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 16),
        textStyle: const TextStyle(
          fontFamily: 'GowunDodum',
          fontSize: 17,
          fontWeight: FontWeight.w400,
        ),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: accent,
        shape: const StadiumBorder(),
        side: const BorderSide(color: hairline),
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 13),
      ),
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: accent,
      foregroundColor: Colors.white,
      shape: CircleBorder(),
      elevation: 8,
    ),
    bottomNavigationBarTheme: const BottomNavigationBarThemeData(
      backgroundColor: bg,
      selectedItemColor: accent,
      unselectedItemColor: textMuted,
      type: BottomNavigationBarType.fixed,
      selectedLabelStyle: TextStyle(fontFamily: 'GowunDodum', fontSize: 10),
      unselectedLabelStyle: TextStyle(fontFamily: 'GowunDodum', fontSize: 10),
    ),
    dividerTheme: const DividerThemeData(color: divider, thickness: 1),
    textTheme: const TextTheme(
      headlineLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 28, fontWeight: FontWeight.w400, color: text, letterSpacing: -0.4),
      headlineMedium: TextStyle(fontFamily: 'GowunDodum', fontSize: 24, fontWeight: FontWeight.w400, color: text),
      titleLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 21, fontWeight: FontWeight.w400, color: text),
      titleMedium: TextStyle(fontFamily: 'GowunDodum', fontSize: 17, fontWeight: FontWeight.w600, color: text),
      titleSmall: TextStyle(fontFamily: 'GowunDodum', fontSize: 14, fontWeight: FontWeight.w600, color: text),
      bodyLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 17, color: text, height: 1.65, letterSpacing: -0.2),
      bodyMedium: TextStyle(fontFamily: 'GowunDodum', fontSize: 15, color: text2, height: 1.6),
      bodySmall: TextStyle(fontFamily: 'GowunDodum', fontSize: 12, color: textMuted),
      labelLarge: TextStyle(fontFamily: 'GowunDodum', fontSize: 17, fontWeight: FontWeight.w400),
    ),
  );
}
