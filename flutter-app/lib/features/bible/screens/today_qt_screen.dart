import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../music/widgets/music_toggle_button.dart';
import '../../tts/widgets/qt_tts_button.dart';
import '../models/bible_models.dart';
import '../providers/bible_providers.dart';

/// QT 본문 전체 텍스트 (TTS 입력용).
String _fullTextOf(TodayQtPassage data) {
  final buf = StringBuffer();
  for (final v in data.verses) {
    final t = v.koreanText?.trim();
    if (t != null && t.isNotEmpty) buf.writeln(t);
  }
  return buf.toString().trim();
}

/// QT 날짜 (TTS 캐시 키용).
String _qtDateOf(TodayQtPassage data) =>
    data.passageDate ?? DateTime.now().toIso8601String().substring(0, 10);

class TodayQtScreen extends ConsumerWidget {
  const TodayQtScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final passage = ref.watch(todayQtPassageProvider);
    final data = passage.valueOrNull;
    final fullText = data == null ? '' : _fullTextOf(data);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.bibleTodayQt),
        actions: [
          // 본문 읽기(TTS) — 아이콘 하나로 재생/정지 토글
          if (data != null && fullText.isNotEmpty)
            QtTtsButton(
              qtText: fullText,
              qtDate: _qtDateOf(data),
              qtPassageId: data.qtPassageId,
            ),
          // 배경음악 켜기/끄기 — TTS 버튼 오른쪽 음표 토글
          const MusicToggleButton(),
          IconButton(
            tooltip: l.commonRefresh,
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.invalidate(todayQtPassageProvider),
          ),
        ],
      ),
      body: passage.whenOrDefault(
        loading: () => LoadingView(message: l.bibleTodayLoading),
        error: (error, _) => ErrorView(
          message: '${l.bibleTodayLoadError}\n$error',
          onRetry: () => ref.invalidate(todayQtPassageProvider),
        ),
        data: (data) => _TodayQtContent(data: data),
      ),
    );
  }
}

class _TodayQtContent extends StatelessWidget {
  final TodayQtPassage data;

  const _TodayQtContent({required this.data});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return RefreshIndicator(
      onRefresh: () async {},
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        children: [
          Text(
            data.reference.displayText,
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 6),
          if (data.title != null && data.title!.isNotEmpty) ...[
            Text(
              data.title!,
              style: theme.textTheme.titleMedium,
            ),
            const SizedBox(height: 6),
          ],
          Text(
            data.book.englishName,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          _ActionRow(
            qtPassageId: data.qtPassageId,
            simulatorStatus: data.simulatorStatus,
            hasExplanation: data.hasExplanation,
          ),
          const SizedBox(height: 20),
          for (final verse in data.verses) _VerseTile(verse: verse),
        ],
      ),
    );
  }
}

class _ActionRow extends StatelessWidget {
  final int? qtPassageId;
  final String simulatorStatus;
  final bool hasExplanation;

  const _ActionRow({
    required this.qtPassageId,
    required this.simulatorStatus,
    required this.hasExplanation,
  });

  void _showComingSoon(BuildContext context, String feature) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(AppLocalizations.of(context).bibleComingSoon(feature))),
    );
  }

  @override
  Widget build(BuildContext context) {
    // 버그 수정(2026-06-05): 서버 simulatorStatus/hasExplanation을 파싱하지 않고
    // 버튼을 영구 비활성(qtPassageId: null 고정)하던 단절 수정.
    // 활성 조건은 고정 제품 결정(CLAUDE.md §6)을 따른다:
    //  - 시뮬레이터 버튼은 simulatorStatus == READY 일 때만 활성화
    //  - 해설 버튼은 승인 해설 존재(hasExplanation) 시 활성화
    // 각 상세 화면 연결은 후속 작업(서버 계약 파리티 우선).
    final simulatorReady = qtPassageId != null && simulatorStatus == 'READY';
    final explanationReady = qtPassageId != null && hasExplanation;
    final l = AppLocalizations.of(context);

    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        FilledButton.icon(
          onPressed: explanationReady
              ? () => _showComingSoon(context, l.bibleExplanation)
              : null,
          icon: const Icon(Icons.menu_book_outlined),
          label: Text(l.bibleExplanation),
        ),
        OutlinedButton.icon(
          onPressed: simulatorReady
              ? () => _showComingSoon(context, l.bibleSimulator)
              : null,
          icon: const Icon(Icons.movie_outlined),
          label: Text(l.bibleSimulator),
        ),
        OutlinedButton.icon(
          onPressed: qtPassageId == null
              ? null
              : () => _showComingSoon(context, l.bibleMeditationNote),
          icon: const Icon(Icons.edit_note_outlined),
          label: Text(l.navNote),
        ),
      ],
    );
  }
}

class _VerseTile extends StatelessWidget {
  final BibleVerse verse;

  const _VerseTile({required this.verse});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final koreanText = verse.koreanText?.trim();
    final englishText = verse.englishText?.trim();

    return Padding(
      padding: const EdgeInsets.only(bottom: 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${verse.chapterNo}:${verse.verseNo}',
            style: theme.textTheme.labelLarge?.copyWith(
              color: theme.colorScheme.primary,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 6),
          if (koreanText != null && koreanText.isNotEmpty)
            Text(
              koreanText,
              style: theme.textTheme.bodyLarge?.copyWith(height: 1.6),
            ),
          if (englishText != null && englishText.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              englishText,
              style: theme.textTheme.bodyMedium?.copyWith(
                height: 1.5,
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ],
      ),
    );
  }
}
