import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';

/// 댓글 입력 줄 — 텍스트 필드 + 전송 버튼.
///
/// 입력값/전송중 상태는 상위 화면이 소유하고, 이 위젯은 표시와 콜백만 담당한다.
class SharingCommentInput extends StatelessWidget {
  final TextEditingController controller;
  final bool sending;
  final VoidCallback onSend;

  const SharingCommentInput({
    super.key,
    required this.controller,
    required this.sending,
    required this.onSend,
  });

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);

    return Row(
      children: [
        Expanded(
          child: TextField(
            controller: controller,
            decoration: InputDecoration(
              hintText: l.sharingCommentHint,
              isDense: true,
              border: const OutlineInputBorder(),
            ),
            minLines: 1,
            maxLines: 3,
          ),
        ),
        IconButton(
          icon: sending
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.send),
          onPressed: sending ? null : onSend,
        ),
      ],
    );
  }
}
