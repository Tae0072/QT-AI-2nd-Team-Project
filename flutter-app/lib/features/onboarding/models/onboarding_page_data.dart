import 'package:flutter/material.dart';

/// 온보딩 개별 페이지 데이터 모델.
class OnboardingPageData {
  final String title;
  final String description;
  final IconData icon;
  final Color backgroundColor;
  final Color iconColor;

  const OnboardingPageData({
    required this.title,
    required this.description,
    required this.icon,
    required this.backgroundColor,
    required this.iconColor,
  });

  /// 4페이지 기본 구성 — QT 묵상 / AI 해설 / 노트·공유 / 시작하기.
  static const List<OnboardingPageData> defaults = [
    OnboardingPageData(
      title: '매일 큐티와 함께',
      description: '오늘의 말씀과 묵상 본문을\n매일 아침 새롭게 만나보세요.',
      icon: Icons.menu_book_rounded,
      backgroundColor: Color(0xFF1A237E), // 남색
      iconColor: Color(0xFFB388FF),
    ),
    OnboardingPageData(
      title: 'AI가 도와주는 해설',
      description: '이해하기 어려운 구절도\nAI 해설로 쉽게 풀어드립니다.',
      icon: Icons.auto_awesome_rounded,
      backgroundColor: Color(0xFF283593), // 남색 밝은
      iconColor: Color(0xFFEA80FC),
    ),
    OnboardingPageData(
      title: '나만의 묵상 노트',
      description: '묵상 내용을 기록하고\n다른 사람들과 나눌 수 있어요.',
      icon: Icons.edit_note_rounded,
      backgroundColor: Color(0xFF303F9F), // 인디고
      iconColor: Color(0xFF80D8FF),
    ),
    OnboardingPageData(
      title: '지금 시작해볼까요?',
      description: '카카오 로그인으로\n간편하게 시작하세요.',
      icon: Icons.rocket_launch_rounded,
      backgroundColor: Color(0xFF3949AB), // 인디고 밝은
      iconColor: Color(0xFFFFD180),
    ),
  ];
}
