import 'package:flutter/material.dart';

import '../models/sharing_post_response.dart';

/// 댓글 1건 타일.
///
/// [onDelete]가 null이면 삭제 버튼을 표시하지 않는다(내 댓글일 때만 전달).
class SharingCommentTile extends StatelessWidget {
  final CommentItem comment;
  final VoidCallback? onDelete;

  const SharingCommentTile({
    super.key,
    required this.comment,
    this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: Text(comment.nickname,
          style: theme.textTheme.bodySmall?.copyWith(color: Colors.grey)),
      subtitle: Text(comment.body, style: theme.textTheme.bodyMedium),
      trailing: onDelete != null
          ? IconButton(
              icon: const Icon(Icons.delete_outline, size: 20),
              onPressed: onDelete,
            )
          : null,
    );
  }
}
