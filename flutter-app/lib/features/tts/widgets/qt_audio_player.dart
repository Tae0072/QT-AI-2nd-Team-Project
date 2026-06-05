import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import '../providers/tts_providers.dart';

/// QT 본문 읽기용 오디오 플레이어 위젯.
///
/// QT 상세 화면 하단에 배치한다. 위젯이 뜨는 즉시(=QT 본문 로드 시)
/// 백그라운드로 음성을 미리 생성하고, ▶ 버튼은 준비된 음성을 바로 재생한다.
/// [qtText]에 QT 본문, [qtDate]에 날짜를 넘기면
/// 캐시 키를 자동 생성하여 같은 QT는 한 번만 생성한다.
/// 목소리는 마이페이지 > 설정에서 변경하며, 변경 시 자동으로 다시 준비한다.
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

    // QT 본문이 로드되어 위젯이 뜨면 바로 음성을 미리 준비한다 (자동 생성).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _prepareAudio();
    });
  }

  @override
  void dispose() {
    _player.dispose();
    super.dispose();
  }

  /// 음성을 생성(또는 캐시에서 로드)해 플레이어에 세팅한다.
  ///
  /// [autoPlay]가 true면 준비 완료 후 바로 재생한다.
  /// 위젯 표시 직후에는 autoPlay=false로 미리 준비만 해두고,
  /// 준비 전에 ▶를 누르면 autoPlay=true로 호출된다.
  Future<void> _prepareAudio({bool autoPlay = false}) async {
    final repo = ref.read(ttsRepositoryProvider);
    final voice = ref.read(selectedVoiceProvider);
    final token = ref.read(ttsTokenProvider);

    if (token.isEmpty) {
      setState(() => _errorMessage = 'TTS 토큰이 설정되지 않았습니다');
      return;
    }
    if (widget.qtText.trim().isEmpty || _isGenerating) return;

    setState(() {
      _isGenerating = true;
      _errorMessage = null;
    });

    try {
      // 캐시 키: 날짜_목소리해시 — 같은 날짜·목소리면 재생성하지 않는다
      final voiceHash = voice.hashCode.toRadixString(16);
      final cacheKey = '${widget.qtDate}_$voiceHash';

      final audioPath = await repo.generateQtAudio(
        text: widget.qtText,
        voice: voice,
        cacheKey: cacheKey,
      );

      if (!mounted) return;
      await _player.setFilePath(audioPath);
      if (autoPlay) await _player.play();
    } catch (e) {
      if (mounted) setState(() => _errorMessage = '음성 생성 실패: $e');
    } finally {
      if (mounted) setState(() => _isGenerating = false);
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

    // 설정에서 목소리가 바뀌면 새 목소리로 음성을 다시 준비한다.
    ref.listen<String>(selectedVoiceProvider, (prev, next) {
      if (prev != next) {
        _player.stop();
        _prepareAudio();
      }
    });

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
              // 현재 목소리 표시 (변경은 마이페이지 > 설정)
              Consumer(builder: (context, ref, _) {
                final voice = ref.watch(selectedVoiceProvider);
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.record_voice_over, size: 16,
                          color: theme.colorScheme.outline),
                      const SizedBox(width: 4),
                      Text(voice, style: TextStyle(fontSize: 12,
                          color: theme.colorScheme.outline)),
                    ],
                  ),
                );
              }),

              const Spacer(),

              // TTS 시작/정지 토글 — 누르면 재생, 다시 누르면 정지
              _isGenerating
                  ? const SizedBox(
                      width: 52, height: 52,
                      child: Padding(
                        padding: EdgeInsets.all(13),
                        child: CircularProgressIndicator(strokeWidth: 2.5),
                      ),
                    )
                  : IconButton.filled(
                      tooltip: playing ? 'TTS 정지' : 'TTS 시작',
                      onPressed: () {
                        if (playing) {
                          // 한 번 더 누르면 정지 (처음으로 되감기)
                          _player.stop();
                          _player.seek(Duration.zero);
                        } else if (hasAudio) {
                          // 미리 준비된 음성을 바로 재생
                          _player.play();
                        } else {
                          // 준비 실패/미완료 시 생성 후 바로 재생
                          _prepareAudio(autoPlay: true);
                        }
                      },
                      icon: const ImageIcon(
                        AssetImage('assets/icons/tts_voice.png'),
                      ),
                      iconSize: 34,
                      style: IconButton.styleFrom(
                        backgroundColor: playing
                            ? theme.colorScheme.error
                            : theme.colorScheme.primary,
                        foregroundColor: theme.colorScheme.onPrimary,
                        minimumSize: const Size(52, 52),
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

}
