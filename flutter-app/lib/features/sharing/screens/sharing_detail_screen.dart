import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/sharing_post_response.dart';
import '../providers/sharing_providers.dart';

/// 나눔 상세 화면 (S-02).
///
/// - 본문 전체 표시
/// - 좋아요 토글 (UI만, API TODO)
/// - 댓글 목록 (UI만, API TODO)
/// - 신고 버튼 (UI만, API TODO)
class SharingDetailScreen extends ConsumerStatefulWidget {
  final int postId;

  const SharingDetailScreen({super.key, required this.postId});

  @override
  ConsumerState<SharingDetailScreen> createState() => _SharingDetailScreenState();
}

class _SharingDetailScreenState extends ConsumerState<SharingDetailScreen> {
  late Future<SharingPostDetail> _detailFuture;

  @override
  void initState() {
    super.initState();
    _detailFuture = ref.read(sharingRepositoryProvider).getSharingPostDetail(widget.postId);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('나눔 상세'),
        centerTitle: true,
        actions: [
          // 신고 버튼
          IconButton(
            icon: const Icon(Icons.flag_outlined),
            onPressed: () {
              // TODO: POST /reports API 연결
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('신고 기능은 준비 중입니다')),
              );
            },
          ),
        ],
      ),
      body: FutureBuilder<SharingPostDetail>(
        future: _detailFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text('오류: ${snapshot.error}'));
          }

          final detail = snapshot.data!;

          return SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 작성자 + 카테고리
                Row(
                  children: [
                    CircleAvatar(
                      radius: 18,
                      child: Text(detail.nicknameSnapshot.isNotEmpty
                          ? detail.nicknameSnapshot[0]
                          : '?'),
                    ),
                    const SizedBox(width: 8),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(detail.nicknameSnapshot, style: theme.textTheme.titleSmall),
                        Text(detail.category,
                            style: theme.textTheme.bodySmall?.copyWith(color: Colors.grey)),
                      ],
                    ),
                  ],
                ),

                const SizedBox(height: 16),

                // 제목
                Text(detail.titleSnapshot, style: theme.textTheme.titleLarge),

                const SizedBox(height: 12),

                // 본문
                Text(detail.bodySnapshot, style: theme.textTheme.bodyLarge?.copyWith(height: 1.7)),

                const SizedBox(height: 24),

                // 좋아요 + 댓글 수
                Row(
                  children: [
                    // 좋아요 버튼
                    IconButton(
                      icon: Icon(
                        detail.likedByMe ? Icons.favorite : Icons.favorite_border,
                        color: detail.likedByMe ? Colors.red : Colors.grey,
                      ),
                      onPressed: () {
                        // TODO: POST/DELETE /sharing-posts/{postId}/like API 연결
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('좋아요 기능은 준비 중입니다')),
                        );
                      },
                    ),
                    Text('${detail.likeCount}'),
                    const SizedBox(width: 16),
                    const Icon(Icons.chat_bubble_outline, color: Colors.grey),
                    const SizedBox(width: 4),
                    Text('${detail.commentCount}'),
                  ],
                ),

                const Divider(),

                // 댓글 영역
                if (detail.commentsEnabled) ...[
                  Text('댓글', style: theme.textTheme.titleMedium),
                  const SizedBox(height: 8),
                  // TODO: GET /sharing-posts/{postId}/comments API 연결
                  const Center(
                    child: Padding(
                      padding: EdgeInsets.all(16),
                      child: Text('댓글 기능은 준비 중입니다', style: TextStyle(color: Colors.grey)),
                    ),
                  ),
                ] else
                  const Padding(
                    padding: EdgeInsets.all(16),
                    child: Text('댓글이 비활성화된 글입니다', style: TextStyle(color: Colors.grey)),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}
