import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../providers/sharing_providers.dart';
import '../widgets/post_card.dart';
import '../widgets/sharing_feed_palette.dart';

/// '#태그' 화면 — 내가 멘션(태그)된 나눔 글만 모아 본다.
///
/// 나눔 피드 카테고리 줄의 '#태그' 버튼으로 진입한다. 최근 글 순으로 보여주고,
/// 글을 누르면 상세로 이동한다(여기선 좋아요/저장 토글 없이 보기 전용).
class SharingMentionsScreen extends ConsumerWidget {
  const SharingMentionsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mentionsAsync = ref.watch(mentionsProvider);

    return Scaffold(
      backgroundColor: SharingFeedPalette.bg,
      appBar: AppBar(
        backgroundColor: SharingFeedPalette.bg,
        title: const Text('나를 태그한 글'),
        centerTitle: true,
      ),
      body: mentionsAsync.whenOrDefault(
        data: (response) {
          if (response.items.isEmpty) {
            return const Center(
              child: Text(
                '아직 나를 태그한 글이 없어요.\n누군가 글·댓글에서 #내닉네임으로 부르면 여기에 모여요.',
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
            onRefresh: () async => ref.invalidate(mentionsProvider),
            child: ListView.separated(
              itemCount: response.items.length,
              padding: const EdgeInsets.only(bottom: 12),
              separatorBuilder: (_, __) => const Divider(
                  height: 1, thickness: 1, color: SharingFeedPalette.divider),
              itemBuilder: (context, index) {
                final item = response.items[index];
                return PostCard(
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
    );
  }
}
