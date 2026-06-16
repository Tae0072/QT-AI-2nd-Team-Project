import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../../sharing/providers/sharing_providers.dart';
import '../models/note_models.dart';
import '../models/qt_note_rich_text.dart';
import '../providers/note_providers.dart';
import '../widgets/note_publish_sheet.dart';
import '../widgets/note_share_sheet.dart';

/// 노트 상세 화면 (N-04).
///
/// - GET /notes/{id} 상세를 noteDetailProvider(id)로 조회
/// - 자유노트: 본문(body) / 묵상노트: 4섹션 표시
/// - 수정: 자유노트만 [수정] 노출 → N-03 편집모드. 묵상은 보기+삭제만(수정은 v2 TODO)
/// - 삭제: 확인창 → DELETE → 목록 새로고침 후 뒤로
class NoteDetailScreen extends ConsumerWidget {
  final int noteId;

  const NoteDetailScreen({super.key, required this.noteId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // family provider에 noteId를 넘겨 이 노트 전용 상세를 watch한다.
    final detailAsync = ref.watch(noteDetailProvider(noteId));
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.noteListTitle),
        centerTitle: true,
        // 액션 버튼은 상세를 받은 뒤에야 카테고리를 알 수 있으므로,
        // detail이 data일 때만 [수정]/[삭제]를 만든다(로딩/에러 땐 안 보임).
        actions: [
          detailAsync.maybeWhen(
            data: (detail) => _Actions(noteId: noteId, detail: detail),
            orElse: () => const SizedBox.shrink(),
          ),
        ],
      ),
      body: detailAsync.whenOrDefault(
        data: (detail) => _DetailBody(detail: detail),
      ),
    );
  }
}

/// AppBar 우측 [수정]/[삭제] 버튼 묶음.
class _Actions extends ConsumerWidget {
  final int noteId;
  final NoteDetail detail;

  const _Actions({required this.noteId, required this.detail});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l = AppLocalizations.of(context);
    return Row(
      children: [
        // 닉네임 나눔 공개(앱 안 피드) — 아직 공개되지 않은 노트에만 노출.
        // 외부 OS 공유(아래 ios_share)와 구분되는 별개 동작이다(F-10).
        if (!detail.shared)
          IconButton(
            tooltip: l.notePublishTooltip,
            icon: const Icon(Icons.public),
            onPressed: () => _confirmPublish(context, ref),
          ),
        // 외부 공유는 카테고리 무관 제공(텍스트/카드 이미지).
        IconButton(
          tooltip: l.noteShareTooltip,
          icon: const Icon(Icons.ios_share),
          onPressed: () => showNoteShareSheet(context, detail),
        ),
        // 모든 노트는 단일 body라 N-03에서 수정 가능하다(작성=각 탭, 수정·삭제=기록).
        // QT·설교의 인용 절(verseIds)은 편집 시 seed→PATCH로 보존된다(QA ⑪, 04 §4.3.6).
        IconButton(
          tooltip: l.commonEdit,
          icon: const Icon(Icons.edit_outlined),
          onPressed: () => _goEdit(context, ref),
        ),
        IconButton(
          tooltip: l.commonDelete,
          icon: const Icon(Icons.delete_outline),
          onPressed: () => _confirmDelete(context, ref),
        ),
      ],
    );
  }

  /// N-03 편집모드로 이동 후 돌아오면 상세/목록을 새로고침.
  Future<void> _goEdit(BuildContext context, WidgetRef ref) async {
    // 'i 방식'이라 noteId만 넘긴다. N-03이 그 id로 상세를 다시 조회해 폼을 채운다.
    await Navigator.of(context).pushNamed(
      AppRouter.noteEdit,
      arguments: NoteEditArgs(noteId: noteId, category: detail.category),
    );
    // 편집에서 돌아오면 이 상세와 목록을 무효화해 최신 내용으로 다시 불러온다.
    ref.invalidate(noteDetailProvider(noteId));
    ref.invalidate(notesProvider);
  }

  /// 닉네임 나눔 공개 — 공개 확인 시트(닉네임 고지+댓글 ON/OFF) → POST /notes/{id}/share.
  Future<void> _confirmPublish(BuildContext context, WidgetRef ref) async {
    final l = AppLocalizations.of(context);
    //   저장 완료(SAVED) 노트만 공개 가능. 임시저장(DRAFT)은 서버가 422로 막으므로
    //   시트를 띄우기 전에 안내만 하고 멈춘다(04 §4.3.8: 저장 확정 전 공유 불가).
    if (detail.status != 'SAVED') {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(
          content: Text(l.notePublishNeedSave),
          duration: const Duration(seconds: 2),
        ));
      return;
    }
    // 시트 반환: 댓글 허용 여부(true/false) = 공개 확정, null = 취소.
    final commentsEnabled = await showNotePublishSheet(context);
    if (commentsEnabled == null || !context.mounted) return;

    try {
      await ref
          .read(sharingRepositoryProvider)
          .publishNote(noteId, commentsEnabled: commentsEnabled);
      //   공개 성공: 이 노트 상세(공유됨 뱃지)·노트 목록·나눔 피드를 모두 무효화해
      //   방금 만든 공유본이 즉시 반영되게 한다.
      ref.invalidate(noteDetailProvider(noteId));
      ref.invalidate(notesProvider);
      ref.invalidate(sharingPostsProvider);
      if (!context.mounted) return;
      //   성공 스낵바에 "보기"를 달아 원하는 사람만 나눔 피드로 가게 한다(강제 이동 X).
      //   "조용한 나눔" 철학에 맞춰 기본은 상세에 머물고, 즉시 확인 경로만 제공.
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text(l.notePublishSuccess),
            duration: const Duration(seconds: 2),
            action: SnackBarAction(
              label: l.notePublishView,
              onPressed: () =>
                  Navigator.of(context).pushNamed(AppRouter.sharing),
            ),
          ),
        );
    } catch (e) {
      if (!context.mounted) return;
      //   409(DUPLICATE_SHARING_POST) = 이미 공개된 노트 → 실패가 아니라 "이미 했음" 안내.
      //   현재 서버 갭으로 공개 후에도 버튼이 안 사라져 재탭이 가능하므로, 그 경우 친절한
      //   안내로 바꿔 "버그처럼 보이는" 회귀를 막는다(백엔드 visibility 갱신은 후속 PR).
      final isAlreadyShared =
          e is DioException && e.response?.statusCode == 409;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text(
                isAlreadyShared ? l.notePublishAlready : l.notePublishFailed),
            duration: const Duration(seconds: 2),
          ),
        );
    }
  }

  /// 삭제 확인창 → 삭제 → 목록 새로고침 후 뒤로. (08 §8.2: 되돌리기 어려운 동작은 확인 절차)
  Future<void> _confirmDelete(BuildContext context, WidgetRef ref) async {
    final l = AppLocalizations.of(context);
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l.noteDeleteConfirmTitle),
        content: Text(l.noteDeleteConfirmBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: Text(l.commonCancel),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: Text(l.commonDelete),
          ),
        ],
      ),
    );
    if (ok != true) return;

    try {
      await ref.read(noteRepositoryProvider).delete(noteId);
      //   삭제 성공: 목록을 무효화해 사라진 노트를 반영하고 상세 화면을 닫는다.
      ref.invalidate(notesProvider);
      // 삭제 시 묵상 달력 체크리스트도 자동 갱신(모든 월 무효화).
      ref.invalidate(meditationCalendarProvider);
      if (!context.mounted) return;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(
          content: Text(l.noteDeleted),
          duration: const Duration(seconds: 2),
        ));
    } catch (e) {
      //   실패 시 화면 유지 + 안내(되돌리기 어려운 동작이라 실패를 명확히 알림).
      if (!context.mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(
          content: Text(l.noteDeleteFailed),
          duration: const Duration(seconds: 2),
        ));
    }
  }
}

