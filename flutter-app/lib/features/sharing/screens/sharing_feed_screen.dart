import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/calm_paper.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../models/sharing_post_response.dart';
import '../providers/sharing_providers.dart';

/// 나눔 피드 화면 (S-01).
///
/// - 카테고리 필터 (전체/묵상/설교/기도/감사/회개)
/// - 텍스트 검색
/// - 목록 → 상세 이동
class SharingFeedScreen extends ConsumerStatefulWidget {
  const SharingFeedScreen({super.key});

  @override
  ConsumerState<SharingFeedScreen> createState() => _SharingFeedScreenState();
}

class _SharingFeedScreenState extends ConsumerState<SharingFeedScreen> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final postsAsync = ref.watch(sharingPostsProvider);
    final selectedCategory = ref.watch(sharingCategoryFilterProvider);
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.navShare),
        centerTitle: true,
        actions: [
          // 내 나눔 관리(M-05)로 진입 — 내가 공유한 글의 숨김/되돌리기/삭제.
          IconButton(
            tooltip: l.sharingMine,
            icon: const Icon(Icons.manage_accounts_outlined),
            onPressed: () =>
                Navigator.of(context).pushNamed(AppRouter.mySharing),
          ),
        ],
      ),
      body: Column(
        children: [
          // 검색바
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: l.sharingSearchHint,
                prefixIcon: const Icon(Icons.search),
                isDense: true,
              ),
              onSubmitted: (value) {
                ref.read(sharingQueryProvider.notifier).state =
                    value.trim().isEmpty ? null : value.trim();
              },
            ),
          ),

          // 카테고리 필터
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              children: [
                _CategoryChip(label: l.noteFilterAll, value: null, selected: selectedCategory),
                _CategoryChip(label: l.catMeditation, value: 'MEDITATION', selected: selectedCategory),
                _CategoryChip(label: l.catSermon, value: 'SERMON', selected: selectedCategory),
                _CategoryChip(label: l.catPrayer, value: 'PRAYER', selected: selectedCategory),
                _CategoryChip(label: l.catGratitude, value: 'GRATITUDE', selected: selectedCategory),
                _CategoryChip(label: l.catRepentance, value: 'REPENTANCE', selected: selectedCategory),
              ],
            ),
          ),

          const SizedBox(height: 8),

          // 목록
          Expanded(
            child: postsAsync.whenOrDefault(
              data: (response) {
                if (response.items.isEmpty) {
                  return Center(
                    child: Text(l.sharingFeedEmpty,
                        style: theme.textTheme.bodyMedium
                            ?.copyWith(color: theme.colorScheme.outline)),
                  );
                }

                return RefreshIndicator(
                  onRefresh: () async => ref.invalidate(sharingPostsProvider),
                  child: ListView.separated(
                    itemCount: response.items.length,
                    padding: const EdgeInsets.only(bottom: 12),
                    separatorBuilder: (_, __) => Divider(
                        height: 1, thickness: 1, color: context.appColors.hairline),
                    itemBuilder: (context, index) {
                      final item = response.items[index];
                      return _PostCard(
                        item: item,
                        onTap: () => Navigator.of(context).pushNamed(
                          AppRouter.sharingDetail,
                          arguments: item.id,
                        ),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _CategoryChip extends ConsumerWidget {
  final String label;
  final String? value;
  final String? selected;

  const _CategoryChip({required this.label, required this.value, required this.selected});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: FilterChip(
        label: Text(label),
        selected: selected == value,
        onSelected: (_) {
          ref.read(sharingCategoryFilterProvider.notifier).state = value;
        },
      ),
    );
  }
}

/// 나눔 피드 카드 (DESIGN_PROTOTYPE .post): 작성자·카테고리·시간 / 제목 / 본문 미리보기 / 좋아요·댓글.
class _PostCard extends StatelessWidget {
  final SharingPostItem item;
  final VoidCallback onTap;

  const _PostCard({required this.item, required this.onTap});

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
            // 작성자 + 카테고리 배지 + 시간
            Row(
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
                CpBadge(_categoryLabel(l, item.category)),
                const SizedBox(width: 8),
                Text(
                  _relativeTime(item.publishedAt),
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

/// 카테고리 코드 → 한글 라벨.
String _categoryLabel(AppLocalizations l, String code) {
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

/// 게시 시각 → 상대 시간 ("2시간 전" 등).
String _relativeTime(DateTime? t) {
  if (t == null) return '';
  final d = DateTime.now().difference(t);
  if (d.inMinutes < 1) return '방금';
  if (d.inMinutes < 60) return '${d.inMinutes}분 전';
  if (d.inHours < 24) return '${d.inHours}시간 전';
  if (d.inDays < 7) return '${d.inDays}일 전';
  return '${t.year}.${t.month.toString().padLeft(2, '0')}.${t.day.toString().padLeft(2, '0')}';
}
