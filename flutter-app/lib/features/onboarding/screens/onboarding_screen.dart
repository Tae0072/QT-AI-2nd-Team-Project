import 'package:flutter/material.dart';

import '../models/onboarding_page_data.dart';
import '../widgets/onboarding_page_widget.dart';
import '../widgets/page_indicator.dart';

/// 온보딩 화면 — 4페이지 스와이프 + Skip + dot indicator.
///
/// 최초 설치 시 1회만 표시되며, 완료 후 로그인 화면으로 이동한다.
/// [onComplete] 콜백을 통해 완료 처리를 외부에 위임한다.
class OnboardingScreen extends StatefulWidget {
  /// 온보딩 완료 시 호출되는 콜백 (SharedPreferences 저장 + 라우팅).
  final VoidCallback onComplete;

  /// 표시할 페이지 데이터 목록 (테스트 주입용).
  final List<OnboardingPageData> pages;

  const OnboardingScreen({
    super.key,
    required this.onComplete,
    this.pages = const [],
  });

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  List<OnboardingPageData> get _pages =>
      widget.pages.isNotEmpty ? widget.pages : OnboardingPageData.defaults;

  bool get _isLastPage => _currentPage == _pages.length - 1;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onPageChanged(int index) {
    setState(() => _currentPage = index);
  }

  void _onNext() {
    if (_isLastPage) {
      widget.onComplete();
    } else {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  void _onSkip() {
    widget.onComplete();
  }

  @override
  Widget build(BuildContext context) {
    final pages = _pages;
    return Scaffold(
      body: Stack(
        children: [
          // 페이지 뷰
          PageView.builder(
            controller: _pageController,
            onPageChanged: _onPageChanged,
            itemCount: pages.length,
            itemBuilder: (context, index) {
              return OnboardingPageWidget(data: pages[index]);
            },
          ),

          // 상단 Skip 버튼
          Positioned(
            top: MediaQuery.of(context).padding.top + 8,
            right: 16,
            child: AnimatedOpacity(
              opacity: _isLastPage ? 0.0 : 1.0,
              duration: const Duration(milliseconds: 200),
              child: TextButton(
                onPressed: _isLastPage ? null : _onSkip,
                child: Text(
                  '건너뛰기',
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.8),
                    fontSize: 16,
                  ),
                ),
              ),
            ),
          ),

          // 하단 영역: dot indicator + 버튼
          Positioned(
            left: 0,
            right: 0,
            bottom: MediaQuery.of(context).padding.bottom + 32,
            child: Column(
              children: [
                // Dot Indicator
                PageIndicator(
                  currentPage: _currentPage,
                  pageCount: pages.length,
                ),
                const SizedBox(height: 32),

                // 다음 / 시작하기 버튼
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 40),
                  child: SizedBox(
                    width: double.infinity,
                    height: 52,
                    child: ElevatedButton(
                      onPressed: _onNext,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.white,
                        foregroundColor: pages[_currentPage].backgroundColor,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(26),
                        ),
                        elevation: 0,
                      ),
                      child: Text(
                        _isLastPage ? '시작하기' : '다음',
                        style: const TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
