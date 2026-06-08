import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';

/// 노트를 닉네임 나눔에 공개하기 전 확인 바텀시트. (N-04에서 호출, F-10)
///
/// 외부 OS 공유(note_share_sheet)와 다른, **앱 안 닉네임 나눔 공개**용이다.
/// - 닉네임이 함께 공개된다는 안내를 보여준다(07 F-10 §공유 정책 필수).
/// - 댓글 허용 ON/OFF를 고를 수 있다(commentsEnabled).
///
/// [공개]를 누르면 선택한 commentsEnabled(bool)를 반환하고,
/// 취소하거나 시트를 닫으면 null을 반환한다(= 공개하지 않음).
Future<bool?> showNotePublishSheet(BuildContext context) {
  return showModalBottomSheet<bool>(
    context: context,
    showDragHandle: true,
    builder: (_) => const _NotePublishSheet(),
  );
}

class _NotePublishSheet extends StatefulWidget {
  const _NotePublishSheet();

  @override
  State<_NotePublishSheet> createState() => _NotePublishSheetState();
}

class _NotePublishSheetState extends State<_NotePublishSheet> {
  // 댓글 허용 기본값은 ON(04 §4.3.8 commentsEnabled 기본 true와 맞춤).
  bool _commentsEnabled = true;

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final theme = Theme.of(context);

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l.notePublishSheetTitle, style: theme.textTheme.titleMedium),
            const SizedBox(height: 12),
            // 닉네임 공개 안내 — 공유 직전 닉네임 노출 고지(F-10 필수).
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.info_outline,
                    size: 18, color: theme.colorScheme.primary),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(l.notePublishNicknameNotice,
                      style: theme.textTheme.bodyMedium),
                ),
              ],
            ),
            const SizedBox(height: 4),
            // 댓글 허용 토글 — 게시글별 댓글 ON/OFF(F-10 §댓글 정책).
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(l.notePublishCommentsLabel),
              value: _commentsEnabled,
              onChanged: (v) => setState(() => _commentsEnabled = v),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    // 취소: null 반환(= 공개 안 함).
                    onPressed: () => Navigator.of(context).pop(),
                    child: Text(l.commonCancel),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    // 공개: 선택한 댓글 허용 여부를 반환.
                    onPressed: () =>
                        Navigator.of(context).pop(_commentsEnabled),
                    child: Text(l.notePublishConfirm),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
