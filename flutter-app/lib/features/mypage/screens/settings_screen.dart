import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../../tts/providers/tts_providers.dart';
import '../providers/mypage_providers.dart';

/// 설정 화면 (M-06).
///
/// - 알림 수신: Switch
/// - 폰트 크기: DropdownButton (SMALL/MEDIUM/LARGE)
/// - TTS 목소리: BottomSheet 선택 (로컬 저장, 앱 전체 적용)
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

              const Divider(),

              // TTS 목소리
              ListTile(
                title: const Text('QT 읽기 목소리'),
                subtitle: const Text('QT 본문을 읽어주는 목소리를 설정합니다'),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      ref.watch(selectedVoiceProvider),
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.primary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const Icon(Icons.chevron_right),
                  ],
                ),
                onTap: () => _showVoiceSelector(context, ref),
              ),
            ],
          );
        },
      ),
    );
  }

  /// 목소리 선택 BottomSheet.
  ///
  /// TTS 서버의 `/voices` 목록을 보여주고, 선택값은
  /// [SelectedVoiceNotifier]를 통해 SharedPreferences에 저장한다.
  void _showVoiceSelector(BuildContext context, WidgetRef ref) {
    final voicesAsync = ref.read(ttsVoicesProvider);
    final current = ref.read(selectedVoiceProvider);

    voicesAsync.when(
      data: (voices) {
        if (voices.isEmpty) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('TTS 서버에 연결할 수 없습니다')),
          );
          return;
        }
        showModalBottomSheet(
          context: context,
          showDragHandle: true,
          builder: (ctx) => SafeArea(
            child: ListView.builder(
              shrinkWrap: true,
              itemCount: voices.length,
              itemBuilder: (ctx, i) {
                final v = voices[i];
                final selected = v.name == current;
                return ListTile(
                  leading: Icon(
                    selected
                        ? Icons.radio_button_checked
                        : Icons.radio_button_off,
                    color: selected
                        ? Theme.of(ctx).colorScheme.primary
                        : null,
                  ),
                  title: Text(v.displayName),
                  subtitle: Text(v.type == 'custom' ? '커스텀 목소리' : '기본 목소리'),
                  trailing: v.hasFinetuned
                      ? const Chip(
                          label: Text('학습됨', style: TextStyle(fontSize: 10)))
                      : null,
                  onTap: () {
                    ref.read(selectedVoiceProvider.notifier).select(v.name);
                    Navigator.pop(ctx);
                  },
                );
              },
            ),
          ),
        );
      },
      loading: () {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('목소리 목록을 불러오는 중입니다')),
        );
      },
      error: (_, __) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('목소리 목록을 불러올 수 없습니다')),
        );
      },
    );
  }
}
