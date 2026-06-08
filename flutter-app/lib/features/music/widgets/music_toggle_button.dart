import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/music_providers.dart';
import 'music_track_sheet.dart';

/// 오늘의 QT 앱바의 배경음악 토글(음표 아이콘).
///
/// - 탭: 배경음악 켜기/끄기
/// - 길게 누르기: 음원 목록 시트 표시
/// 재생 중이면 채워진 음표, 꺼져 있으면 음표-슬래시.
class MusicToggleButton extends ConsumerWidget {
  const MusicToggleButton({super.key});

  void _showTrackSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      showDragHandle: true,
      builder: (_) => const MusicTrackSheet(),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    // 아이콘은 "켜짐 설정(enabled)" 기준 — 웹에서 첫 제스처 전 재생이 막혀도
    // 음소거처럼 보이지 않고, 앱 시작 시 기본 ON으로 표시된다.
    final enabled = ref.watch(musicControllerProvider.select((s) => s.enabled));

    // 길게 누르기로 음원 목록 시트를 열어야 한다.
    // IconButton의 tooltip을 그대로 쓰면, 모바일에서 Tooltip 기본 트리거가
    // "길게 누르기"라 우리의 onLongPress보다 먼저 길게 누르기를 가로채
    // 시트가 열리지 않는다(툴팁만 뜬다). 그래서 IconButton 자체 tooltip은 쓰지 않고,
    // manual 트리거 Tooltip으로 감싸 길게 누르기를 가로채지 않게 한다
    // (데스크톱/웹 마우스 호버 힌트는 그대로 유지).
    return Tooltip(
      message: enabled ? '배경음악 끄기 (길게: 목록)' : '배경음악 켜기 (길게: 목록)',
      triggerMode: TooltipTriggerMode.manual,
      child: GestureDetector(
        onLongPress: () => _showTrackSheet(context),
        child: IconButton(
          icon: Icon(enabled ? Icons.music_note : Icons.music_off),
          color: enabled ? theme.colorScheme.primary : null,
          iconSize: 26,
          onPressed: () =>
              ref.read(musicControllerProvider.notifier).setEnabled(!enabled),
        ),
      ),
    );
  }
}
