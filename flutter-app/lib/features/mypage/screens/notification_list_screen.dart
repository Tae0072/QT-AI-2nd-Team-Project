import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../providers/mypage_providers.dart';

/// 알림 목록 화면 (M-02).
///
/// - 미읽음 필터 토글
/// - 개별 읽음 처리 (탭)
/// - 전체 읽음 처리 (AppBar 액션)
class NotificationListScreen extends ConsumerWidget {
  const NotificationListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notificationsAsync = ref.watch(notificationsProvider);
    final unreadOnly = ref.watch(unreadOnlyFilterProvider);
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('알림'),
        centerTitle: true,
        actions: [
          TextButton(
            onPressed: () async {
              final repository = ref.read(myPageRepositoryProvider);
              await repository.markAllNotificationsRead();
              ref.invalidate(notificationsProvider);
              ref.invalidate(dashboardProvider);
            },
            child: const Text('모두 읽음'),
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
                  label: const Text('미읽음만'),
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
                  return const Center(
                    child: Text('알림이 없습니다', style: TextStyle(color: Colors.grey)),
                  );
                }

                return RefreshIndicator(
                  onRefresh: () async => ref.invalidate(notificationsProvider),
                  child: ListView.separated(
                    itemCount: response.items.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final item = response.items[index];
                      return ListTile(
                        leading: Icon(
                          item.read ? Icons.notifications_outlined : Icons.notifications_active,
                          color: item.read ? Colors.grey : theme.colorScheme.primary,
                        ),
                        title: Text(
                          item.message,
                          style: TextStyle(
                            fontWeight: item.read ? FontWeight.normal : FontWeight.bold,
                          ),
                        ),
                        subtitle: Text(
                          _formatDate(item.createdAt),
                          style: theme.textTheme.bodySmall,
                        ),
                        onTap: () async {
                          if (!item.read) {
                            final repository = ref.read(myPageRepositoryProvider);
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

  String _formatDate(DateTime dateTime) {
    final now = DateTime.now();
    final diff = now.difference(dateTime);
    if (diff.inMinutes < 1) return '방금 전';
    if (diff.inHours < 1) return '${diff.inMinutes}분 전';
    if (diff.inDays < 1) return '${diff.inHours}시간 전';
    if (diff.inDays < 7) return '${diff.inDays}일 전';
    return '${dateTime.month}/${dateTime.day}';
  }
}
