import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/core/theme/app_theme.dart';
import 'package:qtai_app/core/widgets/calm_paper.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
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
          // 통계는 별도 카드 대신 프로필 부제로 접는다(프로토타입과 동일).
          final subtitle = stats != null
              ? '${l.statsStreak} ${l.statsDays(stats.meditationStreakDays)} · '
                  '${l.statsWeek} ${l.statsDays(stats.week.meditationDays)} · '
                  '${l.statsMonth} ${l.statsDays(stats.month.meditationDays)}'
              : l.mypageViewProfile;

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
                    subtitle: subtitle,
                    onTap: () =>
                        Navigator.of(context).pushNamed(AppRouter.profileEdit),
                  ),
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
                  CpRow(
                    leading: Icons.music_note_outlined,
                    title: l.qmMyPraise,
                    meta: l.qmSongCount(
                        dashboard.praiseSummary?.savedSongCount ?? 0),
                    chevron: true,
                    onTap: () =>
                        Navigator.of(context).pushNamed(AppRouter.praise),
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
