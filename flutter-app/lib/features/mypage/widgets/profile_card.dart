import 'package:flutter/material.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/core/theme/app_theme.dart';
import '../models/dashboard_response.dart';

/// 대시보드 상단 프로필 카드.
///
/// 프로필 이미지(CircleAvatar)와 닉네임을 표시하며,
/// 탭하면 프로필 상세 화면으로 이동한다.
class ProfileCard extends StatelessWidget {
  final ProfileSummary profile;
  final String? profileImageUrl;
  final VoidCallback? onTap;

  const ProfileCard({
    super.key,
    required this.profile,
    this.profileImageUrl,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              CircleAvatar(
                radius: 28,
                backgroundColor: context.appColors.bgSunken,
                backgroundImage: profileImageUrl != null
                    ? NetworkImage(profileImageUrl!)
                    : null,
                child: profileImageUrl == null
                    ? Icon(Icons.person,
                        size: 28, color: context.appColors.text2)
                    : null,
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      profile.nickname,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      l.mypageViewProfile,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.outline,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right,
                color: theme.colorScheme.outline,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
