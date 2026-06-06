import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
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
  List<CommentItem> _comments = [];
  final _commentController = TextEditingController();
  bool _sendingComment = false;

  @override
  void initState() {
    super.initState();
    _loadDetail();
  }

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  Future<void> _loadDetail() async {
    setState(() => _isLoading = true);
    try {
      final repo = ref.read(sharingRepositoryProvider);
      final detail = await repo.getSharingPostDetail(widget.postId);
      // ✏️ 댓글이 켜진 글만 댓글을 불러온다(꺼진 글은 빈 목록 유지).
      final comments =
          detail.commentsEnabled ? await repo.getComments(widget.postId) : <CommentItem>[];
      if (mounted) {
        setState(() {
          _detail = detail;
          _comments = comments;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  /// 댓글 작성 → 목록·상세(댓글 수) 새로고침.
  Future<void> _submitComment() async {
    final body = _commentController.text.trim();
    if (body.isEmpty || _sendingComment) return;
    setState(() => _sendingComment = true);
    try {
      await ref.read(sharingRepositoryProvider).createComment(widget.postId, body);
      _commentController.clear();
      await _loadDetail(); // 댓글 목록 + commentCount 갱신
      ref.invalidate(sharingPostsProvider); // 피드의 댓글 수도 갱신
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppLocalizations.of(context).sharingCommentFailed)),
        );
      }
    } finally {
      if (mounted) setState(() => _sendingComment = false);
    }
  }

  /// 내 댓글 삭제.
  Future<void> _deleteComment(int commentId) async {
    try {
      await ref.read(sharingRepositoryProvider).deleteComment(commentId);
      await _loadDetail();
      ref.invalidate(sharingPostsProvider);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppLocalizations.of(context).sharingCommentDeleteFailed)),
        );
      }
    }
  }

  /// 신고 바텀시트 → 사유 선택 시 POST /reports.
  Future<void> _showReportSheet() async {
    final l = AppLocalizations.of(context);
    final reasons = {
      'SPAM': l.reportSpam,
      'HATE': l.reportHate,
      'SEXUAL': l.reportSexual,
      'ETC': l.reportEtc,
    };
    final reason = await showModalBottomSheet<String>(
      context: context,
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(l.sharingReportPrompt),
            ),
            for (final entry in reasons.entries)
              ListTile(
                title: Text(entry.value),
                onTap: () => Navigator.pop(context, entry.key),
              ),
          ],
        ),
      ),
    );
    if (reason == null || !mounted) return;
    try {
      await ref.read(sharingRepositoryProvider).report(
            targetType: 'POST',
            targetId: widget.postId,
            reason: reason,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.sharingReportSubmitted)),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.sharingReportFailed)),
        );
      }
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
          SnackBar(content: Text(_detail!.likedByMe ? AppLocalizations.of(context).sharingUnlikeFailed : AppLocalizations.of(context).sharingLikeFailed)),
        );
      }
    }
  }

  Future<void> _deletePost() async {
    final l = AppLocalizations.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l.sharingDeleteTitle),
        content: Text(l.sharingDeleteConfirmBody2),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: Text(l.commonCancel)),
          TextButton(onPressed: () => Navigator.pop(context, true), child: Text(l.commonDelete, style: const TextStyle(color: Colors.red))),
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
          SnackBar(content: Text(l.sharingDeleteFailed)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.sharingDetailTitle),
        centerTitle: true,
        actions: [
          if (_detail != null && _detail!.ownedByMe)
            IconButton(icon: const Icon(Icons.delete_outline), onPressed: _deletePost),
          IconButton(
            icon: const Icon(Icons.flag_outlined),
            tooltip: l.sharingReport,
            onPressed: _showReportSheet,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _detail == null
              ? Center(child: Text(l.sharingLoadFailed))
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
                        Text(l.sharingComments, style: theme.textTheme.titleMedium),
                        const SizedBox(height: 8),
                        // 댓글 입력 줄
                        Row(
                          children: [
                            Expanded(
                              child: TextField(
                                controller: _commentController,
                                decoration: InputDecoration(
                                  hintText: l.sharingCommentHint,
                                  isDense: true,
                                  border: const OutlineInputBorder(),
                                ),
                                minLines: 1,
                                maxLines: 3,
                              ),
                            ),
                            IconButton(
                              icon: _sendingComment
                                  ? const SizedBox(
                                      width: 18,
                                      height: 18,
                                      child: CircularProgressIndicator(strokeWidth: 2))
                                  : const Icon(Icons.send),
                              onPressed: _sendingComment ? null : _submitComment,
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        // 댓글 목록
                        if (_comments.isEmpty)
                          Padding(
                            padding: const EdgeInsets.all(16),
                            child: Text(l.sharingNoComments,
                                style: const TextStyle(color: Colors.grey)),
                          )
                        else
                          for (final c in _comments)
                            ListTile(
                              contentPadding: EdgeInsets.zero,
                              title: Text(c.nickname,
                                  style: theme.textTheme.bodySmall
                                      ?.copyWith(color: Colors.grey)),
                              subtitle: Text(c.body,
                                  style: theme.textTheme.bodyMedium),
                              trailing: c.ownedByMe
                                  ? IconButton(
                                      icon: const Icon(Icons.delete_outline, size: 20),
                                      onPressed: () => _deleteComment(c.id),
                                    )
                                  : null,
                            ),
                      ] else
                        Padding(
                          padding: const EdgeInsets.all(16),
                          child: Text(l.sharingCommentsDisabled, style: const TextStyle(color: Colors.grey)),
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
