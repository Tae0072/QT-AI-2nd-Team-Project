import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/music_providers.dart';

/// 화면에 보이지 않는 배경음악 호스트.
///
/// HomeScreen(하단바 루트)에 마운트되어, 메인 진입 직후 배경음악을 초기화/자동재생한다.
/// HomeScreen이 살아있는 동안(모든 탭·푸시된 하위 화면 포함) 재생이 유지된다.
class BackgroundMusicHost extends ConsumerStatefulWidget {
  const BackgroundMusicHost({super.key});

  @override
  ConsumerState<BackgroundMusicHost> createState() =>
      _BackgroundMusicHostState();
}

class _BackgroundMusicHostState extends ConsumerState<BackgroundMusicHost> {
  @override
  void initState() {
    super.initState();
    // 첫 프레임 후 초기화 — 설정(기본 ON)에 따라 자동 재생.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        ref.read(musicControllerProvider.notifier).ensureInitialized();
      }
    });
  }

  @override
  Widget build(BuildContext context) => const SizedBox.shrink();
}
