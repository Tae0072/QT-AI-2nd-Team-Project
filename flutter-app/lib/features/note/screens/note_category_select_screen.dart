import 'package:flutter/material.dart';

import '../../../routes/app_router.dart';
import '../models/note_models.dart';

/// 노트 카테고리 선택 화면 (N-02).
///
/// 자유 노트(기도/회개/감사) 중 하나를 고르면
/// 선택한 카테고리 코드를 arguments로 실어 N-03 작성 화면으로 이동한다.
class NoteCategorySelectScreen extends StatelessWidget {
  const NoteCategorySelectScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('새 노트'), centerTitle: true),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 8),
            child: Text('어떤 노트를 작성할까요?',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          ),
          // 작성 가능한 카테고리 목록(writableNoteCategories)을 돌며 버튼을 만든다.
          // 카테고리가 늘거나 줄어도 이 목록만 고치면 화면이 따라 바뀐다.
          for (final code in writableNoteCategories)
            Card(
              child: ListTile(
                leading: Icon(_iconFor(code)),
                title: Text(noteCategoryLabel(code)),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  // 선택한 카테고리 코드를 arguments로 실어 작성 화면으로 이동.
                  // 값이 이 이동에만 딸려가므로 잔여 상태가 남지 않는다.
                  Navigator.of(context).pushNamed(
                    AppRouter.noteEdit,
                    arguments: code,
                  );
                },
              ),
            ),
        ],
      ),
    );
  }

  // 카테고리별 아이콘 (단순 매핑).
  IconData _iconFor(String code) {
    switch (code) {
      case 'PRAYER':
        return Icons.volunteer_activism;
      case 'REPENTANCE':
        return Icons.self_improvement;
      case 'GRATITUDE':
        return Icons.favorite_outline;
      default:
        return Icons.edit_note;
    }
  }
}
