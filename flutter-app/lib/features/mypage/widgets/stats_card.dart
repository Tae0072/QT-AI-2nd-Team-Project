import 'package:flutter/material.dart';

import '../models/dashboard_response.dart';

/// 통계 위젯 카드.
///
/// 주간/월간 QT 묵상 일수, 노트 수, 연속 묵상 일수를 표시한다.
/// notes 도메인이 아직 미구현이므로 현재는 0으로 표시된다.
class StatsCard extends StatelessWidget {
  final StatsWidget stats;

  const StatsCard({super.key, required this.stats});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '나의 묵상',
              style: theme.textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _StatItem(
                    label: '이번 주',
                    value: '${stats.week.meditationDays}일',
                    icon: Icons.calendar_today,
                  ),
                ),
                Expanded(
                  child: _StatItem(
                    label: '이번 달',
                    value: '${stats.month.meditationDays}일',
                    icon: Icons.calendar_month,
                  ),
                ),
                Expanded(
                  child: _StatItem(
                    label: '연속',
                    value: '${stats.meditationStreakDays}일',
                    icon: Icons.local_fire_department,
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

class _StatItem extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;

  const _StatItem({
    required this.label,
    required this.value,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      children: [
        Icon(icon, size: 24, color: theme.colorScheme.primary),
        const SizedBox(height: 4),
        Text(
          value,
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: theme.textTheme.bodySmall?.copyWith(
            color: theme.colorScheme.outline,
          ),
        ),
      ],
    );
  }
}
