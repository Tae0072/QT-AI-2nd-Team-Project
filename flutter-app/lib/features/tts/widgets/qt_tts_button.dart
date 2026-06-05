import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import '../providers/tts_providers.dart';

/// QT 본문 읽기(TTS) 단일 아이콘 버튼.
///
/// 앱바의 뒤로가기/새로고침처럼 아이콘 하나로만 동작한다.
/// - QT 본문 로드 후 위젯이 뜨면 백그라운드에서 음성을 미리 준비한다.
/// - 탭: 재생 시작 (미준비 시 생성 후 자동 재생)
/// - 재생 중 탭: 정지 (처음으로 되감기)
/// - 생성 중: 작은 스피너 표시
/// 목소리는 마이페이지 > 설정에서 변경하며, 변경 시 자동으로 다시 준비한다.
class QtTtsButton extends ConsumerStatefulWidget {
  final String qtText;
  final String qtDate; // 예: "2026-06-05"

  const QtTtsButton({
    super.key,
    required this.qtText,
    required this.qtDate,
  });

  @override
  ConsumerState<QtTtsButton> createState() => _QtTtsButtonState();
}

class _QtTtsButtonState extends ConsumerState<QtTtsButton> {
  final AudioPlayer _player = AudioPlayer();
  bool _isGenerating = false;

  @override
  void initState() {
    super.initState();
    _player.playerStateStream.listen((state) {
      if (!mounted) return;
      // 끝까지 재생되면 정지 상태로 초기화 (다시 탭하면 처음부터)
      if (state.processingState == ProcessingState.completed) {
        _player.pause();
        _player.seek(Duration.zero);
      }
      setState(() {});
    });

    // QT 본문이 로드되면 바로 음성을 미리 준비한다 (자동 생성).
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
  /// [autoPlay]가 true면 준비 완료 후 바로 재생한다.
  Future<void> _prepareAudio({bool autoPlay = false}) async {
    final repo = ref.read(ttsRepositoryProvider);
    final voice = ref.read(selectedVoiceProvider);
    final token = ref.read(ttsTokenProvider);

    if (token.isEmpty) {
      if (autoPlay) _showMessage('TTS 토큰이 설정되지 않았습니다');
      return;
    }
    if (widget.qtText.trim().isEmpty || _isGenerating) return;

    setState(() => _isGenerating = true);

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
    } catch (_) {
      if (mounted && autoPlay) _showMessage('음성을 준비하지 못했습니다');
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  void _onTap() {
    if (_player.playing) {
      // 한 번 더 누르면 정지 (처음으로 되감기)
      _player.pause();
      _player.seek(Duration.zero);
    } else if (_player.processingState != ProcessingState.idle) {
      // 미리 준비된 음성을 바로 재생
      _player.play();
    } else {
      // 준비 실패/미완료 시 생성 후 바로 재생
      _prepareAudio(autoPlay: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    // 설정에서 목소리가 바뀌면 새 목소리로 음성을 다시 준비한다.
    ref.listen<String>(selectedVoiceProvider, (prev, next) {
      if (prev != next) {
        _player.stop();
        _prepareAudio();
      }
    });

    if (_isGenerating) {
      return const SizedBox(
        width: 48,
        height: 48,
        child: Padding(
          padding: EdgeInsets.all(14),
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      );
    }

    final playing = _player.playing;
    return IconButton(
      tooltip: playing ? '읽기 정지' : '본문 읽기',
      onPressed: _onTap,
      icon: ImageIcon(
        const AssetImage('assets/icons/tts_voice.png'),
        color: playing ? theme.colorScheme.error : null,
      ),
      iconSize: 26,
    );
  }
}
