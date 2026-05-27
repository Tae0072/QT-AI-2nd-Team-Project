import 'package:flutter/material.dart';

import '../models/onboarding_page_data.dart';

/// 온보딩 개별 페이지 위젯.
///
/// 상단: 플레이스홀더 아이콘 영역, 하단: 제목 + 설명 텍스트.
/// 배경은 차분한 남색/보라 그라데이션.
class OnboardingPageWidget extends StatelessWidget {
  final OnboardingPageData data;

  const OnboardingPageWidget({super.key, required this.data});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            data.backgroundColor,
            data.backgroundColor.withValues(alpha:0.8),
          ],
        ),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            children: [
              const Spacer(flex: 2),

              // 플레이스홀더 이미지 영역 (나중에 디자이너가 교체)
              Container(
                width: 200,
                height: 200,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha:0.1),
                  borderRadius: BorderRadius.circular(32),
                ),
                child: Icon(
                  data.icon,
                  size: 96,
                  color: data.iconColor,
                ),
              ),

              const Spacer(flex: 1),

              // 제목
              Text(
                data.title,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                  height: 1.3,
                ),
              ),
              const SizedBox(height: 16),

              // 설명
              Text(
                data.description,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: Colors.white.withValues(alpha:0.85),
                  fontSize: 16,
                  height: 1.6,
                ),
              ),

              const Spacer(flex: 3),
            ],
          ),
        ),
      ),
    );
  }
}
