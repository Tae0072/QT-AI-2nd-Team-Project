import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/app_dimens.dart';
import '../../../core/widgets/common_widgets.dart';
import '../models/member_suggestion.dart';
import '../models/sharing_post_response.dart';
import '../providers/sharing_providers.dart';
import '../widgets/sharing_comment_input.dart';
import '../widgets/sharing_comment_tile.dart';
import '../widgets/sharing_snapshot_card.dart';

/// 나눔 상세 화면 (S-02).
///
/// ② Riverpod 일관화: 상세/댓글 조회를 [sharingPostDetailProvider]로 일원화한다.
/// (이전엔 화면이 직접 repository를 호출하고 setState로 보관했음)
/// 댓글 입력값/전송중 플래그만 순수 로컬 UI 상태로 setState를 유지한다.
class SharingDetailScreen extends ConsumerStatefulWidget {
  final int postId;

  const SharingDetailScreen({super.key, required this.postId});

  @override
  ConsumerState<SharingDetailScreen> createState() =>
      _SharingDetailScreenState();
}

class _SharingDetailScreenState extends ConsumerState<SharingDetailScreen> {
  final _commentController = TextEditingController();
  bool _sendingComment = false;

  // 댓글 '#닉네임' 자동완성 상태.
  List<MemberSuggestion> _mentionSuggestions = const [];
  Timer? _mentionDebounce;

  // '#' 뒤 닉네임 글자(0~20자) 매칭. 0자도 허용해 '#'만 입력해도 후보가 뜬다.
  static final RegExp _mentionPrefix = RegExp(r'(?:^|\s)#([0-9A-Za-z가-힣_]{0,20})$');
  static final RegExp _mentionReplace = RegExp(r'#([0-9A-Za-z가-힣_]{0,20})$');

  int get _postId => widget.postId;

  @override
  void initState() {
    super.initState();
    _commentController.addListener(_onCommentChanged);
  }

  @override
  void dispose() {
    _mentionDebounce?.cancel();
    _commentController.removeListener(_onCommentChanged);
    _commentController.dispose();
    super.dispose();
  }

  /// 커서 앞 텍스트가 '#닉네임'으로 끝나면 그 접두사를, 아니면 null.
  String? _currentMentionPrefix() {
    final sel = _commentController.selection;
    if (!sel.isValid || sel.baseOffset < 0) return null;
    final before = _commentController.text.substring(0, sel.baseOffset);
    return _mentionPrefix.firstMatch(before)?.group(1);
  }

  /// 입력이 바뀌면 '#접두사'를 감지해 닉네임 후보를 검색(200ms 디바운스)한다.
  void _onCommentChanged() {
    final prefix = _currentMentionPrefix();
    // prefix가 빈 문자열("")이면 '#'만 입력한 상태 → 기본 목록을 띄운다. null이면 멘션 아님.
    if (prefix == null) {
      if (_mentionSuggestions.isNotEmpty) {
        setState(() => _mentionSuggestions = const []);
      }
      return;
    }
    _mentionDebounce?.cancel();
    _mentionDebounce = Timer(const Duration(milliseconds: 200), () async {
      try {
        final results =
            await ref.read(sharingRepositoryProvider).searchMembers(prefix);
        if (mounted) setState(() => _mentionSuggestions = results);
      } catch (_) {
        // 자동완성 검색 실패는 조용히 무시(입력은 계속 가능).
      }
    });
  }

  /// 후보를 누르면 현재 '#접두사'를 '#닉네임 '으로 교체한다.
  void _insertMention(String nickname) {
    final sel = _commentController.selection;
    if (!sel.isValid || sel.baseOffset < 0) return;
    final full = _commentController.text;
    final before = full.substring(0, sel.baseOffset);
    final after = full.substring(sel.baseOffset);
    final m = _mentionReplace.firstMatch(before);
    if (m == null) return;
    final newBefore = '${before.substring(0, m.start)}#$nickname ';
    final newText = newBefore + after;
    _commentController.value = TextEditingValue(
      text: newText,
      selection: TextSelection.collapsed(offset: newBefore.length),
    );
    setState(() => _mentionSuggestions = const []);
  }

