import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/network/dio_client.dart';

/// 오늘 QT 화면 (앱 첫 진입).
///
/// MVP UX (DECISIONS.md §3.1):
/// - 별도 홈 없음. 본 화면이 첫 화면.
/// - 본문 + 쉬운 설명 먼저 로딩, 묵상 노트/AI 세션 상태는 백그라운드.
/// - AI 질문/묵상 작성 버튼은 본 화면에서만 노출.
///
/// TODO(김지민):
/// - Riverpod AsyncNotifierProvider로 /api/v1/qt/today 호출
/// - Sliver scroll + skeleton loading
final todayQtProvider = FutureProvider<Map<String, dynamic>>((ref) async {
  final dio = ref.read(dioProvider);
  final res = await dio.get('/api/v1/qt/today');
  return Map<String, dynamic>.from(res.data as Map);
});

class TodayQtScreen extends ConsumerWidget {
  const TodayQtScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final today = ref.watch(todayQtProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('오늘의 QT'),
        actions: [
          IconButton(
            icon: const Icon(Icons.menu_book_outlined),
            onPressed: () => context.go('/bible/GEN/1/1'),
            tooltip: '성경 보기',
          ),
          IconButton(
            icon: const Icon(Icons.edit_note_outlined),
            onPressed: () => context.go('/journal'),
            tooltip: '묵상 노트',
          ),
        ],
      ),
      body: today.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('불러오기 실패: $e')),
        data: (qt) {
          final kr = qt['kr'] as Map?;
          final en = qt['en'] as Map?;
          final exp = qt['explanation'] as Map?;
          return ListView(
            padding: const EdgeInsets.all(20),
            children: [
              Text(
                '${kr?['bookNameKr'] ?? ''} ${qt['passage']?['chapter']}:${qt['passage']?['verse']}',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 12),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text(
                    kr?['content'] ?? '...',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                ),
              ),
              const SizedBox(height: 8),
              Text(en?['content'] ?? '', style: const TextStyle(color: Colors.grey)),
              const SizedBox(height: 24),
              if (exp != null) ...[
                Text('한 줄 요약', style: Theme.of(context).textTheme.titleSmall),
                Text(exp['summary'] ?? ''),
                const SizedBox(height: 12),
                Text('배경', style: Theme.of(context).textTheme.titleSmall),
                Text(exp['background'] ?? ''),
              ],
              const SizedBox(height: 32),
              FilledButton.icon(
                onPressed: () {
                  // TODO: POST /ai/sessions → context.go('/ai/$sessionId')
                },
                icon: const Icon(Icons.chat_bubble_outline),
                label: const Text('AI에게 질문하기'),
              ),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: () {
                  // TODO: POST /api/v1/journals/today → context.go('/journal/$id')
                },
                icon: const Icon(Icons.edit_outlined),
                label: const Text('묵상 노트 쓰기'),
              ),
            ],
          );
        },
      ),
    );
  }
}
