import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';

/// 회원 탈퇴 확인 다이얼로그.
///
/// 개인정보 2년 보관·자동 삭제 정책(2026-06-05 Lead 결정)을 고지하고
/// 확인 시 true, 취소 시 false를 반환한다.
Future<bool?> showWithdrawDialog(BuildContext context) {
  final l = AppLocalizations.of(context);
  return showDialog<bool>(
    context: context,
    builder: (context) {
      final theme = Theme.of(context);

      return AlertDialog(
        title: Text(l.withdrawTitle),
        content: Text(l.withdrawBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(l.commonCancel),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(
              l.withdrawConfirm,
              style: TextStyle(color: theme.colorScheme.error),
            ),
          ),
        ],
      );
    },
  );
}
