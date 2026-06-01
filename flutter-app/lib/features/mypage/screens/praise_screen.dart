import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/common_widgets.dart';
import '../providers/mypage_providers.dart';

/// 찬양 화면 (M-03) — TabBar로 내 찬양 / 큐레이션 전환.
class PraiseScreen extends ConsumerWidget {
  const PraiseScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('찬양'),
          centerTitle: true,
          bottom: const TabBar(
            tabs: [
              Tab(text: '내 찬양'),
              Tab(text: '큐레이션'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            _MyPraiseTab(),
            _CurationTab(),
          ],
        ),
      ),
    );
  }
}

/// 내 찬양 목록 탭 — 삭제 가능.
class _MyPraiseTab extends ConsumerWidget {
  const _MyPraiseTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final myPraiseAsync = ref.watch(myPraiseSongsProvider);

    return myPraiseAsync.whenOrDefault(
      data: (songs) {
        if (songs.isEmpty) {
          return const Center(
            child: Text('저장한 찬양이 없습니다\n큐레이션에서 찬양을 저장해보세요',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey)),
          );
        }

        return RefreshIndicator(
          onRefresh: () async => ref.invalidate(myPraiseSongsProvider),
          child: ListView.separated(
            itemCount: songs.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final song = songs[index];
              return ListTile(
                leading: const Icon(Icons.music_note),
                title: Text(song.displayTitle),
                subtitle: Text(song.artist),
                trailing: IconButton(
                  icon: const Icon(Icons.delete_outline, color: Colors.red),
                  onPressed: () async {
                    final repository = ref.read(myPageRepositoryProvider);
                    await repository.deleteMyPraiseSong(song.id);
                    ref.invalidate(myPraiseSongsProvider);
                    ref.invalidate(dashboardProvider);
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('찬양이 삭제되었습니다')),
                      );
                    }
                  },
                ),
              );
            },
          ),
        );
      },
    );
  }
}

/// 큐레이션 곡 목록 탭 — 저장 가능.
class _CurationTab extends ConsumerWidget {
  const _CurationTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final curationAsync = ref.watch(curationSongsProvider);

    return curationAsync.whenOrDefault(
      data: (songs) {
        if (songs.isEmpty) {
          return const Center(
            child: Text('등록된 큐레이션 곡이 없습니다',
                style: TextStyle(color: Colors.grey)),
          );
        }

        return RefreshIndicator(
          onRefresh: () async => ref.invalidate(curationSongsProvider),
          child: ListView.separated(
            itemCount: songs.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final song = songs[index];
              return ListTile(
                leading: const Icon(Icons.library_music),
                title: Text(song.title),
                subtitle: Text(song.artist),
                trailing: FilledButton.tonal(
                  onPressed: () async {
                    try {
                      final repository = ref.read(myPageRepositoryProvider);
                      await repository.saveMyPraiseSong(song.id);
                      ref.invalidate(myPraiseSongsProvider);
                      ref.invalidate(dashboardProvider);
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('내 찬양에 저장되었습니다')),
                        );
                      }
                    } catch (e) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('이미 저장된 곡입니다')),
                        );
                      }
                    }
                  },
                  child: const Text('저장'),
                ),
              );
            },
          ),
        );
      },
    );
  }
}
