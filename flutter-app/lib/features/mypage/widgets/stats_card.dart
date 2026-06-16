import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../models/dashboard_response.dart';

/// 통계 위젯 카드.
///
/// 주간/월간 QT 묵상 일수, 노트 수, 연속 묵상 일수를 표시한다.
/// 값은 서버(service-note 묵상 달력)가 집계한다 — 노트를 저장(SAVED)한 날이
/// 묵상 1일로 즉시 반영되며, 임시저장(DRAFT)은 포함되지 않는다.
class StatsCard extends StatelessWidget {
  final StatsWidget stats;

  const StatsCard({super.key, required this.stats});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l.statsTitle,
              style: theme.textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _StatItem(
                    label: l.statsWeek,
                    value: l.statsDays(stats.week.meditationDays),
                    icon: Icons.calendar_today,
                  ),
                ),
                Expanded(
                  child: _StatItem(
                    label: l.statsMonth,
                    value: l.statsDays(stats.month.meditationDays),
                    icon: Icons.calendar_month,
                  ),
                ),
                Expanded(
                  child: _StatItem(
                    label: l.statsStreak,
                    value: l.statsDays(stats.meditationStreakDays),
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
