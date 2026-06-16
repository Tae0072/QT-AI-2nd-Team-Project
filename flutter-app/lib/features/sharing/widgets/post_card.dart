import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/utils/date_format_utils.dart';
import '../models/sharing_post_response.dart';
import 'sharing_feed_palette.dart';

/// 나눔 피드 카드 — 시안(흰 배경) 기준.
/// 작성자 + 카테고리 배지 + 시간 / 제목 / 본문 미리보기 / (본문 범위 인용 박스) / 좋아요·댓글.
class PostCard extends StatelessWidget {
  final SharingPostItem item;
  final VoidCallback onTap;

  /// 좋아요 토글(피드 낙관적 업데이트). null이면 하트 비활성(상세 등).
  final VoidCallback? onLike;

  /// 저장(북마크) 토글. null이면 저장 버튼 숨김.
  final VoidCallback? onBookmark;

  const PostCard({
    super.key,
    required this.item,
    required this.onTap,
    this.onLike,
    this.onBookmark,
  });

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final cat = _categoryStyle(item.category);
    final hasVerse = item.verseLabel != null;

    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 작성자 + 카테고리 배지(폭 공유, 길면 닉네임 말줄임) + 시간(끝 고정).
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
                          style: const TextStyle(
                              fontFamily: 'GowunDodum',
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: SharingFeedPalette.text),
                        ),
                      ),
                      const SizedBox(width: 8),
                      _CategoryBadge(
                          label: categoryLabel(l, item.category),
                          bg: cat.bg,
                          fg: cat.fg),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  relativeTimeLabel(item.publishedAt),
                  style: const TextStyle(
                      fontFamily: 'GowunDodum',
                      fontSize: 13,
                      color: SharingFeedPalette.muted),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              item.titleSnapshot,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                  fontFamily: 'GowunDodum',
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: SharingFeedPalette.text),
            ),
            if (item.bodyPreview.isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(
                item.bodyPreview,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                    fontFamily: 'GowunDodum',
                    fontSize: 14,
                    height: 1.55,
                    color: SharingFeedPalette.muted),
              ),
            ],
            // 본문 범위 인용 박스(범위 라벨이 있을 때만). 피드 API는 범위 라벨만 제공.
            if (hasVerse) ...[
              const SizedBox(height: 12),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
                decoration: BoxDecoration(
                  color: SharingFeedPalette.verseBoxBg,
                  borderRadius: BorderRadius.circular(10),
                  border: Border(left: BorderSide(color: cat.fg, width: 3)),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.format_quote_rounded, size: 16, color: cat.fg),
                    const SizedBox(width: 6),
                    Flexible(
                      child: Text(
                        item.verseLabel!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                            fontFamily: 'GowunDodum',
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: SharingFeedPalette.verseLabelText),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 12),
            Row(
              children: [
                // 하트는 카드 탭(상세 이동)과 분리된 InkWell로 좋아요만 처리.
                InkWell(
                  onTap: onLike,
                  borderRadius: BorderRadius.circular(20),
                  child: Padding(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          item.likedByMe
                              ? Icons.favorite
                              : Icons.favorite_border,
                          size: 15,
                          color: item.likedByMe
                              ? SharingFeedPalette.liked
                              : SharingFeedPalette.muted,
                        ),
                        const SizedBox(width: 4),
                        Text('${item.likeCount}',
                            style: const TextStyle(
                                fontFamily: 'GowunDodum',
                                fontSize: 13,
                                color: SharingFeedPalette.muted)),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                const Icon(Icons.chat_bubble_outline,
                    size: 15, color: SharingFeedPalette.muted),
                const SizedBox(width: 4),
                Text('${item.commentCount}',
                    style: const TextStyle(
                        fontFamily: 'GowunDodum',
                        fontSize: 13,
                        color: SharingFeedPalette.muted)),
                // 저장(북마크) 버튼 — 액션 줄 오른쪽 끝에 배치.
                if (onBookmark != null) ...[
                  const Spacer(),
                  InkWell(
                    onTap: onBookmark,
                    borderRadius: BorderRadius.circular(20),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 4, vertical: 4),
                      child: Icon(
                        item.bookmarkedByMe
                            ? Icons.bookmark
                            : Icons.bookmark_border,
                        size: 17,
                        color: item.bookmarkedByMe
                            ? SharingFeedPalette.text
                            : SharingFeedPalette.muted,
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// 카테고리 배지 (시안의 색상 캡슐). 카테고리별 파스텔 bg/fg.
class _CategoryBadge extends StatelessWidget {
  final String label;
  final Color bg;
  final Color fg;
  const _CategoryBadge(
      {required this.label, required this.bg, required this.fg});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(
            fontFamily: 'GowunDodum',
            fontSize: 11,
            fontWeight: FontWeight.w700,
            color: fg),
      ),
    );
  }
}

class _CatStyle {
  final Color bg;
  final Color fg;
  const _CatStyle(this.bg, this.fg);
}

/// 카테고리 코드 → 배지 색. 묵상은 시안과 동일한 보라, 나머지는 어울리는 파스텔.
_CatStyle _categoryStyle(String code) {
  switch (code) {
    case 'MEDITATION':
      return const _CatStyle(Color(0xFFEDEAFD), Color(0xFF6C56D6));
    case 'SERMON':
      return const _CatStyle(Color(0xFFE6F0FB), Color(0xFF3B7DD8));
    case 'PRAYER':
      return const _CatStyle(Color(0xFFE7F5EE), Color(0xFF2E9E6B));
    case 'GRATITUDE':
      return const _CatStyle(Color(0xFFFBF0DA), Color(0xFFB9821C));
    case 'REPENTANCE':
      return const _CatStyle(Color(0xFFFBE9EC), Color(0xFFC2476A));
    default:
      return const _CatStyle(Color(0xFFEDEAFD), Color(0xFF6C56D6));
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
