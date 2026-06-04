import 'package:flutter/material.dart';

import '../../bible/screens/today_qt_screen.dart';
import '../../mypage/screens/mypage_screen.dart';
import '../../note/screens/note_list_screen.dart';
import '../../sharing/screens/sharing_feed_screen.dart';

/// 홈 화면 — 하단 탭바 (QT / 노트 / 성경 / 나눔 / 마이페이지).
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  // 탭 순서: QT / 노트 / 성경 / 나눔 / 마이
  // 성경(B-01)은 이지윤 담당·미구현이라 임시 placeholder를 둔다.
  final _screens = const [
    TodayQtScreen(),
    NoteListScreen(),
    _BiblePlaceholderTab(),
    SharingFeedScreen(),
    MyPageScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _screens,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) => setState(() => _currentIndex = index),
        // 탭이 4개 이상이면 type: fixed를 줘야 라벨이 항상 보인다.
        // (기본값 shifting은 선택 안 된 탭의 라벨을 숨김)
        type: BottomNavigationBarType.fixed,
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.menu_book_outlined),
            activeIcon: Icon(Icons.menu_book),
            label: 'QT',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.edit_note_outlined),
            activeIcon: Icon(Icons.edit_note),
            label: '노트',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.import_contacts_outlined),
            activeIcon: Icon(Icons.import_contacts),
            label: '성경',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.people_outline),
            activeIcon: Icon(Icons.people),
            label: '나눔',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.person_outline),
            activeIcon: Icon(Icons.person),
            label: '마이',
          ),
        ],
      ),
    );
  }
}

/// 성경(B-01) 임시 placeholder.
/// TODO(이지윤): B-01 성경 탐색 화면이 나오면 이 위젯을 교체한다.
class _BiblePlaceholderTab extends StatelessWidget {
  const _BiblePlaceholderTab();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('성경'), centerTitle: true),
      body: const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.import_contacts_outlined, size: 48, color: Colors.grey),
            SizedBox(height: 12),
            Text('성경 화면 준비 중입니다', style: TextStyle(color: Colors.grey)),
          ],
        ),
      ),
    );
  }
}
