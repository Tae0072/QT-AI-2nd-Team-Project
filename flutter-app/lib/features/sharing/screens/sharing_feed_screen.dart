import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../providers/sharing_providers.dart';
import '../widgets/post_card.dart';

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
                _CategoryChip(
                    label: l.noteFilterAll,
                    value: null,
                    selected: selectedCategory),
                _CategoryChip(
                    label: l.catMeditation,
                    value: 'MEDITATION',
                    selected: selectedCategory),
                _CategoryChip(
                    label: l.catSermon,
                    value: 'SERMON',
                    selected: selectedCategory),
                _CategoryChip(
                    label: l.catPrayer,
                    value: 'PRAYER',
                    selected: selectedCategory),
                _CategoryChip(
                    label: l.catGratitude,
                    value: 'GRATITUDE',
                    selected: selectedCategory),
                _CategoryChip(
                    label: l.catRepentance,
                    value: 'REPENTANCE',
                    selected: selectedCategory),
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
                        height: 1,
                        thickness: 1,
                        color: context.appColors.hairline),
                    itemBuilder: (context, index) {
                      final item = response.items[index];
                      return PostCard(
                        item: item,
                        // 낙관적 좋아요: 즉시 갱신, 실패 시 롤백 + 안내(전체 재조회 없음).
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

  const _CategoryChip(
      {required this.label, required this.value, required this.selected});

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
