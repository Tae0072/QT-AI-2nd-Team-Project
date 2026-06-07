import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/music_providers.dart';

/// 음악 설정 화면.
///
/// 설정(M-06) > "음악 설정"에서 진입. 배경음악 켜기/끄기·볼륨·종류를 조절하며
/// 서버(member_settings)에 저장된다. 변경은 즉시 전역 플레이어에 반영된다.
class MusicSettingsScreen extends ConsumerStatefulWidget {
  const MusicSettingsScreen({super.key});

  @override
  ConsumerState<MusicSettingsScreen> createState() =>
      _MusicSettingsScreenState();
}

class _MusicSettingsScreenState extends ConsumerState<MusicSettingsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        ref.read(musicControllerProvider.notifier).ensureInitialized();
      }
    });
  }

  static const Map<String, String> _categoryLabels = {
    'ALL': '전체',
    'BGM': '배경음악(브금)',
    'HYMN': '찬송가',
  };

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final state = ref.watch(musicControllerProvider);
    final controller = ref.read(musicControllerProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: const Text('음악 설정'),
        centerTitle: true,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 배경음악 켜기/끄기
          SwitchListTile(
            title: const Text('배경음악'),
            subtitle: const Text('앱 전체에서 배경음악을 재생합니다.'),
            value: state.enabled,
            onChanged: (v) => controller.setEnabled(v),
          ),

          const Divider(),

          // 볼륨
          ListTile(
            title: const Text('볼륨'),
            subtitle: Slider(
              min: 0,
              max: 100,
              divisions: 20,
              label: '${state.volume}',
              value: state.volume.clamp(0, 100).toDouble(),
              onChanged: state.enabled
                  ? (v) => controller.previewVolume(v.round())
                  : null,
              onChangeEnd: state.enabled
                  ? (v) => controller.commitVolume(v.round())
                  : null,
            ),
            trailing: Text(
              '${state.volume}',
              style: TextStyle(
                color: theme.colorScheme.primary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),

          const Divider(),

          // 음악 종류
          ListTile(
            title: const Text('음악 종류'),
            subtitle: const Text('재생할 음악 종류를 선택합니다.'),
            trailing: DropdownButton<String>(
              value: state.category,
              underline: const SizedBox.shrink(),
              items: kMusicCategories
                  .map((c) => DropdownMenuItem(
                        value: c,
                        child: Text(_categoryLabels[c] ?? c),
                      ))
                  .toList(),
              onChanged: (v) {
                if (v != null) controller.setCategory(v);
              },
            ),
          ),

          if (state.initialized && !state.hasTracks) ...[
            const SizedBox(height: 12),
            Card(
              color: theme.colorScheme.surfaceContainerHighest,
              child: const Padding(
                padding: EdgeInsets.all(16),
                child: Text(
                  '재생할 음원이 아직 없습니다. 관리자가 음원을 등록하면 자동으로 재생됩니다.',
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
