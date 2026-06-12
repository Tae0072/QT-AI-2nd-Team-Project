import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/utils/date_format_utils.dart';
import '../../../core/widgets/calm_paper.dart';
import '../models/sharing_post_response.dart';

/// 나눔 피드 카드 (DESIGN_PROTOTYPE `.post`):
/// 작성자 + 카테고리 배지 + 시간 / 제목 / 본문 미리보기 / 좋아요·댓글.
class PostCard extends StatelessWidget {
  final SharingPostItem item;
  final VoidCallback onTap;

  const PostCard({super.key, required this.item, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    final l = AppLocalizations.of(context);
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 작성자 + 카테고리 배지(남는 폭 공유, 길면 닉네임 말줄임) + 시간(끝 고정).
            // 닉네임·배지를 Expanded 안에 묶어 좁은 폭에서도 오버플로하지 않게 한다.
            Row(
              children: [
                Expanded(
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Flexible(
                        child: Text(
                          item.nicknameSnapshot,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                              fontFamily: 'GowunDodum',
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: c.text),
                        ),
                      ),
                      const SizedBox(width: 8),
                      CpBadge(categoryLabel(l, item.category)),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  relativeTimeLabel(item.publishedAt),
                  style: TextStyle(
                      fontFamily: 'GowunDodum', fontSize: 13, color: c.text2),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              item.titleSnapshot,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                  fontFamily: 'GowunDodum',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: c.text),
            ),
            if (item.bodyPreview.isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(
                item.bodyPreview,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                    fontFamily: 'GowunDodum',
                    fontSize: 14,
                    height: 1.55,
                    color: c.text2),
              ),
            ],
            const SizedBox(height: 12),
            Row(
              children: [
                Icon(
                  item.likedByMe ? Icons.favorite : Icons.favorite_border,
                  size: 15,
                  color: item.likedByMe ? c.accentDot : c.text2,
                ),
                const SizedBox(width: 4),
                Text('${item.likeCount}',
                    style: TextStyle(
                        fontFamily: 'GowunDodum', fontSize: 13, color: c.text2)),
                const SizedBox(width: 16),
                Icon(Icons.chat_bubble_outline, size: 15, color: c.text2),
                const SizedBox(width: 4),
                Text('${item.commentCount}',
                    style: TextStyle(
                        fontFamily: 'GowunDodum', fontSize: 13, color: c.text2)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// 나눔 카테고리 코드 → 한글 라벨.
String categoryLabel(AppLocalizations l, String code) {
  switch (code) {
    case 'MEDITATION':
      return l.catMeditation;
    case 'SERMON':
      return l.catSermon;
    case 'PRAYER':
      return l.catPrayer;
    case 'GRATITUDE':
      return l.catGratitude;
    case 'REPENTANCE':
      return l.catRepentance;
    default:
      return code;
  }
}
