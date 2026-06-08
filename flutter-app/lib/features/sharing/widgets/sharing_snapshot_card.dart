import 'package:flutter/material.dart';

import '../../../core/constants/post_category.dart';
import '../../../core/theme/app_dimens.dart';
import '../models/sharing_post_response.dart';

/// 나눔 글 스냅샷 카드 — 작성자/카테고리/제목 요약.
class SharingSnapshotCard extends StatelessWidget {
  final SharingPostDetail detail;

  const SharingSnapshotCard({super.key, required this.detail});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.md),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Padding(
        padding: AppPad.all16,
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
                AppGap.w8,
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(detail.nicknameSnapshot,
                        style: theme.textTheme.titleSmall),
                    Text(postCategoryLabel(detail.category),
                        style: theme.textTheme.bodySmall
                            ?.copyWith(color: Colors.grey)),
                  ],
                ),
              ],
            ),
            AppGap.h12,
            // 제목
            Text(detail.titleSnapshot,
                style: theme.textTheme.titleMedium
                    ?.copyWith(fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }
}
