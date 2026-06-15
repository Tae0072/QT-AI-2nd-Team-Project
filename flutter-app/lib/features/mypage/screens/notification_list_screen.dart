import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../models/notification_response.dart';
import '../providers/mypage_providers.dart';

/// 알림 목록 화면 (M-02).
///
/// - 미읽음 필터 토글
/// - 개별 읽음 처리 (탭)
/// - 전체 읽음 처리 (AppBar 액션)
class NotificationListScreen extends ConsumerStatefulWidget {
  const NotificationListScreen({
    super.key,
    DateTime Function()? now,
  }) : _now = now ?? DateTime.now;

  final DateTime Function() _now;

  @override
  ConsumerState<NotificationListScreen> createState() =>
      _NotificationListScreenState();
}

class _NotificationListScreenState
    extends ConsumerState<NotificationListScreen> {
  Timer? _relativeTimeRefreshTimer;

  @override
  void initState() {
    super.initState();
    _relativeTimeRefreshTimer =
        Timer.periodic(const Duration(seconds: 30), (_) {
      if (mounted) {
        setState(() {});
      }
    });
  }

  @override
  void dispose() {
    _relativeTimeRefreshTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final notificationsAsync = ref.watch(notificationsProvider);
    final unreadOnly = ref.watch(unreadOnlyFilterProvider);
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.qmNotifications),
        centerTitle: true,
        actions: [
          TextButton(
            onPressed: () async {
              final repository = ref.read(myPageRepositoryProvider);
              await repository.markAllNotificationsRead();
              ref.invalidate(notificationsProvider);
              ref.invalidate(dashboardProvider);
            },
            child: Text(l.notifMarkAllRead),
          ),
        ],
      ),
      body: Column(
        children: [
          // 미읽음 필터
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                FilterChip(
                  label: Text(l.notifUnreadOnly),
                  selected: unreadOnly,
                  onSelected: (value) {
                    ref.read(unreadOnlyFilterProvider.notifier).state = value;
                  },
                ),
              ],
            ),
          ),

          // 알림 목록
          Expanded(
            child: notificationsAsync.whenOrDefault(
              data: (response) {
                if (response.items.isEmpty) {
                  return Center(
                    child: Text(l.notifEmpty,
                        style: const TextStyle(color: Colors.grey)),
                  );
                }

                return RefreshIndicator(
                  onRefresh: () async => ref.invalidate(notificationsProvider),
                  child: ListView.separated(
                    itemCount: response.items.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final item = response.items[index];
                      final title = _notificationTitle(item);
                      final createdAtText = _formatDate(l, item.createdAt);
                      return ListTile(
                        leading: Icon(
                          item.read
                              ? Icons.notifications_outlined
                              : Icons.notifications_active,
                          color: item.read
                              ? Colors.grey
                              : theme.colorScheme.primary,
                        ),
                        title: Text(
                          title,
                          style: TextStyle(
                            fontWeight:
                                item.read ? FontWeight.normal : FontWeight.bold,
                          ),
                        ),
                        subtitle: _NotificationSubtitle(
                          body: item.body,
                          createdAtText: createdAtText,
                        ),
                        onTap: () async {
                          await _showNotificationDetail(
                            context,
                            item,
                            title,
                            createdAtText,
                          );
                          if (!mounted) return;
                          if (!item.read) {
                            final repository =
                                ref.read(myPageRepositoryProvider);
                            await repository.markNotificationRead(item.id);
                            ref.invalidate(notificationsProvider);
                            ref.invalidate(dashboardProvider);
                          }
                        },
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  String _formatDate(AppLocalizations l, DateTime dateTime) {
    final now = widget._now();
    final diff = now.difference(dateTime);
    if (diff.inMinutes < -1) {
      return _absoluteDateTimeLabel(dateTime);
    }

    final clockText = _clockText(dateTime);
    if (diff.inMinutes < 1) {
      return '${l.timeJustNow} · $clockText';
    }
    if (diff.inHours < 1) {
      return '${l.timeMinutesAgo(diff.inMinutes)} · $clockText';
    }
    if (diff.inDays < 1) {
      return '${l.timeHoursAgo(diff.inHours)} · $clockText';
    }
    if (diff.inDays < 7) {
      return '${l.timeDaysAgo(diff.inDays)} · $clockText';
    }
    return _absoluteDateTimeLabel(dateTime);
  }

  String _notificationTitle(NotificationItem item) {
    final title = item.title.trim();
    if (title.isNotEmpty) return title;
    final message = item.message.trim();
    if (message.isNotEmpty) return message;
    return item.type == 'NOTICE' ? '시스템 공지' : '알림';
  }

  Future<void> _showNotificationDetail(
    BuildContext context,
    NotificationItem item,
    String title,
    String createdAtText,
  ) {
    final body = item.body.trim();
    return showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(body.isEmpty ? '상세 내용이 없습니다.' : body),
            const SizedBox(height: 12),
            Text(
              createdAtText,
              style: Theme.of(context)
                  .textTheme
                  .bodySmall
                  ?.copyWith(color: Colors.grey),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  String _clockText(DateTime dateTime) {
    final hour = dateTime.hour.toString().padLeft(2, '0');
    final minute = dateTime.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  String _absoluteDateTimeLabel(DateTime dateTime) {
    return '${dateTime.month}/${dateTime.day} ${_clockText(dateTime)}';
  }
}

class _NotificationSubtitle extends StatelessWidget {
  const _NotificationSubtitle({
    required this.body,
    required this.createdAtText,
  });

  final String body;
  final String createdAtText;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final lines = <Widget>[];

    if (body.isNotEmpty) {
      lines.add(
        Text(
          body,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.bodySmall,
        ),
      );
      lines.add(const SizedBox(height: 2));
    }

    lines.add(
      Text(
        createdAtText,
        style: theme.textTheme.bodySmall?.copyWith(color: Colors.grey),
      ),
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: lines,
    );
  }
}
