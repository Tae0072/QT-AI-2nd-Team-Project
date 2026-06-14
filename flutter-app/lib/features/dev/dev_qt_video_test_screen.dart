// [DEV_MODE] =================================================================
// QT영상(시뮬레이터) 테스트 화면.
//  1) 샘플 mp4를 실제 영상 플레이어 위젯으로 바로 재생 — DB 데이터 없이도 플레이어
//     동작(재생/일시정지/배속/구간/전체화면)을 검증한다.
//  2) qtPassageId를 입력해 서버→DB 경로(QtVideoSection)로 실제 클립을 불러와,
//     "오늘의 QT 영상"이 DB에서 정상적으로 내려오는지 status까지 확인한다.
// 개발 종료 시: `[DEV_MODE]` 검색 후 삭제.
// ===========================================================================
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../bible/providers/bible_providers.dart';
import '../bible/widgets/qt_video_player.dart';

/// Flutter 공식 데모 영상(약 6초). 로컬 검증 전용 — 안정적으로 200 응답.
const String _kSampleVideoUrl =
    'https://flutter.github.io/assets-for-api-docs/assets/videos/butterfly.mp4';

class DevQtVideoTestScreen extends ConsumerStatefulWidget {
  const DevQtVideoTestScreen({super.key});

  @override
  ConsumerState<DevQtVideoTestScreen> createState() =>
      _DevQtVideoTestScreenState();
}

class _DevQtVideoTestScreenState extends ConsumerState<DevQtVideoTestScreen> {
  // 오늘 QT 본문 id가 보통 가장 큰 값이라 기본값으로 채워 둔다(필요 시 수정).
  final _passageIdCtrl = TextEditingController(text: '5');
  int? _loadedPassageId;

  @override
  void dispose() {
    _passageIdCtrl.dispose();
    super.dispose();
  }

  void _load() {
    final id = int.tryParse(_passageIdCtrl.text.trim());
    if (id == null || id < 1) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('올바른 QT 본문 ID(숫자)를 입력하세요.')),
      );
      return;
    }
    // 다시 누르면 항상 최신 상태로 받도록 캐시를 무효화한다.
    ref.invalidate(qtVideoClipProvider(id));
    setState(() => _loadedPassageId = id);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('QT영상 테스트'), centerTitle: true),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // ── 1) 샘플 영상 플레이어 ─────────────────────────────────────────
          Text('1) 샘플 영상 플레이어',
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          const Text(
            'DB 데이터와 무관하게 영상 플레이어 자체(재생·일시정지·배속·구간·전체화면)가 '
            '동작하는지 확인합니다. 공개 샘플 영상의 0~5초 구간을 재생합니다.',
            style: TextStyle(fontSize: 12, color: Colors.grey),
          ),
          const SizedBox(height: 10),
          const QtVideoPlayer(
            videoUrl: _kSampleVideoUrl,
            cacheKey: 'dev-sample-butterfly',
            startTimeSec: 0,
            endTimeSec: 5,
          ),
          const Divider(height: 32),

          // ── 2) 실제 DB 경로(서버) ─────────────────────────────────────────
          Text('2) 서버·DB에서 불러오기',
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          const Text(
            'QT 본문 ID로 서버(/qt/{id}/video)를 호출해 DB에 저장된 승인 클립을 받습니다. '
            '버튼이 비활성이던 "오늘의 QT 영상"과 동일한 경로입니다. '
            'status가 READY가 아니면 해당 본문에 승인된 영상 클립이 DB에 없는 것입니다.',
            style: TextStyle(fontSize: 12, color: Colors.grey),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              SizedBox(
                width: 120,
                child: TextField(
                  controller: _passageIdCtrl,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    labelText: 'QT 본문 ID',
                    isDense: true,
                    border: OutlineInputBorder(),
                  ),
                  onSubmitted: (_) => _load(),
                ),
              ),
              const SizedBox(width: 8),
              FilledButton(onPressed: _load, child: const Text('불러오기')),
            ],
          ),
          const SizedBox(height: 12),
          if (_loadedPassageId != null) _DbVideoResult(qtPassageId: _loadedPassageId!),
        ],
      ),
    );
  }
}

/// 입력한 본문 ID로 서버 클립을 받아 status를 보여주고, READY면 실제 섹션을 렌더한다.
class _DbVideoResult extends ConsumerWidget {
  final int qtPassageId;

  const _DbVideoResult({required this.qtPassageId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final clip = ref.watch(qtVideoClipProvider(qtPassageId));
    return clip.when(
      loading: () => const Padding(
        padding: EdgeInsets.symmetric(vertical: 16),
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => _StatusBox(
        color: Colors.red,
        text: '조회 실패: $e',
      ),
      data: (clip) {
        final lines = [
          'status: ${clip.status}',
          'isReady: ${clip.isReady}',
          if (clip.videoUrl != null) 'videoUrl: ${clip.videoUrl}',
          if (clip.startTimeSec != null || clip.endTimeSec != null)
            'segment: ${clip.startTimeSec ?? 0}s ~ ${clip.endTimeSec ?? '-'}s',
        ];
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _StatusBox(
              color: clip.isReady ? Colors.green : Colors.orange,
              text: lines.join('\n'),
            ),
            const SizedBox(height: 12),
            // 실제 화면과 동일한 위젯 — READY일 때만 플레이어가 보인다.
            QtVideoSection(qtPassageId: qtPassageId),
          ],
        );
      },
    );
  }
}

class _StatusBox extends StatelessWidget {
  final Color color;
  final String text;

  const _StatusBox({required this.color, required this.text});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.10),
        border: Border.all(color: color.withValues(alpha: 0.5)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: SelectableText(
        text,
        style: const TextStyle(fontFamily: 'monospace', fontSize: 12, height: 1.5),
      ),
    );
  }
}

// [DEV_MODE] end =============================================================