  /// 상세/댓글 + 피드를 모두 새로고침.
  void _refresh() {
    ref.invalidate(sharingPostDetailProvider(_postId));
    ref.invalidate(sharingPostsProvider);
  }

  Future<void> _submitComment() async {
    final l = AppLocalizations.of(context);
    final body = _commentController.text.trim();
    if (body.isEmpty || _sendingComment) return;
    setState(() => _sendingComment = true);
    try {
      await ref.read(sharingRepositoryProvider).createComment(_postId, body);
      _commentController.clear();
      _refresh();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.sharingCommentFailed)),
        );
      }
    } finally {
      if (mounted) setState(() => _sendingComment = false);
    }
  }

  Future<void> _deleteComment(int commentId) async {
    final l = AppLocalizations.of(context);
    try {
      await ref.read(sharingRepositoryProvider).deleteComment(commentId);
      _refresh();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.sharingCommentDeleteFailed)),
        );
      }
    }
  }

  Future<void> _toggleLike(SharingPostDetail detail) async {
    final l = AppLocalizations.of(context);
    try {
      // 낙관적 갱신: 본문·댓글 재조회 없이 좋아요만 즉시 바뀐다(실패 시 롤백).
      await ref.read(sharingPostDetailProvider(_postId).notifier).toggleLike();
      // 피드 목록도 좋아요 수가 어긋나지 않게 백그라운드로 동기화(상세 화면은 깜빡이지 않음).
      ref.invalidate(sharingPostsProvider);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text(detail.likedByMe
                  ? l.sharingUnlikeFailed
                  : l.sharingLikeFailed)),
        );
      }
    }
  }

  Future<void> _toggleBookmark(SharingPostDetail detail) async {
    try {
      // 낙관적 갱신: 저장 아이콘만 즉시 바뀐다(실패 시 롤백).
      await ref
          .read(sharingPostDetailProvider(_postId).notifier)
          .toggleBookmark();
      // 피드 목록의 저장 표시도 어긋나지 않게 백그라운드 동기화.
      ref.invalidate(sharingPostsProvider);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text(detail.bookmarkedByMe ? '저장 해제에 실패했어요.' : '저장에 실패했어요.')),
        );
      }
    }
  }

  /// 신고 사유 시트를 띄우고 접수한다. 게시글(POST)·댓글(COMMENT) 공용.
  Future<void> _showReportSheet({
    required String targetType,
    required int targetId,
  }) async {
    final l = AppLocalizations.of(context);
    final reasons = {
      'SPAM': l.reportSpam,
      'HATE': l.reportHate,
      'SEXUAL': l.reportSexual,
      'ETC': l.reportEtc,
    };
    final reason = await showModalBottomSheet<String>(
      context: context,
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: AppPad.all16,
              child: Text(targetType == 'COMMENT' ? '댓글을 신고합니다' : l.sharingReportPrompt),
            ),
            for (final entry in reasons.entries)
              ListTile(
                title: Text(entry.value),
                onTap: () => Navigator.pop(context, entry.key),
              ),
          ],
        ),
      ),
    );
    if (reason == null || !mounted) return;
    try {
      await ref.read(sharingRepositoryProvider).report(
            targetType: targetType,
            targetId: targetId,
            reason: reason,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.sharingReportSubmitted)),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.sharingReportFailed)),
        );
      }
    }
  }

  Future<void> _deletePost() async {
    final l = AppLocalizations.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l.sharingDeleteTitle),
        content: Text(l.sharingDeleteConfirmBody2),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(l.commonCancel)),
          TextButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(l.commonDelete,
                  style: const TextStyle(color: Colors.red))),
        ],
      ),
    );

    if (confirmed != true || !mounted) return;

    try {
      await ref.read(sharingRepositoryProvider).deletePost(_postId);
      ref.invalidate(sharingPostsProvider);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.sharingDeleteFailed)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final detailAsync = ref.watch(sharingPostDetailProvider(_postId));
    final detail = detailAsync.valueOrNull?.detail;

    return Scaffold(
      appBar: AppBar(
        title: Text(l.sharingDetailTitle),
        centerTitle: true,
        actions: [
          if (detail != null)
            IconButton(
              icon: Icon(detail.bookmarkedByMe
                  ? Icons.bookmark
                  : Icons.bookmark_border),
              tooltip: '저장',
              onPressed: () => _toggleBookmark(detail),
            ),
          if (detail != null && detail.ownedByMe)
            IconButton(
                icon: const Icon(Icons.delete_outline),
                onPressed: _deletePost),
          IconButton(
            icon: const Icon(Icons.flag_outlined),
            tooltip: l.sharingReport,
            onPressed: () =>
                _showReportSheet(targetType: 'POST', targetId: _postId),
          ),
        ],
      ),
      body: detailAsync.whenOrDefault(
        error: (_, __) => Center(child: Text(l.sharingLoadFailed)),
        data: (data) => _buildContent(l, theme, data.detail, data.comments),
      ),
    );
  }

  Widget _buildContent(AppLocalizations l, ThemeData theme,
      SharingPostDetail detail, List<CommentItem> comments) {
    return SingleChildScrollView(
      padding: AppPad.all16,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 스냅샷 카드
          SharingSnapshotCard(detail: detail),

          const SizedBox(height: 16),

          // 본문
          Text(detail.bodySnapshot,
              style: theme.textTheme.bodyLarge?.copyWith(height: 1.7)),

          const SizedBox(height: 24),

          // 좋아요 + 댓글 수
          Row(
            children: [
              IconButton(
                icon: Icon(
                  detail.likedByMe ? Icons.favorite : Icons.favorite_border,
                  color: detail.likedByMe ? Colors.red : Colors.grey,
                ),
                onPressed: () => _toggleLike(detail),
              ),
              Text('${detail.likeCount}'),
              const SizedBox(width: 16),
              const Icon(Icons.chat_bubble_outline, color: Colors.grey),
              const SizedBox(width: 4),
              Text('${detail.commentCount}'),
            ],
          ),

          const Divider(),

          // 댓글 영역
          if (detail.commentsEnabled) ...[
            Text(l.sharingComments, style: theme.textTheme.titleMedium),
            const SizedBox(height: 8),
            // '#닉네임' 자동완성 후보 — 입력 중 '#접두사'가 있을 때만.
            if (_mentionSuggestions.isNotEmpty)
              Container(
                margin: const EdgeInsets.only(bottom: 6),
                constraints: const BoxConstraints(maxHeight: 180),
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey.shade300),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: _mentionSuggestions.length,
                  itemBuilder: (context, i) {
                    final s = _mentionSuggestions[i];
                    return ListTile(
                      dense: true,
                      leading: const Icon(Icons.alternate_email, size: 18),
                      title: Text(s.nickname),
                      onTap: () => _insertMention(s.nickname),
                    );
                  },
                ),
              ),
            // 댓글 입력 줄
            SharingCommentInput(
              controller: _commentController,
              sending: _sendingComment,
              onSend: _submitComment,
            ),
            const SizedBox(height: 8),
            // 댓글 목록
            if (comments.isEmpty)
              Padding(
                padding: AppPad.all16,
                child: Text(l.sharingNoComments,
                    style: const TextStyle(color: Colors.grey)),
              )
            else
              for (final c in comments)
                SharingCommentTile(
                  comment: c,
                  onDelete: c.ownedByMe ? () => _deleteComment(c.id) : null,
                  // 남의 댓글만 신고 가능(내 댓글은 삭제).
                  onReport: c.ownedByMe
                      ? null
                      : () => _showReportSheet(targetType: 'COMMENT', targetId: c.id),
                ),
          ] else
            Padding(
              padding: AppPad.all16,
              child: Text(l.sharingCommentsDisabled,
                  style: const TextStyle(color: Colors.grey)),
            ),
        ],
      ),
    );
  }
}
