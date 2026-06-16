import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/core/theme/app_theme.dart';
import 'package:qtai_app/core/widgets/calm_paper.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../models/dashboard_response.dart';
import '../providers/mypage_providers.dart';

/// 마이페이지 대시보드 화면 (Calm Paper — DESIGN_PROTOTYPE.md s-my).
///
/// 프로필은 흰 카드가 아니라 행으로, 묵상 통계는 프로필 부제로 접고,
/// 메뉴는 회색 그룹 박스로 묶는다. Pull-to-refresh로 새로고침한다.
class MyPageScreen extends ConsumerWidget {
  const MyPageScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dashboardAsync = ref.watch(dashboardProvider);
    final l = AppLocalizations.of(context);

    // 위젯 부분 실패 시 SnackBar 표시.
    ref.listen(dashboardProvider, (prev, next) {
      next.whenData((dashboard) {
        if (dashboard.widgetErrors.isNotEmpty) {
          final errorNames = dashboard.widgetErrors.join(', ');
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('${l.mypagePartialError}: $errorNames'),
              duration: const Duration(seconds: 3),
            ),
          );
        }
      });
    });

    return Scaffold(
      appBar: AppBar(title: Text(l.mypageTitle), centerTitle: true),
      body: dashboardAsync.whenOrDefault(
        data: (dashboard) {
          final stats = dashboard.stats;

          return RefreshIndicator(
            onRefresh: () async {
              ref.invalidate(dashboardProvider);
              await ref.read(dashboardProvider.future);
            },
            child: ListView(
              padding: const EdgeInsets.fromLTRB(20, 10, 20, 24),
              children: [
                if (dashboard.profile != null)
                  _ProfileRow(
                    nickname: dashboard.profile!.nickname,
                    subtitle: l.mypageViewProfile,
                    onTap: () =>
                        Navigator.of(context).pushNamed(AppRouter.profileEdit),
                  ),
                // 미션 블록 — 연속/이번주/이번달 묵상 목표를 별도 블록으로 표시.
                if (stats != null) ...[
                  CpSectionTitle(l.missionTitle),
                  _MissionBlock(stats: stats),
                  const SizedBox(height: 8),
                ] else
                  const SizedBox(height: 18),
                CpGroup(children: [
                  CpRow(
                    leading: Icons.notifications_outlined,
                    title: l.qmNotifications,
                    trailing: dashboard.unreadNotificationCount > 0
                        ? CpBadge(
                            dashboard.unreadNotificationCount > 99
                                ? '99+'
                                : '${dashboard.unreadNotificationCount}',
                            dot: true)
                        : null,
                    chevron: true,
                    onTap: () => Navigator.of(context)
                        .pushNamed(AppRouter.notifications),
                  ),
                ]),
                CpGroup(children: [
                  CpRow(
                    leading: Icons.settings_outlined,
                    title: l.qmSettings,
                    chevron: true,
                    onTap: () =>
                        Navigator.of(context).pushNamed(AppRouter.appSettings),
                  ),
                ]),
              ],
            ),
          );
        },
      ),
    );
  }
}

/// 프로필 행 — 회색 아바타 + 닉네임 + 통계 부제 + 쉐브론.
class _ProfileRow extends StatelessWidget {
  final String nickname;
  final String subtitle;
  final VoidCallback? onTap;
  const _ProfileRow({
    required this.nickname,
    required this.subtitle,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Row(
          children: [
            CircleAvatar(
              radius: 28,
              backgroundColor: c.bgSunken,
              child: Icon(Icons.person, size: 28, color: c.text2),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    nickname,
                    style: TextStyle(
                        fontFamily: 'GowunDodum',
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                        color: c.text),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: TextStyle(
                        fontFamily: 'GowunDodum', fontSize: 13, color: c.text2),
                  ),
                ],
              ),
            ),
            Icon(Icons.chevron_right, size: 20, color: c.textMuted),
          ],
        ),
      ),
    );
  }
}

/// 미션 블록 — 연속/이번 주/이번 달 묵상 일수를 한 박스에 모아 보여 준다.
///
/// 기존엔 프로필 닉네임 부제로 접혀 있던 통계를 별도 블록으로 분리했다.
class _MissionBlock extends StatelessWidget {
  final StatsWidget stats;
  const _MissionBlock({required this.stats});

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final c = context.appColors;
    return CpSubBox(
      child: Row(
        children: [
          Expanded(
            child: _MissionMetric(
              icon: Icons.local_fire_department,
              value: l.statsDays(stats.meditationStreakDays),
              label: l.statsStreak,
            ),
          ),
          Container(width: 1, height: 38, color: c.hairline),
          Expanded(
            child: _MissionMetric(
              icon: Icons.calendar_today,
              value: l.statsDays(stats.week.meditationDays),
              label: l.statsWeek,
            ),
          ),
          Container(width: 1, height: 38, color: c.hairline),
          Expanded(
            child: _MissionMetric(
              icon: Icons.calendar_month,
              value: l.statsDays(stats.month.meditationDays),
              label: l.statsMonth,
            ),
          ),
        ],
      ),
    );
  }
}

/// 미션 블록의 단일 지표(아이콘 + 값 + 라벨).
class _MissionMetric extends StatelessWidget {
  final IconData icon;
  final String value;
  final String label;
  const _MissionMetric({
    required this.icon,
    required this.value,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    return Column(
      children: [
        Icon(icon, size: 22, color: c.text2),
        const SizedBox(height: 6),
        Text(
          value,
          style: TextStyle(
              fontFamily: 'GowunDodum',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: c.text),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: TextStyle(
              fontFamily: 'GowunDodum', fontSize: 12, color: c.textMuted),
        ),
      ],
    );
  }
}
