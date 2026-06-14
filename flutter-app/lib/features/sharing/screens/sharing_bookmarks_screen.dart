import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../providers/sharing_providers.dart';
import '../widgets/post_card.dart';
import '../widgets/sharing_feed_palette.dart';

/// 저장(북마크) 목록 화면. 나눔 피드 카테고리 줄의 '저장' 버튼으로 진입한다.
///
/// 내가 저장한 글을 최근 저장순으로 보여주고, 저장 아이콘을 다시 누르면
/// 목록에서 즉시 빠진다(낙관적 해제, 실패 시 복원).
class SharingBookmarksScreen extends ConsumerWidget {
  const SharingBookmarksScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final bookmarksAsync = ref.watch(bookmarksProvider);

    return Scaffold(
      backgroundColor: SharingFeedPalette.bg,
      appBar: AppBar(
        backgroundColor: SharingFeedPalette.bg,
        title: const Text('저장한 글'),
        centerTitle: true,
      ),
      body: bookmarksAsync.whenOrDefault(
        data: (response) {
          if (response.items.isEmpty) {
            return const Center(
              child: Text(
                '저장한 글이 없어요.\n나눔 글에서 북마크를 눌러 저장해 보세요.',
                textAlign: TextAlign.center,
                style: TextStyle(
                    fontFamily: 'GowunDodum',
                    fontSize: 14,
                    height: 1.6,
                    color: SharingFeedPalette.muted),
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(bookmarksProvider),
            child: ListView.separated(
              itemCount: response.items.length,
              padding: const EdgeInsets.only(bottom: 12),
              separatorBuilder: (_, __) => const Divider(
                  height: 1, thickness: 1, color: SharingFeedPalette.divider),
              itemBuilder: (context, index) {
                final item = response.items[index];
                return PostCard(
                  item: item,
                  // 저장 목록에서 북마크를 다시 누르면 목록에서 제거(낙관적), 실패 시 복원.
                  onBookmark: () async {
                    try {
                      await ref
                          .read(bookmarksProvider.notifier)
                          .removeBookmark(item.id);
                    } catch (_) {
                      if (!context.mounted) return;
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('저장 해제에 실패했어요.')),
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
    );
  }
}
