import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../providers/sharing_providers.dart';
import '../widgets/post_card.dart';

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

  // 시안 팔레트(이 화면 한정).
  static const Color _bg = Colors.white;
  static const Color _text = Color(0xFF1F1F1F);
  static const Color _muted = Color(0xFF8A8A8E);
  static const Color _fieldBg = Color(0xFFF4F3F1);
  static const Color _divider = Color(0xFFEFEDEA);
  static const Color _chipBorder = Color(0xFFE3E1DD);

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
      backgroundColor: _bg,
      appBar: AppBar(
        backgroundColor: _bg,
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
                  fontFamily: 'GowunDodum', fontSize: 14, color: _text),
              decoration: InputDecoration(
                hintText: l.sharingSearchHint,
                hintStyle: const TextStyle(
                    fontFamily: 'GowunDodum', fontSize: 14, color: _muted),
                prefixIcon: const Icon(Icons.search, color: _muted, size: 20),
                isDense: true,
                filled: true,
                fillColor: _fieldBg,
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
                  borderSide: const BorderSide(color: _text, width: 1),
                ),
              ),
              onSubmitted: (value) {
                ref.read(sharingQueryProvider.notifier).state =
                    value.trim().isEmpty ? null : value.trim();
              },
            ),
          ),

          // 카테고리 필터
          SizedBox(
            height: 44,
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
                            color: _muted)),
                  );
                }

                return RefreshIndicator(
                  onRefresh: () async => ref.invalidate(sharingPostsProvider),
                  child: ListView.separated(
                    itemCount: response.items.length,
                    padding: const EdgeInsets.only(bottom: 12),
                    separatorBuilder: (_, __) => const Divider(
                        height: 1, thickness: 1, color: _divider),
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
          ref.read(sharingCategoryFilterProvider.notifier).state = value;
        },
        child: Center(
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 120),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: isSelected
                  ? _SharingFeedScreenState._text
                  : Colors.white,
              borderRadius: BorderRadius.circular(999),
              border: Border.all(
                color: isSelected
                    ? _SharingFeedScreenState._text
                    : _SharingFeedScreenState._chipBorder,
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
                    color: isSelected
                        ? Colors.white
                        : _SharingFeedScreenState._muted,
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
