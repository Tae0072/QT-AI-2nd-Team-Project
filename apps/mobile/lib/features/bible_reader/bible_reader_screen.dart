import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 일반 성경 보기 (읽기 전용).
///
/// MVP (DECISIONS.md §3.1): AI 시작/묵상 작성 버튼 없음.
/// TODO(김지민): GET /api/v1/passages/{bookCode}/{ch}/{v} 로 KR+EN+설명+해설 일괄.
class BibleReaderScreen extends ConsumerWidget {
  final String bookCode;
  final int chapter;
  final int verse;
  const BibleReaderScreen({
    super.key,
    required this.bookCode,
    required this.chapter,
    required this.verse,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: Text('$bookCode $chapter:$verse')),
      body: const Center(child: Text('TODO: 본문 + 해설')),
    );
  }
}
