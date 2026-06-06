import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../models/sharing_post_response.dart';
import '../providers/sharing_providers.dart';

/// 내 나눔 관리 화면 (M-05).
///
/// - GET /me/sharing-posts(공개+숨김)를 mySharingPostsProvider로 조회
/// - 각 글: 상태 뱃지(공개중/숨김) + ⋮ 메뉴(상태별 숨김/되돌리기/삭제)
/// - 항목 탭 → 나눔 상세(S-02)
class MySharingScreen extends ConsumerWidget {
  const MySharingScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final postsAsync = ref.watch(mySharingPostsProvider);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(title: Text(l.sharingMine), centerTitle: true),
      body: postsAsync.whenOrDefault(
        data: (response) {
          if (response.items.isEmpty) {
            return EmptyView(message: l.sharingMineEmpty);
          }
          // ✏️ 당겨서 새로고침 — provider를 무효화하면 최신 목록을 다시 받는다.
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(mySharingPostsProvider),
            child: ListView.separated(
              itemCount: response.items.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, index) =>
                  _MyPostTile(post: response.items[index]),
            ),
          );
        },
      ),
    );
  }
}

/// 내 나눔 목록 1줄 (제목 + 상태/통계 + ⋮ 메뉴).
class _MyPostTile extends ConsumerWidget {
  final MySharingPostItem post;

  const _MyPostTile({required this.post});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    return ListTile(
      title: Text(
        post.titleSnapshot.isEmpty ? l.noteUntitled : post.titleSnapshot,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Row(
        children: [
          Text(_categoryLabel(post.category),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.primary)),
          const SizedBox(width: 8),
          // ✏️ 상태 뱃지: 숨김이면 회색 '숨김', 공개중이면 표시 안 함(기본).
          if (post.isHidden)
            Text(l.sharingHidden,
                style: theme.textTheme.bodySmall?.copyWith(color: Colors.grey)),
          const Spacer(),
          // 좋아요/댓글 수
          Icon(Icons.favorite_border, size: 14, color: Colors.grey.shade500),
          const SizedBox(width: 2),
          Text('${post.likeCount}', style: theme.textTheme.bodySmall),
          const SizedBox(width: 8),
          Icon(Icons.chat_bubble_outline, size: 14, color: Colors.grey.shade500),
          const SizedBox(width: 2),
          Text('${post.commentCount}', style: theme.textTheme.bodySmall),
        ],
      ),
      // ✏️ 상태에 따라 메뉴 항목이 달라진다(공개중=숨김/삭제, 숨김=되돌리기/삭제).
      trailing: PopupMenuButton<String>(
        onSelected: (action) => _onAction(context, ref, action),
        itemBuilder: (_) => [
          if (post.isHidden)
            PopupMenuItem(value: 'show', child: Text(l.sharingShow))
          else
            PopupMenuItem(value: 'hide', child: Text(l.sharingHide)),
          PopupMenuItem(value: 'delete', child: Text(l.commonDelete)),
        ],
      ),
      // 항목 탭 → 상세(S-02)
      onTap: () => Navigator.of(context)
          .pushNamed(AppRouter.sharingDetail, arguments: post.id),
    );
  }

  Future<void> _onAction(
      BuildContext context, WidgetRef ref, String action) async {
    final l = AppLocalizations.of(context);
    final repo = ref.read(sharingRepositoryProvider);
    try {
      switch (action) {
        case 'hide':
          await repo.hidePost(post.id);
          break;
        case 'show':
          await repo.showPost(post.id);
          break;
        case 'delete':
          final ok = await _confirmDelete(context);
          if (ok != true) return;
          await repo.deletePost(post.id);
          break;
      }
      // ✏️ 변경 성공: 내 목록과 공개 피드 둘 다 무효화(숨김/삭제는 피드에도 영향).
      ref.invalidate(mySharingPostsProvider);
      ref.invalidate(sharingPostsProvider);
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(l.sharingActionFailed)));
    }
  }

  /// 삭제 확인창 (08 §8.2: 되돌리기 어려운 동작은 확인 절차).
  Future<bool?> _confirmDelete(BuildContext context) {
    final l = AppLocalizations.of(context);
    return showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l.sharingDeleteConfirmTitle),
        content: Text(l.sharingDeleteConfirmBody),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: Text(l.commonCancel)),
          FilledButton(
              onPressed: () => Navigator.of(ctx).pop(true),
              child: Text(l.commonDelete)),
        ],
      ),
    );
  }

  String _categoryLabel(String category) {
    switch (category) {
      case 'MEDITATION':
        return '묵상';
      case 'SERMON':
        return '설교';
      case 'PRAYER':
        return '기도';
      case 'GRATITUDE':
        return '감사';
      case 'REPENTANCE':
        return '회개';
      default:
        return category;
    }
  }
}
