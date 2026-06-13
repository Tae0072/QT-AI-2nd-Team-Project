import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/theme/font_scale_provider.dart';
import '../../../core/theme/theme_providers.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../providers/mypage_providers.dart';
// [DEV_MODE] 버전 정보 타일(5탭 → 개발자 모드). 개발 종료 시 이 import와 아래 타일 제거.
import '../../dev/dev_mode_screen.dart';

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

    // 서버 설정값이 도착하면 로컬 폰트 크기 provider에 동기화한다(다른 기기에서
    // 바꾼 값 반영). set()은 같은 값이면 무시하므로 루프가 생기지 않는다.
    ref.listen(settingsProvider, (prev, next) {
      next.whenData(
        (s) => ref.read(fontSizeProvider.notifier).set(s.fontSize),
      );
    });

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

              // 화면 테마 — 라이트/다크/시스템 중 선택(기기 로컬 저장).
              ListTile(
                leading: const Icon(Icons.brightness_6_outlined),
                title: Text(l.settingsThemeMode),
                subtitle: Text(_themeModeLabel(l, ref.watch(themeModeProvider))),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => _showThemeModeSheet(context, ref, l),
              ),

              const Divider(),

              // 폰트 크기
              ListTile(
                title: Text(l.settingsFontSize),
                subtitle: Text(l.settingsFontSizeDesc),
                // 즉시 반영을 위해 렌더링 값은 로컬 provider를 따른다(서버 응답 대기 X).
                trailing: DropdownButton<String>(
                  value: ref.watch(fontSizeProvider),
                  underline: const SizedBox.shrink(),
                  items: [
                    DropdownMenuItem(value: 'SMALL', child: Text(l.settingsFontSmall)),
                    DropdownMenuItem(value: 'MEDIUM', child: Text(l.settingsFontMedium)),
                    DropdownMenuItem(value: 'LARGE', child: Text(l.settingsFontLarge)),
                  ],
                  onChanged: (value) async {
                    if (value == null) return;
                    // 1) 로컬 즉시 반영, 2) 서버에도 저장(다른 기기 동기화).
                    await ref.read(fontSizeProvider.notifier).set(value);
                    final repository = ref.read(myPageRepositoryProvider);
                    await repository.updateSettings(fontSize: value);
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
              // [DEV_MODE] 버전 정보 — 5번 연속 탭 시 비밀번호 후 개발자 모드 진입
              const DevVersionTile(),
            ],
          );
        },
      ),
    );
  }

  /// 현재 테마 모드의 표시 라벨.
  String _themeModeLabel(AppLocalizations l, ThemeMode mode) => switch (mode) {
        ThemeMode.light => l.settingsThemeLight,
        ThemeMode.dark => l.settingsThemeDark,
        ThemeMode.system => l.settingsThemeSystem,
      };

  /// 라이트/다크/시스템 중 하나를 고르는 바텀시트.
  Future<void> _showThemeModeSheet(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l,
  ) async {
    final current = ref.read(themeModeProvider);
    final selected = await showModalBottomSheet<ThemeMode>(
      context: context,
      showDragHandle: true,
      builder: (ctx) => SafeArea(
        child: RadioGroup<ThemeMode>(
          groupValue: current,
          onChanged: (value) => Navigator.of(ctx).pop(value),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              for (final mode in const [
                ThemeMode.light,
                ThemeMode.dark,
                ThemeMode.system,
              ])
                RadioListTile<ThemeMode>(
                  value: mode,
                  title: Text(_themeModeLabel(l, mode)),
                  subtitle: mode == ThemeMode.system
                      ? Text(l.settingsThemeSystemDesc)
                      : null,
                ),
            ],
          ),
        ),
      ),
    );
    if (selected == null) return;
    await ref.read(themeModeProvider.notifier).setMode(selected);
  }
}
