import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../providers/mypage_providers.dart';
import '../widgets/profile_card.dart';
import '../widgets/quick_menu_card.dart';
import '../widgets/stats_card.dart';

/// 마이페이지 대시보드 화면.
///
/// Pull-to-refresh로 데이터를 새로고침할 수 있다.
/// 위젯별 부분 실패 시 SnackBar로 알린다.
class MyPageScreen extends ConsumerWidget {
  const MyPageScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dashboardAsync = ref.watch(dashboardProvider);
    final l = AppLocalizations.of(context);

    // 위젯 에러 SnackBar 표시
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
      appBar: AppBar(
        title: Text(l.mypageTitle),
        centerTitle: true,
      ),
      body: dashboardAsync.whenOrDefault(
        data: (dashboard) {
          return RefreshIndicator(
            onRefresh: () async {
              ref.invalidate(dashboardProvider);
              // 새 데이터가 로드될 때까지 대기
              await ref.read(dashboardProvider.future);
            },
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // 프로필 카드
                if (dashboard.profile != null)
                  ProfileCard(
                    profile: dashboard.profile!,
                    onTap: () {
                      Navigator.of(context)
                          .pushNamed(AppRouter.profileEdit);
                    },
                  ),

                const SizedBox(height: 12),

                // 통계 카드
                if (dashboard.stats != null)
                  StatsCard(stats: dashboard.stats!),

                const SizedBox(height: 12),

                // 퀵 메뉴 (알림·찬양·설정)
                QuickMenuCard(
                  unreadNotificationCount:
                      dashboard.unreadNotificationCount,
                  savedSongCount:
                      dashboard.praiseSummary?.savedSongCount ?? 0,
                  onNotificationTap: () {
                    Navigator.of(context)
                        .pushNamed(AppRouter.notifications);
                  },
                  onPraiseTap: () {
                    Navigator.of(context)
                        .pushNamed(AppRouter.praise);
                  },
                  onSettingsTap: () {
                    Navigator.of(context)
                        .pushNamed(AppRouter.appSettings);
                  },
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
