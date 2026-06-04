import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import '../providers/tts_providers.dart';
import '../services/tts_repository.dart';

/// QT 본문 읽기용 오디오 플레이어 위젯.
///
/// QT 상세 화면 하단에 배치하여 음성 생성 → 재생을 한 곳에서 처리한다.
/// [qtText]에 QT 본문, [qtDate]에 날짜를 넘기면
/// 캐시 키를 자동 생성하여 같은 QT는 한 번만 생성한다.
class QtAudioPlayer extends ConsumerStatefulWidget {
  final String qtText;
  final String qtDate; // 예: "2026-06-04"

  const QtAudioPlayer({
    super.key,
    required this.qtText,
    required this.qtDate,
  });

  @override
  ConsumerState<QtAudioPlayer> createState() => _QtAudioPlayerState();
}

class _QtAudioPlayerState extends ConsumerState<QtAudioPlayer> {
  final AudioPlayer _player = AudioPlayer();
  bool _isGenerating = false;
  String? _errorMessage;
  Duration _position = Duration.zero;
  Duration _duration = Duration.zero;

  @override
  void initState() {
    super.initState();
    _player.positionStream.listen((pos) {
      if (mounted) setState(() => _position = pos);
    });
    _player.durationStream.listen((dur) {
      if (mounted && dur != null) setState(() => _duration = dur);
    });
    _player.playerStateStream.listen((state) {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _player.dispose();
    super.dispose();
  }

  Future<void> _generateAndPlay() async {
    final repo = ref.read(ttsRepositoryProvider);
    final voice = ref.read(selectedVoiceProvider);
    final token = ref.read(ttsTokenProvider);

    if (token.isEmpty) {
      setState(() => _errorMessage = 'TTS 토큰이 설정되지 않았습니다');
      return;
    }

    setState(() {
      _isGenerating = true;
      _errorMessage = null;
    });

    try {
      // 캐시 키: 날짜_목소리해시
      final voiceHash = voice.hashCode.toRadixString(16);
      final cacheKey = '${widget.qtDate}_$voiceHash';

      final audioPath = await repo.generateQtAudio(
        text: widget.qtText,
        voice: voice,
        cacheKey: cacheKey,
      );

      await _player.setFilePath(audioPath);
      await _player.play();
    } catch (e) {
      setState(() => _errorMessage = '음성 생성 실패: $e');
    } finally {
      setState(() => _isGenerating = false);
    }
  }

  String _formatDuration(Duration d) {
    final m = d.inMinutes;
    final s = d.inSeconds % 60;
    return '$m:${s.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final processing = _player.processingState;
    final playing = _player.playing;
    final hasAudio = processing != ProcessingState.idle;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 에러 메시지
          if (_errorMessage != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Text(
                _errorMessage!,
                style: TextStyle(color: theme.colorScheme.error, fontSize: 12),
              ),
            ),

          // 시크 바 + 시간
          if (hasAudio) ...[
            SliderTheme(
              data: SliderThemeData(
                trackHeight: 3,
                thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
                activeTrackColor: theme.colorScheme.primary,
                inactiveTrackColor: theme.colorScheme.outline.withValues(alpha: 0.3),
                thumbColor: theme.colorScheme.primary,
              ),
              child: Slider(
                value: _duration.inMilliseconds > 0
                    ? _position.inMilliseconds / _duration.inMilliseconds
                    : 0.0,
                onChanged: (v) {
                  final pos = Duration(
                    milliseconds: (v * _duration.inMilliseconds).round(),
                  );
                  _player.seek(pos);
                },
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(_formatDuration(_position),
                      style: TextStyle(fontSize: 11, color: theme.colorScheme.outline)),
                  Text(_formatDuration(_duration),
                      style: TextStyle(fontSize: 11, color: theme.colorScheme.outline)),
                ],
              ),
            ),
          ],

          const SizedBox(height: 4),

          // 컨트롤 버튼
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // 목소리 선택
              Consumer(builder: (context, ref, _) {
                final voice = ref.watch(selectedVoiceProvider);
                return TextButton.icon(
                  onPressed: () => _showVoiceSelector(context, ref),
                  icon: Icon(Icons.record_voice_over, size: 16,
                      color: theme.colorScheme.outline),
                  label: Text(voice, style: TextStyle(fontSize: 12,
                      color: theme.colorScheme.outline)),
                );
              }),

              const Spacer(),

              // 정지 (처음으로)
              if (hasAudio)
                IconButton(
                  onPressed: () {
                    _player.stop();
                    _player.seek(Duration.zero);
                  },
                  icon: const Icon(Icons.stop_rounded),
                  iconSize: 28,
                ),

              // 재생/일시정지/생성
              _isGenerating
                  ? const SizedBox(
                      width: 48, height: 48,
                      child: Padding(
                        padding: EdgeInsets.all(12),
                        child: CircularProgressIndicator(strokeWidth: 2.5),
                      ),
                    )
                  : IconButton.filled(
                      onPressed: () {
                        if (hasAudio && playing) {
                          _player.pause();
                        } else if (hasAudio) {
                          _player.play();
                        } else {
                          _generateAndPlay();
                        }
                      },
                      icon: Icon(
                        hasAudio && playing
                            ? Icons.pause_rounded
                            : Icons.play_arrow_rounded,
                      ),
                      iconSize: 32,
                      style: IconButton.styleFrom(
                        backgroundColor: theme.colorScheme.primary,
                        foregroundColor: theme.colorScheme.onPrimary,
                      ),
                    ),

              const Spacer(),
              const SizedBox(width: 80), // 좌우 균형
            ],
          ),
        ],
      ),
    );
  }

  void _showVoiceSelector(BuildContext context, WidgetRef ref) {
    final voices = ref.read(ttsVoicesProvider);
    voices.when(
      data: (list) {
        if (list.isEmpty) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('TTS 서버에 연결할 수 없습니다')),
          );
          return;
        }
        showModalBottomSheet(
          context: context,
          builder: (ctx) => ListView.builder(
            shrinkWrap: true,
            itemCount: list.length,
            itemBuilder: (ctx, i) {
              final v = list[i];
              return ListTile(
                title: Text(v.displayName),
                subtitle: Text(v.type == 'custom' ? '커스텀 목소리' : '기본 목소리'),
                trailing: v.hasFinetuned
                    ? const Chip(label: Text('학습됨', style: TextStyle(fontSize: 10)))
                    : null,
                onTap: () {
                  ref.read(selectedVoiceProvider.notifier).state = v.name;
                  Navigator.pop(ctx);
                },
              );
            },
          ),
        );
      },
      loading: () {},
      error: (_, __) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('목소리 목록을 불러올 수 없습니다')),
        );
      },
    );
  }
}
