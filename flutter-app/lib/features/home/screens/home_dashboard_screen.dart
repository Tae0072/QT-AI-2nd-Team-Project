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
          // 인사 텍스트가 상단·아래 카드와 충분히 떨어지도록 위/아래 여백을 크게.
          padding: const EdgeInsets.fromLTRB(20, 72, 20, 24),
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
            const SizedBox(height: 56),

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
                  // 기록 탭의 + 버튼과 동일한 액센트 색.
                  backgroundColor: context.appColors.accentDot,
                  foregroundColor: Colors.white,
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
    final colors = context.appColors;

    // 본문(라벨 + 절 + 참조) — 가운데 정렬. 절은 폭을 제한해 길면 4~5줄로 감싸 가운데로.
    final content = passageAsync.when(
      loading: () => const Padding(
        padding: EdgeInsets.symmetric(vertical: 28),
        child: CircularProgressIndicator(color: Colors.white),
      ),
      error: (_, __) => Text(
        '오늘의 말씀을 불러오지 못했어요.',
        textAlign: TextAlign.center,
        style: theme.textTheme.bodyMedium?.copyWith(color: Colors.white),
      ),
      data: (passage) {
        final verses = passage.verses
            .where((v) => (v.koreanText ?? '').trim().isNotEmpty)
            .toList();
        if (verses.isEmpty) {
          return Text(
            '오늘 준비된 말씀이 없어요.',
            textAlign: TextAlign.center,
            style: theme.textTheme.bodyMedium?.copyWith(color: Colors.white),
          );
        }
        // 매일 랜덤 1절(시드 분리).
        final verse = verses[Random(dailySeed + 7).nextInt(verses.length)];
        final label =
            '${passage.reference.koreanBookName} ${verse.chapterNo}:${verse.verseNo}';
        return Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Text(
              '오늘의 말씀',
              textAlign: TextAlign.center,
              style: theme.textTheme.labelMedium?.copyWith(
                color: Colors.white.withValues(alpha: 0.9),
                letterSpacing: 1.2,
              ),
            ),
            const SizedBox(height: 14),
            Text(
              '"${verse.koreanText!.trim()}"',
              textAlign: TextAlign.center,
              style: theme.textTheme.titleMedium?.copyWith(
                color: Colors.white,
                height: 1.6,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 14),
            Text(
              label,
              textAlign: TextAlign.center,
              style: theme.textTheme.labelLarge
                  ?.copyWith(color: Colors.white.withValues(alpha: 0.85)),
            ),
          ],
        );
      },
    );

    // 배경: 그림 자산이 있으면 이미지, 없으면 그라데이션 + 기독교 드로잉(빛+십자가).
    final hasImage = _kBackgroundAssets.isNotEmpty;
    final Widget background = hasImage
        ? Image.asset(
            _kBackgroundAssets[Random(dailySeed).nextInt(_kBackgroundAssets.length)],
            fit: BoxFit.cover,
            errorBuilder: (_, __, ___) =>
                _GradientBackdrop(gradient: gradient),
          )
        : _GradientBackdrop(gradient: gradient);

    return Container(
      // 그림 뒤에 한 겹 더 있는 형태(외곽 프레임).
      padding: const EdgeInsets.all(6),
      decoration: BoxDecoration(
        color: colors.bgSunken,
        borderRadius: BorderRadius.circular(22),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: ConstrainedBox(
          // 카드는 본문에 맞춰 늘어나되 최소 높이를 확보(4~5줄도 여유).
          constraints: const BoxConstraints(minHeight: 200),
          child: Stack(
            fit: StackFit.passthrough,
            children: [
              Positioned.fill(child: background),
              // 글자 가독용 어두운 막(이미지/그라데이션 공통).
              Positioned.fill(
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [
                        Colors.black.withValues(alpha: 0.30),
                        Colors.black.withValues(alpha: 0.52),
                      ],
                    ),
                  ),
                ),
              ),
              // 본문 — 배경 그림 기준 정중앙, 폭 제한으로 가운데 정렬.
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 28),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 600),
                    child: content,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 그림 자산이 없을 때의 기본 배경 — 그라데이션 + 기독교 드로잉(빛+십자가).
