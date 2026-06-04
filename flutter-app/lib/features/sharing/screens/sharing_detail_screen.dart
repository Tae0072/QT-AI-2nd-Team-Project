import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/sharing_post_response.dart';
import '../providers/sharing_providers.dart';

/// 나눔 상세 화면 (S-02).
class SharingDetailScreen extends ConsumerStatefulWidget {
  final int postId;

  const SharingDetailScreen({super.key, required this.postId});

  @override
  ConsumerState<SharingDetailScreen> createState() => _SharingDetailScreenState();
}

class _SharingDetailScreenState extends ConsumerState<SharingDetailScreen> {
  SharingPostDetail? _detail;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadDetail();
  }

  Future<void> _loadDetail() async {
    setState(() => _isLoading = true);
    try {
      final detail = await ref.read(sharingRepositoryProvider).getSharingPostDetail(widget.postId);
      if (mounted) setState(() { _detail = detail; _isLoading = false; });
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _toggleLike() async {
    if (_detail == null) return;
    final repo = ref.read(sharingRepositoryProvider);
    try {
      if (_detail!.likedByMe) {
        await repo.unlike(_detail!.id);
      } else {
        await repo.like(_detail!.id);
      }
      await _loadDetail();
      ref.invalidate(sharingPostsProvider);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(_detail!.likedByMe ? '좋아요 취소에 실패했습니다' : '좋아요에 실패했습니다')),
        );
      }
    }
  }

  Future<void> _deletePost() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('나눔 글 삭제'),
        content: const Text('삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('취소')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('삭제', style: TextStyle(color: Colors.red))),
        ],
      ),
    );

    if (confirmed != true || !mounted) return;

    try {
      await ref.read(sharingRepositoryProvider).deletePost(_detail!.id);
      ref.invalidate(sharingPostsProvider);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('삭제에 실패했습니다')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('나눔 상세'),
        centerTitle: true,
        actions: [
          if (_detail != null && _detail!.ownedByMe)
            IconButton(icon: const Icon(Icons.delete_outline), onPressed: _deletePost),
          IconButton(
            icon: const Icon(Icons.flag_outlined),
            onPressed: () {
              // TODO: POST /reports API
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('신고 기능은 준비 중입니다')),
              );
            },
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _detail == null
              ? const Center(child: Text('글을 불러올 수 없습니다'))
              : SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // 스냅샷 카드
                      _SnapshotCard(detail: _detail!),

                      const SizedBox(height: 16),

                      // 본문
                      Text(_detail!.bodySnapshot, style: theme.textTheme.bodyLarge?.copyWith(height: 1.7)),

                      const SizedBox(height: 24),

                      // 좋아요 + 댓글 수
                      Row(
                        children: [
                          IconButton(
                            icon: Icon(
                              _detail!.likedByMe ? Icons.favorite : Icons.favorite_border,
                              color: _detail!.likedByMe ? Colors.red : Colors.grey,
                            ),
                            onPressed: _toggleLike,
                          ),
                          Text('${_detail!.likeCount}'),
                          const SizedBox(width: 16),
                          const Icon(Icons.chat_bubble_outline, color: Colors.grey),
                          const SizedBox(width: 4),
                          Text('${_detail!.commentCount}'),
                        ],
                      ),

                      const Divider(),

                      // 댓글 영역
                      if (_detail!.commentsEnabled) ...[
                        Text('댓글', style: theme.textTheme.titleMedium),
                        const SizedBox(height: 8),
                        // TODO: 댓글 API 연결
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
                ),
    );
  }
}

/// 나눔 글 스냅샷 카드 — 작성자/제목/카테고리 요약.
class _SnapshotCard extends StatelessWidget {
  final SharingPostDetail detail;

  const _SnapshotCard({required this.detail});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 작성자
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
                    Text(_categoryLabel(detail.category),
                        style: theme.textTheme.bodySmall?.copyWith(color: Colors.grey)),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 12),
            // 제목
            Text(detail.titleSnapshot, style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }

  String _categoryLabel(String category) {
    switch (category) {
      case 'MEDITATION': return '묵상';
      case 'SERMON': return '설교';
      case 'PRAYER': return '기도';
      case 'GRATITUDE': return '감사';
      case 'REPENTANCE': return '회개';
      default: return category;
    }
  }
}
