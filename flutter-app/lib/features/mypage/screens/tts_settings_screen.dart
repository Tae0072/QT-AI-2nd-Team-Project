import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../tts/providers/tts_providers.dart';

/// TTS 읽기 설정 화면.
///
/// 설정(M-06) > "TTS 읽기 설정"에서 진입하는 전용 화면.
/// - 읽기 목소리: BottomSheet 선택 (SharedPreferences 영구 저장)
/// - 본문 읽기(한글) / 해설 읽기: Switch — 둘 다 켜면 본문 후 해설 낭독
class TtsSettingsScreen extends ConsumerWidget {
  const TtsSettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.settingsTts),
        centerTitle: true,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 읽기 목소리
          ListTile(
            title: Text(l.ttsVoice),
            subtitle: Text(l.ttsVoiceDesc),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  ref.watch(selectedVoiceProvider),
                  style: TextStyle(
                    color: theme.colorScheme.primary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const Icon(Icons.chevron_right),
              ],
            ),
            onTap: () => _showVoiceSelector(context, ref),
          ),

          const Divider(),

          // 읽기 범위 — 본문(한글) / 해설
          // 둘 다 켜면 본문을 읽은 후 이어서 해설을 읽는다.
          SwitchListTile(
            title: Text(l.ttsReadBible),
            subtitle: Text(l.ttsReadBibleDesc),
            value: ref.watch(ttsReadBibleProvider),
            onChanged: (value) => _setReadScope(
              context, ref,
              bible: value,
              explanation: ref.read(ttsReadExplanationProvider),
              target: ttsReadBibleProvider,
            ),
          ),
          SwitchListTile(
            title: Text(l.ttsReadExplanation),
            subtitle: Text(l.ttsReadExplanationDesc),
            value: ref.watch(ttsReadExplanationProvider),
            onChanged: (value) => _setReadScope(
              context, ref,
              bible: ref.read(ttsReadBibleProvider),
              explanation: value,
              target: ttsReadExplanationProvider,
            ),
          ),
        ],
      ),
    );
  }

  /// 읽기 범위 토글 — 둘 다 꺼지는 것은 막는다.
  void _setReadScope(
    BuildContext context,
    WidgetRef ref, {
    required bool bible,
    required bool explanation,
    required StateNotifierProvider<TtsReadScopeNotifier, bool> target,
  }) {
    if (!bible && !explanation) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppLocalizations.of(context).ttsAtLeastOne)),
      );
      return;
    }
    final value = target == ttsReadBibleProvider ? bible : explanation;
    ref.read(target.notifier).set(value);
  }

  /// 목소리 선택 BottomSheet.
  void _showVoiceSelector(BuildContext context, WidgetRef ref) {
    final l = AppLocalizations.of(context);
    final voicesAsync = ref.read(ttsVoicesProvider);
    final current = ref.read(selectedVoiceProvider);

    voicesAsync.when(
      data: (voices) {
        if (voices.isEmpty) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l.ttsServerError)),
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
                  subtitle: Text(v.type == 'custom' ? l.ttsCustomVoice : l.ttsDefaultVoice),
                  trailing: v.hasFinetuned
                      ? Chip(
                          label: Text(l.ttsFinetuned, style: const TextStyle(fontSize: 10)))
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
          SnackBar(content: Text(l.ttsVoicesLoading)),
        );
      },
      error: (_, __) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.ttsVoicesError)),
        );
      },
    );
  }
}
