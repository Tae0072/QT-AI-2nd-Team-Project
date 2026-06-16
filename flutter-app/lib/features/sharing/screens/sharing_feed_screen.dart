import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../providers/sharing_providers.dart';
import '../widgets/page_navigator.dart';
import '../widgets/post_card.dart';
import '../widgets/sharing_feed_palette.dart';

/// 나눔 피드 화면 (S-01) — 시안(흰 배경) 기준.
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
    final l = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: SharingFeedPalette.bg,
      appBar: AppBar(
        backgroundColor: SharingFeedPalette.bg,
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
              style: const TextStyle(
                  fontFamily: 'GowunDodum',
                  fontSize: 14,
                  color: SharingFeedPalette.text),
              decoration: InputDecoration(
                hintText: l.sharingSearchHint,
                hintStyle: const TextStyle(
                    fontFamily: 'GowunDodum',
                    fontSize: 14,
                    color: SharingFeedPalette.muted),
                prefixIcon: const Icon(Icons.search,
                    color: SharingFeedPalette.muted, size: 20),
                isDense: true,
                filled: true,
                fillColor: SharingFeedPalette.fieldBg,
                contentPadding:
                    const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide:
                      const BorderSide(color: SharingFeedPalette.text, width: 1),
                ),
              ),
              onSubmitted: (value) {
                // 검색어가 바뀌면 1페이지부터 다시 본다.
                ref.read(sharingPageProvider.notifier).state = 0;
                ref.read(sharingQueryProvider.notifier).state =
                    value.trim().isEmpty ? null : value.trim();
              },
            ),
          ),

          // 카테고리 필터 + (오른쪽 고정) 저장 목록 버튼
          SizedBox(
            height: 44,
            child: Row(
              children: [
                Expanded(
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
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
                // 카테고리 줄 제일 오른쪽 — 저장한 글 / 나를 태그한 글 진입.
                Container(width: 1, height: 20, color: SharingFeedPalette.divider),
                _FeedActionButton(
                  icon: Icons.bookmark_border,
                  label: '저장',
                  onTap: () =>
                      Navigator.of(context).pushNamed(AppRouter.sharingBookmarks),
                ),
                _FeedActionButton(
                  icon: Icons.alternate_email,
                  label: '#태그',
                  trailingPadding: true,
                  onTap: () =>
                      Navigator.of(context).pushNamed(AppRouter.sharingMentions),
                ),
              ],
            ),
          ),

          const SizedBox(height: 4),

          // 목록
          Expanded(
            child: postsAsync.whenOrDefault(
              data: (response) {
                if (response.items.isEmpty) {
                  return Center(
                    child: Text(l.sharingFeedEmpty,
                        style: const TextStyle(
                            fontFamily: 'GowunDodum',
                            fontSize: 14,
                            color: SharingFeedPalette.muted)),
                  );
                }

                return RefreshIndicator(
                  onRefresh: () async => ref.invalidate(sharingPostsProvider),
                  child: ListView.separated(
                    itemCount: response.items.length,
                    padding: const EdgeInsets.only(bottom: 12),
                    separatorBuilder: (_, __) => const Divider(
                        height: 1, thickness: 1, color: SharingFeedPalette.divider),
                    itemBuilder: (context, index) {
                      final item = response.items[index];
                      return PostCard(
                        item: item,
                        // 좋아요 낙관적 업데이트: 즉시 반영, 실패 시 롤백(상태 provider 처리).
                        onLike: () async {
                          final wasLiked = item.likedByMe;
                          try {
                            await ref
                                .read(sharingPostsProvider.notifier)
                                .toggleLike(item.id);
                          } catch (_) {
                            if (!mounted) return;
                            ScaffoldMessenger.of(this.context).showSnackBar(
                              SnackBar(
                                content: Text(wasLiked
                                    ? l.sharingUnlikeFailed
                                    : l.sharingLikeFailed),
                              ),
                            );
                          }
                        },
                        // 저장(북마크) 토글 — 즉시 반영, 실패 시 롤백.
                        onBookmark: () async {
                          final wasBookmarked = item.bookmarkedByMe;
                          try {
                            await ref
                                .read(sharingPostsProvider.notifier)
                                .toggleBookmark(item.id);
                          } catch (_) {
                            if (!mounted) return;
                            ScaffoldMessenger.of(this.context).showSnackBar(
                              SnackBar(
                                content: Text(wasBookmarked
                                    ? '저장 해제에 실패했어요.'
                                    : '저장에 실패했어요.'),
                              ),
                            );
                          }
                        },
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

          // 목차 번호 페이저(10개씩, 번호 5개씩). 페이지를 누르면 해당 페이지를 불러온다.
          // 현재 페이지는 응답이 아니라 상태(provider)에서 읽어 항상 정확하고,
          // totalPages는 ?? 1로 안전하게 처리한다(핫 리로드 등으로 비어도 크래시하지 않음).
          PageNavigator(
            currentPage: ref.watch(sharingPageProvider),
            totalPages: postsAsync.valueOrNull?.totalPages ?? 1,
            onSelect: (page) {
              ref.read(sharingPageProvider.notifier).state = page;
            },
          ),
        ],
      ),
    );
  }
}

/// 카테고리 줄 오른쪽 액션 버튼(저장 / #태그). 아이콘 + 라벨.
class _FeedActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool trailingPadding;

  const _FeedActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
    this.trailingPadding = false,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(left: 2, right: trailingPadding ? 12 : 2),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(999),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 18, color: SharingFeedPalette.text),
              const SizedBox(width: 4),
              Text(label,
                  style: const TextStyle(
                      fontFamily: 'GowunDodum',
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: SharingFeedPalette.text)),
            ],
          ),
        ),
      ),
    );
  }
}

/// 카테고리 필터 칩 (시안): 선택=검정 채움+체크, 비선택=흰 배경+회색 테두리.
class _CategoryChip extends ConsumerWidget {
  final String label;
  final String? value;
  final String? selected;

  const _CategoryChip(
      {required this.label, required this.value, required this.selected});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isSelected = selected == value;
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: GestureDetector(
        onTap: () {
          // 카테고리가 바뀌면 1페이지부터 다시 본다.
          ref.read(sharingPageProvider.notifier).state = 0;
          ref.read(sharingCategoryFilterProvider.notifier).state = value;
        },
        child: Center(
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 120),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: isSelected ? SharingFeedPalette.text : Colors.white,
              borderRadius: BorderRadius.circular(999),
              border: Border.all(
                color: isSelected
                    ? SharingFeedPalette.text
                    : SharingFeedPalette.chipBorder,
              ),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (isSelected) ...[
                  const Icon(Icons.check, size: 15, color: Colors.white),
                  const SizedBox(width: 4),
                ],
                Text(
                  label,
                  style: TextStyle(
                    fontFamily: 'GowunDodum',
                    fontSize: 14,
                    fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                    color: isSelected ? Colors.white : SharingFeedPalette.muted,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