class _GradientBackdrop extends StatelessWidget {
  final List<Color> gradient;
  const _GradientBackdrop({required this.gradient});

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: gradient,
        ),
      ),
      child: const CustomPaint(painter: _ChristianBackdropPainter()),
    );
  }
}

/// 오늘의 말씀 카드의 기독교 배경 그림 — 하늘에서 비치는 빛줄기 + 은은한 십자가.
/// 외부 사진 없이 직접 그려 저작권 부담이 없고, 흰 본문 글자 가독을 위해 농도는 낮게.
class _ChristianBackdropPainter extends CustomPainter {
  const _ChristianBackdropPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;

    // 1) 상단 중앙에서 퍼지는 은은한 빛 글로우.
    final glowCenter = Offset(w * 0.5, -h * 0.15);
    canvas.drawRect(
      Offset.zero & size,
      Paint()
        ..shader = RadialGradient(
          colors: [
            Colors.white.withValues(alpha: 0.16),
            Colors.white.withValues(alpha: 0.0),
          ],
        ).createShader(Rect.fromCircle(center: glowCenter, radius: h * 1.3)),
    );

    // 2) 빛줄기(rays) — 광원에서 아래로 부채꼴로 퍼진다.
    final rayPaint = Paint()..color = Colors.white.withValues(alpha: 0.05);
    const rayCount = 7;
    for (var i = 0; i < rayCount; i++) {
      final t = (i / (rayCount - 1)) - 0.5; // -0.5 ~ 0.5
      final baseX = w * (0.5 + t * 1.3);
      final spread = w * 0.05;
      final path = Path()
        ..moveTo(glowCenter.dx, glowCenter.dy)
        ..lineTo(baseX - spread, h * 1.1)
        ..lineTo(baseX + spread, h * 1.1)
        ..close();
      canvas.drawPath(path, rayPaint);
    }

    // 3) 은은한 십자가(오른쪽) — 본문 가독을 위해 중앙은 비워 둔다.
    final cx = w * 0.84;
    final cy = h * 0.52;
    final barW = 7.0;
    final vTop = cy - h * 0.30;
    final vBot = cy + h * 0.30;
    final hHalf = h * 0.13;
    final crossPaint = Paint()..color = Colors.white.withValues(alpha: 0.12);
    final radius = const Radius.circular(3);
    // 세로 기둥
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTRB(cx - barW / 2, vTop, cx + barW / 2, vBot),
        radius,
      ),
      crossPaint,
    );
    // 가로 기둥(위쪽 1/3 지점)
    final hY = cy - h * 0.12;
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTRB(cx - hHalf, hY - barW / 2, cx + hHalf, hY + barW / 2),
        radius,
      ),
      crossPaint,
    );
  }

  @override
  bool shouldRepaint(_ChristianBackdropPainter oldDelegate) => false;
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

/// 오늘의 말씀 카드 배경 그림(기독교) 자산 경로 — 매일 랜덤으로 한 장.
/// 모두 직접 그린 원본 일러스트라 저작권 부담이 없다.
const List<String> _kBackgroundAssets = <String>[
  'assets/home_backgrounds/sunrise_cross.png',
  'assets/home_backgrounds/three_crosses.png',
  'assets/home_backgrounds/mountains_dawn.png',
  'assets/home_backgrounds/valley_path.png',
  'assets/home_backgrounds/church_evening.png',
  'assets/home_backgrounds/stained_glass.png',
];

/// 매일 랜덤으로 고를 차분한 어스톤 그라데이션 세트(3스톱, 흰 글자 가독).
/// 단색처럼 보이지 않도록 위→아래 명도 대비를 충분히 둔다.
const List<List<Color>> _kGradients = [
  [Color(0xFF8FA38C), Color(0xFF5E6E5E), Color(0xFF323A32)], // 세이지
  [Color(0xFFA08B77), Color(0xFF6F5E4F), Color(0xFF3C332B)], // 클레이
  [Color(0xFF8A95A0), Color(0xFF5C636C), Color(0xFF333941)], // 슬레이트
  [Color(0xFFB0A077), Color(0xFF7C6E4F), Color(0xFF463D2C)], // 샌드
  [Color(0xFF978AA0), Color(0xFF665F70), Color(0xFF3B3545)], // 모브
  [Color(0xFF7E97A4), Color(0xFF536572), Color(0xFF2E3A41)], // 블루슬레이트
];
