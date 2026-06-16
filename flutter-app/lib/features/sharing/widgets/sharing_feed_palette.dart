import 'package:flutter/material.dart';

/// 나눔 피드 화면 전용 색 팔레트(흰 배경 시안).
///
/// 앱 전역 Calm Paper 토큰과 별개로, 피드 화면과 카드가 공유하는 색을 한 곳에 모은다.
/// (화면 간 `_text`/`_muted` 등 색 상수 중복 제거용 — 코드리뷰 보완)
class SharingFeedPalette {
  const SharingFeedPalette._();

  static const Color bg = Colors.white;
  static const Color text = Color(0xFF1F1F1F);
  static const Color muted = Color(0xFF8A8A8E);
  static const Color fieldBg = Color(0xFFF4F3F1);
  static const Color divider = Color(0xFFEFEDEA);
  static const Color chipBorder = Color(0xFFE3E1DD);
  static const Color verseBoxBg = Color(0xFFF6F5F3);
  static const Color verseLabelText = Color(0xFF5B5B60);
  static const Color liked = Color(0xFFE0492F);
}
