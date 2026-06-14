import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../providers/home_providers.dart';
import '../widgets/double_back_exit_scope.dart';
import 'home_dashboard_screen.dart';
import '../../bible/screens/bible_browser_screen.dart';
import '../../music/providers/music_providers.dart';
import '../../music/widgets/background_music_host.dart';
import '../../mypage/screens/mypage_screen.dart';
import '../../note/screens/note_list_screen.dart';
import '../../sharing/screens/sharing_feed_screen.dart';

/// 홈 화면 — 하단 탭바 5탭 (홈 / 성경 / 나눔 / 기록 / 마이).
///
/// 첫 탭은 랜딩 대시보드([HomeDashboardScreen]). 오늘 QT 본문은 대시보드의
/// '묵상 시작하기'에서 진입한다. 탭 인덱스는 [homeTabIndexProvider]로 관리해
/// 어느 화면에서든(예: 홈의 '모두 보기') 탭을 전환할 수 있다.
class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  // 탭 순서는 아래 BottomNavigationBar items와 동일해야 한다.
  static const _screens = [
    HomeDashboardScreen(),
    BibleBrowserScreen(),
    SharingFeedScreen(),
    NoteListScreen(),
    MyPageScreen(),
  ];

  /// 활성 탭 표시 — 라인 아이콘 그대로 두고 위에 빨간 도트(accentDot)만 켠다.
  Widget _navIcon(BuildContext context, IconData icon, int index, int current) {
    final active = current == index;
    final colors = context.appColors;
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 4,
          height: 4,
          margin: const EdgeInsets.only(bottom: 3),
          decoration: BoxDecoration(
            color: active ? colors.accentDot : Colors.transparent,
            shape: BoxShape.circle,
          ),
        ),
        Icon(icon),
      ],
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l = AppLocalizations.of(context);
    final currentIndex = ref.watch(homeTabIndexProvider);

    return DoubleBackExitScope(
      child: Scaffold(
        body: Listener(
          behavior: HitTestBehavior.translucent,
          // 웹 브라우저는 첫 제스처 전 자동재생이 막히므로 첫 터치에 재생을 시도한다.
          onPointerDown: (_) =>
              ref.read(musicControllerProvider.notifier).notifyUserGesture(),
          child: Stack(
            children: [
              IndexedStack(index: currentIndex, children: _screens),
              // 전역 배경음악 호스트(보이지 않음).
              const BackgroundMusicHost(),
            ],
          ),
        ),
        bottomNavigationBar: Container(
          decoration: BoxDecoration(
            border: Border(top: BorderSide(color: context.appColors.hairline)),
          ),
          child: BottomNavigationBar(
            currentIndex: currentIndex,
            onTap: (index) =>
                ref.read(homeTabIndexProvider.notifier).state = index,
            type: BottomNavigationBarType.fixed,
            items: [
              BottomNavigationBarItem(
                icon: _navIcon(context, Icons.home_outlined, 0, currentIndex),
                label: '홈',
              ),
              BottomNavigationBarItem(
                icon:
                    _navIcon(context, Icons.menu_book_outlined, 1, currentIndex),
                label: l.navBible,
              ),
              BottomNavigationBarItem(
                icon: _navIcon(
                    context, Icons.chat_bubble_outline, 2, currentIndex),
                label: l.navShare,
              ),
              BottomNavigationBarItem(
                icon:
                    _navIcon(context, Icons.edit_note_outlined, 3, currentIndex),
                label: l.navRecord,
              ),
              BottomNavigationBarItem(
                icon: _navIcon(context, Icons.person_outline, 4, currentIndex),
                label: l.navMy,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
