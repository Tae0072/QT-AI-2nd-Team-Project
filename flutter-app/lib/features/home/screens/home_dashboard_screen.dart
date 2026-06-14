import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_theme.dart';
import '../../../routes/app_router.dart';
import '../../bible/models/bible_models.dart';
import '../../bible/providers/bible_providers.dart';
import '../../bible/screens/today_qt_screen.dart';
import '../../mypage/providers/mypage_providers.dart';
import '../../note/models/note_models.dart';
import '../providers/home_providers.dart';

/// 홈(랜딩) 화면.
///
/// - 닉네임 인사
/// - 오늘의 말씀(오늘 QT 절 중 매일 랜덤 1절) + 매일 랜덤 그라데이션 배경
/// - 묵상 시작하기 → 오늘 QT 본문 화면
/// - 최근 묵상 기록(기록 탭과 연동)
class HomeDashboardScreen extends ConsumerWidget {
  const HomeDashboardScreen({super.key});

  // 오늘 날짜 기준 시드(같은 날엔 고정, 날이 바뀌면 변경).
  int get _dailySeed {
    final n = DateTime.now();
    return n.year * 10000 + n.month * 100 + n.day;
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final profile = ref.watch(profileProvider);
    final passageAsync = ref.watch(todayQtPassageProvider);

    final nickname =
        profile.maybeWhen(data: (m) => m.nickname, orElse: () => '');
    final gradient =
        _kGradients[Random(_dailySeed).nextInt(_kGradients.length)];

    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
          children: [
            // 인사
            Text(
              nickname.isEmpty ? '안녕하세요.' : '안녕하세요, $nickname님.',
              style: theme.textTheme.headlineSmall
                  ?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 4),
            Text(
              '오늘도 평온한 묵상의 시간 되세요.',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: context.appColors.text2),
            ),
            const SizedBox(height: 20),

            // 오늘의 말씀 카드
            _TodayVerseCard(passageAsync: passageAsync, gradient: gradient, dailySeed: _dailySeed),
            const SizedBox(height: 20),

            // 묵상 시작하기
            Center(
              child: FilledButton.icon(
                onPressed: () => Navigator.of(context).push(
                  MaterialPageRoute(builder: (_) => const TodayQtScreen()),
                ),
                icon: const Icon(Icons.self_improvement_outlined, size: 20),
                label: const Text('묵상 시작하기'),
                style: FilledButton.styleFrom(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 28, vertical: 14),
                  shape: const StadiumBorder(),
                ),
              ),
            ),
            const SizedBox(height: 28),

            // 최근 묵상 기록(기록 탭 연동)
            Row(
              children: [
                Text(
                  '최근 묵상 기록',
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.w700),
                ),
                const Spacer(),
                TextButton(
                  onPressed: () =>
                      ref.read(homeTabIndexProvider.notifier).state = 3,
                  child: const Text('모두 보기'),
                ),
              ],
            ),
            const SizedBox(height: 4),
            const _RecentNotesList(),
          ],
        ),
      ),
    );
  }
}

class _TodayVerseCard extends StatelessWidget {
  final AsyncValue<TodayQtPassage> passageAsync;
  final List<Color> gradient;
  final int dailySeed;

  const _TodayVerseCard({
    required this.passageAsync,
    required this.gradient,
    required this.dailySeed,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    Widget body;
    body = passageAsync.when(
      loading: () => const SizedBox(
        height: 60,
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (_, __) => Text(
        '오늘의 말씀을 불러오지 못했어요.',
        style: theme.textTheme.bodyMedium?.copyWith(color: Colors.white),
      ),
      data: (passage) {
        final verses =
            passage.verses.where((v) => (v.koreanText ?? '').trim().isNotEmpty).toList();
        if (verses.isEmpty) {
          return Text(
            '오늘 준비된 말씀이 없어요.',
            style: theme.textTheme.bodyMedium?.copyWith(color: Colors.white),
          );
        }
        // 매일 랜덤 1절(시드 분리).
        final verse = verses[Random(dailySeed + 7).nextInt(verses.length)];
        final label =
            '${passage.reference.koreanBookName} ${verse.chapterNo}:${verse.verseNo}';
        return Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Text(
              '"${verse.koreanText!.trim()}"',
              textAlign: TextAlign.center,
              style: theme.textTheme.titleMedium?.copyWith(
                color: Colors.white,
                height: 1.55,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              label,
              style: theme.textTheme.labelLarge
                  ?.copyWith(color: Colors.white.withValues(alpha: 0.85)),
            ),
          ],
        );
      },
    );

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(20, 22, 20, 22),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: gradient,
        ),
      ),
      child: Column(
        children: [
          Text(
            '오늘의 말씀',
            style: theme.textTheme.labelMedium?.copyWith(
              color: Colors.white.withValues(alpha: 0.9),
              letterSpacing: 1.2,
            ),
          ),
          const SizedBox(height: 14),
          body,
        ],
      ),
    );
  }
}

class _RecentNotesList extends ConsumerWidget {
  const _RecentNotesList();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final recent = ref.watch(homeRecentNotesProvider);
    final theme = Theme.of(context);

    return recent.when(
      loading: () => const Padding(
        padding: EdgeInsets.symmetric(vertical: 16),
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (_, __) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Text('기록을 불러오지 못했어요.',
            style:
                theme.textTheme.bodyMedium?.copyWith(color: context.appColors.text2)),
      ),
      data: (items) {
        if (items.isEmpty) {
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: Text('아직 묵상 기록이 없어요. 오늘 첫 묵상을 시작해 보세요.',
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: context.appColors.text2)),
          );
        }
        return Column(
          children: [
            for (final item in items) _RecentNoteTile(item: item),
          ],
        );
      },
    );
  }
}

class _RecentNoteTile extends StatelessWidget {
  final NoteListItem item;

  const _RecentNoteTile({required this.item});

  String _dateLabel() {
    final d = item.updatedAt ?? item.createdAt;
    if (d == null) return '';
    return '${d.month}월 ${d.day}일';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final title = item.title.trim().isEmpty ? '(제목 없음)' : item.title.trim();
    final sub = [
      if (_dateLabel().isNotEmpty) _dateLabel(),
      noteCategoryLabel(item.category),
    ].join(' · ');

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        title: Text(title,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontWeight: FontWeight.w700)),
        subtitle: Text(sub,
            style: theme.textTheme.bodySmall
                ?.copyWith(color: context.appColors.text2)),
        trailing: const Icon(Icons.chevron_right),
        onTap: () => Navigator.of(context)
            .pushNamed(AppRouter.noteDetail, arguments: item.id),
      ),
    );
  }
}

/// 매일 랜덤으로 고를 차분한 어스톤 그라데이션 세트(흰 글자 가독).
const List<List<Color>> _kGradients = [
  [Color(0xFF6B7B6E), Color(0xFF3E463E)], // 세이지
  [Color(0xFF7A6A5D), Color(0xFF493F37)], // 클레이
  [Color(0xFF6E7780), Color(0xFF40464E)], // 슬레이트
  [Color(0xFF8A7B5C), Color(0xFF544A38)], // 샌드
  [Color(0xFF7A6E80), Color(0xFF47414F)], // 모브
  [Color(0xFF5E7480), Color(0xFF38444C)], // 블루슬레이트
];
