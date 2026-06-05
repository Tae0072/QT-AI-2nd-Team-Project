import 'package:flutter/material.dart';

/// 회원 탈퇴 확인 다이얼로그.
///
/// 개인정보 2년 보관·자동 삭제 정책(2026-06-05 Lead 결정)을 고지하고
/// 확인 시 true, 취소 시 false를 반환한다.
Future<bool?> showWithdrawDialog(BuildContext context) {
  return showDialog<bool>(
    context: context,
    builder: (context) {
      final theme = Theme.of(context);

      return AlertDialog(
        title: const Text('회원 탈퇴'),
        content: const Text(
          '탈퇴 시 계정은 비활성화되며, 개인정보와 작성 기록은 '
          '관련 법령에 따라 2년간 보관 후 자동 삭제됩니다.\n\n'
          '보관 기간 내 같은 카카오 계정으로 다시 로그인하면 '
          '계정과 기록이 복구됩니다.\n\n'
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
