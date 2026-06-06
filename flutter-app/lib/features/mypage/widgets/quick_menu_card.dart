import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';

/// 알림·찬양 요약 + 설정 메뉴 카드.
///
/// 대시보드 하단에 배치되며, 알림 카운트와 찬양 저장 곡 수를 표시한다.
/// 각 항목 탭 시 해당 화면으로 이동할 수 있다 (현재는 placeholder).
class QuickMenuCard extends StatelessWidget {
  final int unreadNotificationCount;
  final int savedSongCount;
  final VoidCallback? onNotificationTap;
  final VoidCallback? onPraiseTap;
  final VoidCallback? onSettingsTap;

  const QuickMenuCard({
    super.key,
    this.unreadNotificationCount = 0,
    this.savedSongCount = 0,
    this.onNotificationTap,
    this.onPraiseTap,
    this.onSettingsTap,
  });

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    return Card(
      child: Column(
        children: [
          _MenuItem(
            icon: Icons.notifications_outlined,
            label: l.qmNotifications,
            trailing: unreadNotificationCount > 0
                ? _Badge(count: unreadNotificationCount)
                : null,
            onTap: onNotificationTap,
          ),
          const Divider(height: 1, indent: 56),
          _MenuItem(
            icon: Icons.music_note_outlined,
            label: l.qmMyPraise,
            trailing: Text(
              l.qmSongCount(savedSongCount),
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(context).colorScheme.outline,
                  ),
            ),
            onTap: onPraiseTap,
          ),
          const Divider(height: 1, indent: 56),
          _MenuItem(
            icon: Icons.settings_outlined,
            label: l.qmSettings,
            onTap: onSettingsTap,
          ),
        ],
      ),
    );
  }
}

class _MenuItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final Widget? trailing;
  final VoidCallback? onTap;

  const _MenuItem({
    required this.icon,
    required this.label,
    this.trailing,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon),
      title: Text(label),
      trailing: trailing != null
          ? Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                trailing!,
                const SizedBox(width: 8),
                const Icon(Icons.chevron_right, size: 20),
              ],
            )
          : const Icon(Icons.chevron_right, size: 20),
      onTap: onTap,
    );
  }
}

class _Badge extends StatelessWidget {
  final int count;

  const _Badge({required this.count});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: theme.colorScheme.error,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        count > 99 ? '99+' : '$count',
        style: theme.textTheme.labelSmall?.copyWith(
          color: theme.colorScheme.onError,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
