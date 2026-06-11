import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../bible/screens/bible_browser_screen.dart';
import '../../bible/screens/today_qt_screen.dart';
import '../../music/providers/music_providers.dart';
import '../../music/widgets/background_music_host.dart';
import '../../mypage/screens/mypage_screen.dart';
import '../../note/screens/note_list_screen.dart';
import '../../sharing/screens/sharing_feed_screen.dart';

/// 홈 화면 — 하단 탭바 5탭 (오늘 / 성경 / 나눔 / 노트 / 마이).
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

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
    return Scaffold(
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
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) => setState(() => _currentIndex = index),
        // 탭이 4개 이상이면 type: fixed를 줘야 라벨이 항상 보인다.
        type: BottomNavigationBarType.fixed,
        items: [
          BottomNavigationBarItem(
            icon: const Icon(Icons.wb_sunny_outlined),
            activeIcon: const Icon(Icons.wb_sunny),
            label: l.navToday,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.menu_book_outlined),
            activeIcon: const Icon(Icons.menu_book),
            label: l.navBible,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.chat_bubble_outline),
            activeIcon: const Icon(Icons.chat_bubble),
            label: l.navShare,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.edit_note_outlined),
            activeIcon: const Icon(Icons.edit_note),
            label: l.navRecord,
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.person_outline),
            activeIcon: const Icon(Icons.person),
            label: l.navMy,
          ),
        ],
      ),
    );
  }
}
