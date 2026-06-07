import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/music_providers.dart';

/// 음원 목록 바텀시트 — 오늘의 QT 음표 버튼을 길게 누르면 표시된다.
///
/// 카테고리(전체/브금/찬송가)를 고르고, 현재 카테고리의 곡 목록에서 곡을 탭해 재생한다.
/// 현재 재생 중인 곡은 강조 표시된다.
class MusicTrackSheet extends ConsumerWidget {
  const MusicTrackSheet({super.key});

  static const Map<String, String> _categoryLabels = {
    'ALL': '전체',
    'BGM': '브금',
    'HYMN': '찬송가',
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final state = ref.watch(musicControllerProvider);
    final controller = ref.read(musicControllerProvider.notifier);

    return SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 4, 12, 4),
            child: Row(
              children: [
                Text(
                  '배경음악',
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.w700),
                ),
                const Spacer(),
                IconButton(
                  tooltip: state.enabled ? '배경음악 끄기' : '배경음악 켜기',
                  icon: Icon(
                    state.enabled ? Icons.pause_circle : Icons.play_circle,
                    color: theme.colorScheme.primary,
                  ),
                  iconSize: 30,
                  onPressed: () => controller.setEnabled(!state.enabled),
                ),
              ],
            ),
          ),

          // 카테고리 선택 칩
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Wrap(
              spacing: 8,
              children: kMusicCategories.map((c) {
                return ChoiceChip(
                  label: Text(_categoryLabels[c] ?? c),
                  selected: state.category == c,
                  onSelected: (_) => controller.setCategory(c),
                );
              }).toList(),
            ),
          ),

          const Divider(height: 16),

          Flexible(
            child: state.playlist.isEmpty
                ? const Padding(
                    padding: EdgeInsets.all(24),
                    child: Text('재생할 음원이 없습니다.'),
                  )
                : ListView.builder(
                    shrinkWrap: true,
                    itemCount: state.playlist.length,
                    itemBuilder: (context, i) {
                      final track = state.playlist[i];
                      final isCurrent = i == state.currentIndex;
                      return ListTile(
                        leading: Icon(
                          isCurrent && state.playing
                              ? Icons.equalizer
                              : Icons.music_note,
                          color: isCurrent ? theme.colorScheme.primary : null,
                        ),
                        title: Text(
                          track.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        trailing: isCurrent
                            ? Text(
                                '재생 중',
                                style: TextStyle(
                                  color: theme.colorScheme.primary,
                                  fontWeight: FontWeight.w600,
                                ),
                              )
                            : null,
                        onTap: () => controller.playByIndex(i),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
