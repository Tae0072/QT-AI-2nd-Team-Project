import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../widgets/double_back_exit_scope.dart';
import '../../bible/screens/bible_browser_screen.dart';
import '../../bible/screens/today_qt_screen.dart';
import '../../music/providers/music_providers.dart';
import '../../music/widgets/background_music_host.dart';
import '../../mypage/screens/mypage_screen.dart';
import '../../note/screens/note_list_screen.dart';
import '../../sharing/screens/sharing_feed_screen.dart';

/// 홈 화면 — 하단 탭바 5탭 (QT / 성경 / 나눔 / 노트 / 마이).
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  /// 활성 탭 표시 — 라인 아이콘 그대로 두고 위에 빨간 도트(AppTheme.accentDot)만 켠다.
  /// (Calm Paper §3: filled 아이콘 전환 금지, 유채색 포인트는 이 도트 하나뿐)
  Widget _navIcon(IconData icon, int index) {
    final active = _currentIndex == index;
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 4,
          height: 4,
          margin: const EdgeInsets.only(bottom: 3),
          decoration: BoxDecoration(
            color: active ? AppTheme.accentDot : Colors.transparent,
            shape: BoxShape.circle,
          ),
        ),
        Icon(icon),
      ],
    );
  }

  // 탭 순서는 아래 BottomNavigationBar items와 동일해야 한다.
  final _screens = const [
    TodayQtScreen(),
    BibleBrowserScreen(),
    SharingFeedScreen(),
    NoteListScreen(),
    MyPageScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    // 뒤로가기 2번 종료 — 다섯 탭 어디서든 동일. 홈 위에 push된 상세 화면들(알림·설정·
    // 노트 작성 등)은 자기 라우트가 pop을 처리해 부모로 정상 복귀한다(스코프 javadoc 참조).
    return DoubleBackExitScope(
      child: Scaffold(
      body: Consumer(
        builder: (context, ref, child) => Listener(
          behavior: HitTestBehavior.translucent,
          // 웹 브라우저는 사용자 제스처 전 자동재생이 막히므로 첫 터치에 재생을 시도한다.
          onPointerDown: (_) =>
              ref.read(musicControllerProvider.notifier).notifyUserGesture(),
          child: child,
        ),
        child: Stack(
          children: [
            IndexedStack(
              index: _currentIndex,
              children: _screens,
            ),
            // 전역 배경음악 호스트(보이지 않음) — 메인 진입 후 자동재생, 탭 이동·하위 화면에서도 유지.
            const BackgroundMusicHost(),
          ],
        ),
      ),
      // Calm Paper(§3): 활성 탭은 filled 아이콘 전환 금지 — 라인 아이콘 유지 + 빨간 도트.
      // 상단 헤어라인 1px은 Container border로 표현(테마 elevation 0).
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          border: Border(top: BorderSide(color: AppTheme.hairline)),
        ),
        child: BottomNavigationBar(
          currentIndex: _currentIndex,
          onTap: (index) => setState(() => _currentIndex = index),
          // 탭이 4개 이상이면 type: fixed를 줘야 라벨이 항상 보인다.
          type: BottomNavigationBarType.fixed,
          items: [
            BottomNavigationBarItem(
              icon: _navIcon(Icons.wb_sunny_outlined, 0),
              label: l.navToday,
            ),
            BottomNavigationBarItem(
              icon: _navIcon(Icons.menu_book_outlined, 1),
              label: l.navBible,
            ),
            BottomNavigationBarItem(
              icon: _navIcon(Icons.chat_bubble_outline, 2),
              label: l.navShare,
            ),
            BottomNavigationBarItem(
              icon: _navIcon(Icons.edit_note_outlined, 3),
              label: l.navNote,
            ),
            BottomNavigationBarItem(
              icon: _navIcon(Icons.person_outline, 4),
              label: l.navMy,
            ),
          ],
          ),
        ),
      ),
    );
  }
}
