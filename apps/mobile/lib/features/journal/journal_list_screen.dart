import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 묵상 노트 목록 화면.
/// TODO(김지민): GET /api/v1/journals?page&size → 카드 리스트.
class JournalListScreen extends ConsumerWidget {
  const JournalListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text('내 묵상 노트')),
      body: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: 0,
        separatorBuilder: (_, __) => const SizedBox(height: 12),
        itemBuilder: (_, __) => const SizedBox.shrink(),
      ),
    );
  }
}