/// 상세 본문 — 카테고리에 따라 body 또는 4섹션을 표시.
class _DetailBody extends StatelessWidget {
  final NoteDetail detail;

  const _DetailBody({required this.detail});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // 제목
        Text(
          detail.title.isEmpty ? l.noteUntitled : detail.title,
          style: theme.textTheme.titleLarge,
        ),
        const SizedBox(height: 8),
        // 메타: 카테고리 / 임시저장 / 공유 뱃지
        Row(
          children: [
            Text(
              noteCategoryLabel(detail.category),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.primary),
            ),
            if (detail.status == 'DRAFT') ...[
              const SizedBox(width: 8),
              Text(l.noteDraft,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: Colors.orange)),
            ],
            if (detail.shared) ...[
              const SizedBox(width: 8),
              //   공개된 노트는 "공유됨 ›"을 탭하면 나눔 피드로 이동(지속 경로).
              //   스낵바("보기")가 사라진 뒤에도 언제든 공유본을 찾아갈 수 있다.
              InkWell(
                onTap: () => Navigator.of(context).pushNamed(AppRouter.sharing),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(l.noteShared,
                        style: theme.textTheme.bodySmall
                            ?.copyWith(color: theme.colorScheme.secondary)),
                    Icon(Icons.chevron_right,
                        size: 14, color: theme.colorScheme.secondary),
                  ],
                ),
              ),
            ],
          ],
        ),
        const Divider(height: 24),

        //   전 카테고리 단일 body(QT 포함). 마커는 편집기와 동일 파서로 렌더.
        (detail.body?.isNotEmpty ?? false)
            ? _RichNoteText(text: detail.body!, style: theme.textTheme.bodyLarge)
            : Text(l.noteNoContent, style: theme.textTheme.bodyLarge),

        // 인용 절(있을 때만 표시) — V1은 보기 전용
        if (detail.verses.isNotEmpty) ...[
          const Divider(height: 24),
          Text(l.noteQuotedVerses, style: theme.textTheme.titleSmall),
          const SizedBox(height: 4),
          for (final v in detail.verses)
            Text('· ${v.bookCode} ${v.chapterNo ?? ''}:${v.verseNo ?? ''}',
                style: theme.textTheme.bodyMedium),
        ],
      ],
    );
  }
}

/// 저장된 노트 본문을 편집기와 동일한 마크업(굵게 `**`, 하이라이트 `==`,
/// 글자색·배경·크기 `[fg|bg|fs=..]`)으로 렌더한다.
///
/// 편집기는 [QtNoteRichTextParser]로 라이브 렌더하지만 상세 보기는 평문 [Text]라
/// `**굵게**` 마커가 그대로 노출되던 버그를 수정한다(편집기와 동일 파서 사용).
class _RichNoteText extends StatelessWidget {
  final String text;
  final TextStyle? style;

  /// 이모지 클러스터 크기 — 편집기 `_defaultEmojiFontSize`와 동일.
  static const double _emojiFontSize = 16;

  const _RichNoteText({required this.text, required this.style});

  @override
  Widget build(BuildContext context) {
    return Text.rich(
      QtNoteRichTextParser.parse(text, style, emojiFontSize: _emojiFontSize),
    );
  }
}
