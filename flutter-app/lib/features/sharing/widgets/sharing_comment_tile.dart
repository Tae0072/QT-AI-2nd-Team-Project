import 'package:flutter/material.dart';

import '../models/sharing_post_response.dart';

/// 댓글 1건 타일.
///
/// 내 댓글이면 [onDelete](삭제), 남의 댓글이면 [onReport](신고)를 전달한다.
/// 둘 다 null이면 액션 버튼을 표시하지 않는다.
class SharingCommentTile extends StatelessWidget {
  final CommentItem comment;
  final VoidCallback? onDelete;
  final VoidCallback? onReport;

  const SharingCommentTile({
    super.key,
    required this.comment,
    this.onDelete,
    this.onReport,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: Text(comment.nickname,
          style: theme.textTheme.bodySmall?.copyWith(color: Colors.grey)),
      subtitle: Text(comment.body, style: theme.textTheme.bodyMedium),
      trailing: _trailing(),
    );
  }

  Widget? _trailing() {
    // 내 댓글: 삭제. 남의 댓글: 신고.
    if (onDelete != null) {
      return IconButton(
        icon: const Icon(Icons.delete_outline, size: 20),
        tooltip: '삭제',
        onPressed: onDelete,
      );
    }
    if (onReport != null) {
      return IconButton(
        icon: const Icon(Icons.flag_outlined, size: 20),
        tooltip: '신고',
        onPressed: onReport,
      );
    }
    return null;
  }
}
