import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../providers/mypage_providers.dart';

/// 찬양 화면 (M-03) — TabBar로 내 찬양 / 큐레이션 전환.
class PraiseScreen extends ConsumerWidget {
  const PraiseScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l = AppLocalizations.of(context);
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text(l.praiseTitle),
          centerTitle: true,
          bottom: TabBar(
            tabs: [
              Tab(text: l.praiseMyTab),
              Tab(text: l.praiseCurationTab),
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
    final l = AppLocalizations.of(context);

    return myPraiseAsync.whenOrDefault(
      data: (songs) {
        if (songs.isEmpty) {
          return Center(
            child: Text(l.praiseMyEmpty,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.grey)),
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
                        SnackBar(content: Text(l.praiseDeleted)),
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
    final l = AppLocalizations.of(context);

    return curationAsync.whenOrDefault(
      data: (songs) {
        if (songs.isEmpty) {
          return Center(
            child: Text(l.praiseCurationEmpty,
                style: const TextStyle(color: Colors.grey)),
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
                      await repository.saveMyPraiseSong(song.id, song.title);
                      ref.invalidate(myPraiseSongsProvider);
                      ref.invalidate(dashboardProvider);
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(l.praiseSaved)),
                        );
                      }
                    } catch (e) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(l.praiseAlreadySaved)),
                        );
                      }
                    }
                  },
                  child: Text(l.commonSave),
                ),
              );
            },
          ),
        );
      },
    );
  }
}
