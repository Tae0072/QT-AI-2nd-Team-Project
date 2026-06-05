import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import '../../bible/providers/bible_providers.dart';
import '../providers/tts_providers.dart';

/// QT 본문 읽기(TTS) 단일 아이콘 버튼.
///
/// 앱바의 뒤로가기/새로고침처럼 아이콘 하나로만 동작한다.
/// - QT 본문 로드 후 위젯이 뜨면 백그라운드에서 음성을 미리 준비한다.
/// - 탭: 재생 시작 (미준비 시 생성 후 자동 재생)
/// - 재생 중 탭: 정지 (처음으로 되감기)
/// - 생성 중: 작은 스피너 표시
/// 읽기 범위는 마이페이지 > 설정에서 변경한다:
/// - 본문(한글) 읽기 / 주석(해설) 읽기 — 둘 다 켜면 본문을 읽은 후 주석을 읽는다.
/// 목소리·범위 설정이 바뀌면 자동으로 다시 준비한다.
class QtTtsButton extends ConsumerStatefulWidget {
  final String qtText;
  final String qtDate; // 예: "2026-06-05"
  final int? qtPassageId; // 주석(해설) 조회용 — null이면 주석 읽기 불가

  const QtTtsButton({
    super.key,
    required this.qtText,
    required this.qtDate,
    this.qtPassageId,
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

  /// 본문과 해설 사이에 넣는 묵음 길이(초).
  ///
  /// TTS 서버(/qt/read)가 `[N초]` 태그를 해석해 해당 길이의 무음을 삽입한다.
  static const int _partPauseSeconds = 2;

  /// 설정(본문/해설)에 따라 읽을 텍스트를 조합한다.
  ///
  /// 반환: (낭독 텍스트, 캐시 범위 표시) — 읽을 내용이 없으면 텍스트가 빈 문자열.
  /// 둘 다 켜져 있으면 본문 → [2초] 묵음 → 해설 순서로 읽는다.
  Future<(String, String)> _composeText({required bool autoPlay}) async {
    final readBible = ref.read(ttsReadBibleProvider);
    final readExplanation = ref.read(ttsReadExplanationProvider);

    final parts = <String>[];
    var scope = '';

    if (readBible && widget.qtText.trim().isNotEmpty) {
      parts.add(widget.qtText.trim());
      scope += 'b';
    }

    if (readExplanation) {
      if (widget.qtPassageId == null) {
        if (autoPlay && parts.isEmpty) _showMessage('오늘 QT의 해설 정보가 없습니다');
      } else {
        try {
          final content = await ref
              .read(bibleRepositoryProvider)
              .getQtStudyContent(widget.qtPassageId!);
          final text = content.readableText;
          if (text.isNotEmpty) {
            parts.add(text);
            scope += 'e';
          } else if (autoPlay) {
            _showMessage(parts.isEmpty
                ? '아직 준비된 해설이 없습니다'
                : '해설이 아직 없어 본문만 읽습니다');
          }
        } catch (_) {
          // 해설 조회 실패 — 본문만이라도 읽고, 해설 전용이면 안내
          if (autoPlay) {
            _showMessage(parts.isEmpty
                ? '해설을 불러오지 못했습니다'
                : '해설을 불러오지 못해 본문만 읽습니다');
          }
        }
      }
    }

    if (parts.length > 1) {
      // 본문과 해설 사이 [N초] 묵음 태그 — 캐시 키에도 반영해 구버전과 구분
      return (
        parts.join('\n\n[$_partPauseSeconds초]\n\n'),
        '${scope}s$_partPauseSeconds',
      );
    }
    return (parts.join(), scope);
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
    if (_isGenerating) return;

    setState(() => _isGenerating = true);

    try {
      final (text, scope) = await _composeText(autoPlay: autoPlay);
      if (text.isEmpty) {
        if (autoPlay) _showMessage('설정에서 읽을 항목(본문/주석)을 켜 주세요');
        return;
      }

      // 캐시 키: 날짜_목소리해시_범위 — 같은 조합이면 재생성하지 않는다
      final voiceHash = voice.hashCode.toRadixString(16);
      final cacheKey = '${widget.qtDate}_${voiceHash}_$scope';

      final audioPath = await repo.generateQtAudio(
        text: text,
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

    // 설정(목소리/읽기 범위)이 바뀌면 음성을 다시 준비한다.
    ref.listen<String>(selectedVoiceProvider, (prev, next) {
      if (prev != next) {
        _player.stop();
        _prepareAudio();
      }
    });
    ref.listen<bool>(ttsReadBibleProvider, (prev, next) {
      if (prev != next) {
        _player.stop();
        _prepareAudio();
      }
    });
    ref.listen<bool>(ttsReadExplanationProvider, (prev, next) {
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
