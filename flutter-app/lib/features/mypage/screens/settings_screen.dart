import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../providers/mypage_providers.dart';

/// 설정 화면 (M-06).
///
/// - 알림 수신: Switch
/// - 폰트 크기: DropdownButton (SMALL/MEDIUM/LARGE)
/// - TTS 읽기 설정: 전용 화면으로 이동 (목소리/본문/해설)
class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settingsAsync = ref.watch(settingsProvider);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.qmSettings),
        centerTitle: true,
      ),
      body: settingsAsync.whenOrDefault(
        data: (settings) {
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              // 알림 수신
              SwitchListTile(
                title: Text(l.settingsNotification),
                subtitle: Text(l.settingsNotificationDesc),
                value: settings.notificationEnabled,
                onChanged: (value) async {
                  final repository = ref.read(myPageRepositoryProvider);
                  await repository.updateSettings(notificationEnabled: value);
                  ref.invalidate(settingsProvider);
                },
              ),

              const Divider(),

              // 폰트 크기
              ListTile(
                title: Text(l.settingsFontSize),
                subtitle: Text(l.settingsFontSizeDesc),
                trailing: DropdownButton<String>(
                  value: settings.fontSize,
                  underline: const SizedBox.shrink(),
                  items: [
                    DropdownMenuItem(value: 'SMALL', child: Text(l.settingsFontSmall)),
                    DropdownMenuItem(value: 'MEDIUM', child: Text(l.settingsFontMedium)),
                    DropdownMenuItem(value: 'LARGE', child: Text(l.settingsFontLarge)),
                  ],
                  onChanged: (value) async {
                    if (value == null) return;
                    final repository = ref.read(myPageRepositoryProvider);
                    await repository.updateSettings(fontSize: value);
                    ref.invalidate(settingsProvider);
                  },
                ),
              ),

              const Divider(),

              // TTS 읽기 설정 — 전용 화면 (목소리, 본문/해설 읽기 범위)
              ListTile(
                leading: const Icon(Icons.record_voice_over_outlined),
                title: Text(l.settingsTts),
                subtitle: Text(l.settingsTtsDesc),
                trailing: const Icon(Icons.chevron_right),
                onTap: () =>
                    Navigator.of(context).pushNamed(AppRouter.ttsSettings),
              ),

              const Divider(),

              // 음악 설정 — 전용 화면 (배경음악 on/off, 볼륨, 종류)
              ListTile(
                leading: const Icon(Icons.music_note_outlined),
                title: const Text('음악 설정'),
                subtitle: const Text('배경음악 켜기/끄기, 볼륨, 종류'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () =>
                    Navigator.of(context).pushNamed(AppRouter.musicSettings),
              ),
            ],
          );
        },
      ),
    );
  }
}
