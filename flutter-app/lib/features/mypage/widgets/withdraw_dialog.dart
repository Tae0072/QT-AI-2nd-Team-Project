import 'package:flutter/material.dart';

/// 회원 탈퇴 확인 다이얼로그.
///
/// '정말 탈퇴하시겠습니까?' 확인 후 true를 반환한다.
/// 취소 시 false를 반환한다.
Future<bool?> showWithdrawDialog(BuildContext context) {
  return showDialog<bool>(
    context: context,
    builder: (context) {
      final theme = Theme.of(context);

      return AlertDialog(
        title: const Text('회원 탈퇴'),
        content: const Text(
          '탈퇴하시면 모든 데이터가 삭제되며 복구할 수 없습니다.\n'
          '정말 탈퇴하시겠습니까?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(
              '탈퇴',
              style: TextStyle(color: theme.colorScheme.error),
            ),
          ),
        ],
      );
    },
  );
}
