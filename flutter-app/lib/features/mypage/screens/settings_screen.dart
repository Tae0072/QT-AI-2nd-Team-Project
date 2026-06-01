import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../providers/mypage_providers.dart';

/// 설정 화면 (M-06).
///
/// - 알림 수신: Switch
/// - 폰트 크기: DropdownButton (SMALL/MEDIUM/LARGE)
class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settingsAsync = ref.watch(settingsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('설정'),
        centerTitle: true,
      ),
      body: settingsAsync.whenOrDefault(
        data: (settings) {
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              // 알림 수신
              SwitchListTile(
                title: const Text('알림 수신'),
                subtitle: const Text('푸시 알림을 받습니다'),
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
                title: const Text('폰트 크기'),
                subtitle: const Text('본문 글자 크기를 설정합니다'),
                trailing: DropdownButton<String>(
                  value: settings.fontSize,
                  underline: const SizedBox.shrink(),
                  items: const [
                    DropdownMenuItem(value: 'SMALL', child: Text('작게')),
                    DropdownMenuItem(value: 'MEDIUM', child: Text('보통')),
                    DropdownMenuItem(value: 'LARGE', child: Text('크게')),
                  ],
                  onChanged: (value) async {
                    if (value == null) return;
                    final repository = ref.read(myPageRepositoryProvider);
                    await repository.updateSettings(fontSize: value);
                    ref.invalidate(settingsProvider);
                  },
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
