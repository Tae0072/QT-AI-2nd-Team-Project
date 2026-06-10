import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';
import 'package:video_player/video_player.dart';

import '../models/bible_models.dart';
import '../providers/bible_providers.dart';

class QtVideoSection extends ConsumerWidget {
  final int qtPassageId;

  const QtVideoSection({
    super.key,
    required this.qtPassageId,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final clip = ref.watch(qtVideoClipProvider(qtPassageId));

    return clip.when(
      loading: () => const _QtVideoLoading(),
      error: (error, _) => _QtVideoError(
        onRetry: () => ref.invalidate(qtVideoClipProvider(qtPassageId)),
      ),
      data: (clip) {
        if (!clip.isReady) {
          return const SizedBox.shrink();
        }
        return _QtVideoBlock(
          clip: clip,
          qtPassageId: qtPassageId,
        );
      },
    );
  }
}

class _QtVideoBlock extends StatelessWidget {
  final QtVideoClip clip;
  final int qtPassageId;

  const _QtVideoBlock({
    required this.clip,
    required this.qtPassageId,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'QT 영상',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 10),
        QtVideoPlayer(
          videoUrl: clip.videoUrl!,
          cacheKey: qtVideoCacheKey(qtPassageId, clip.videoUrl!),
          startTimeSec: clip.startTimeSec,
          endTimeSec: clip.endTimeSec,
        ),
      ],
    );
  }
}

class _QtVideoLoading extends StatelessWidget {
  const _QtVideoLoading();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'QT 영상',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 10),
        AspectRatio(
          aspectRatio: 16 / 9,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: Colors.black,
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Center(child: CircularProgressIndicator()),
          ),
        ),
      ],
    );
  }
}

class _QtVideoError extends StatelessWidget {
  final VoidCallback onRetry;

  const _QtVideoError({required this.onRetry});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'QT 영상',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 10),
        OutlinedButton.icon(
          onPressed: onRetry,
          icon: const Icon(Icons.refresh),
          label: const Text('다시 불러오기'),
        ),
      ],
    );
  }
}

class QtVideoPlayer extends StatefulWidget {
  final String videoUrl;
  final String cacheKey;
  final double? startTimeSec;
  final double? endTimeSec;

  const QtVideoPlayer({
    super.key,
    required this.videoUrl,
    required this.cacheKey,
    this.startTimeSec,
    this.endTimeSec,
  });

  @override
  State<QtVideoPlayer> createState() => _QtVideoPlayerState();
}

class _QtVideoPlayerState extends State<QtVideoPlayer> {
  VideoPlayerController? _controller;
  late Future<void> _initializeFuture;
  double _speed = 1.0;
  bool _segmentEndHandled = false;
  bool _controlsVisible = true;
  int _controllerToken = 0;
  Timer? _controlsHideTimer;

  @override
  void initState() {
    super.initState();
    _createController();
  }

  @override
  void didUpdateWidget(covariant QtVideoPlayer oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.videoUrl != widget.videoUrl ||
        oldWidget.cacheKey != widget.cacheKey ||
        oldWidget.startTimeSec != widget.startTimeSec ||
        oldWidget.endTimeSec != widget.endTimeSec) {
      final controller = _controller;
      if (controller != null) {
        controller.removeListener(_handleTick);
        controller.dispose();
        _controller = null;
      }
      _createController();
    }
  }

  void _createController() {
    final token = ++_controllerToken;
    _segmentEndHandled = false;
    _initializeFuture = _prepareController(token);
  }

  Future<void> _prepareController(int token) async {
    final cachedFile = _segmentStart == Duration.zero
        ? await _QtVideoCache.existingFile(
            cacheKey: widget.cacheKey,
            videoUrl: widget.videoUrl,
          )
        : null;
    if (_segmentStart == Duration.zero && cachedFile == null) {
      unawaited(
        _QtVideoCache.download(
          cacheKey: widget.cacheKey,
          videoUrl: widget.videoUrl,
        ),
      );
    }
    final controller = cachedFile == null
        ? VideoPlayerController.networkUrl(
            Uri.parse(widget.videoUrl),
            videoPlayerOptions: VideoPlayerOptions(mixWithOthers: true),
          )
        : VideoPlayerController.file(
            cachedFile,
            videoPlayerOptions: VideoPlayerOptions(mixWithOthers: true),
          );

    if (!mounted || token != _controllerToken) {
      await controller.dispose();
      return;
    }

    _controller = controller..addListener(_handleTick);
    await controller.initialize();

    if (!mounted || token != _controllerToken) {
      controller.removeListener(_handleTick);
      await controller.dispose();
      return;
    }

    final start = _segmentStart;
    if (start > Duration.zero) {
      await controller.seekTo(start);
    }
    await controller.setPlaybackSpeed(_speed);

    if (mounted && token == _controllerToken) {
      setState(() {});
    }
  }

  void _handleTick() {
    final controller = _controller;
    if (controller == null) {
      return;
    }
    final end = _segmentEnd(controller);
    if (end != null &&
        !_segmentEndHandled &&
        controller.value.position >= end &&
        controller.value.isPlaying) {
      _segmentEndHandled = true;
      controller.pause();
      controller.seekTo(end);
      _controlsHideTimer?.cancel();
      _controlsVisible = true;
    }
    if (mounted) {
      setState(() {});
    }
  }

  @override
  void dispose() {
    _controlsHideTimer?.cancel();
    final controller = _controller;
    if (controller != null) {
      controller.removeListener(_handleTick);
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _togglePlay() async {
    final controller = _controller;
    if (controller == null) {
      return;
    }
    if (controller.value.isPlaying) {
      await controller.pause();
      _showControls();
    } else {
      final end = _segmentEnd(controller);
      if (end != null && controller.value.position >= end) {
        await controller.seekTo(_segmentStart);
      }
      _segmentEndHandled = false;
      await controller.play();
      _scheduleControlsHide();
    }
  }

  Future<void> _setSpeed(double speed) async {
    final controller = _controller;
    if (controller == null) {
      return;
    }
    await controller.setPlaybackSpeed(speed);
    if (mounted) {
      setState(() => _speed = speed);
    }
    _scheduleControlsHide();
  }

  void _toggleControls() {
    _controlsHideTimer?.cancel();
    setState(() => _controlsVisible = !_controlsVisible);
    if (_controlsVisible) {
      _scheduleControlsHide();
    }
  }

  void _showControls() {
    _controlsHideTimer?.cancel();
    if (mounted) {
      setState(() => _controlsVisible = true);
    }
  }

  void _scheduleControlsHide() {
    _controlsHideTimer?.cancel();
    final controller = _controller;
    if (controller == null || !controller.value.isPlaying) {
      return;
    }
    _controlsHideTimer = Timer(const Duration(seconds: 3), () {
      if (mounted) {
        setState(() => _controlsVisible = false);
      }
    });
  }

  Future<void> _openFullscreen() async {
    final controller = _controller;
    if (controller == null) {
      return;
    }
    _segmentEndHandled = false;
    final selectedSpeed = await Navigator.of(context).push<double>(
      MaterialPageRoute(
        fullscreenDialog: true,
        builder: (_) => _QtVideoFullscreen(
          controller: controller,
          initialSpeed: _speed,
          segmentStart: _segmentStart,
          segmentEnd: _segmentEnd(controller),
        ),
      ),
    );
    if (selectedSpeed != null && mounted) {
      setState(() => _speed = selectedSpeed);
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<void>(
      future: _initializeFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const _VideoShell(
            child: Center(child: CircularProgressIndicator()),
          );
        }
        final controller = _controller;
        if (controller == null || controller.value.hasError) {
          return const _VideoShell(
            child: Center(
              child: Icon(Icons.error_outline, color: Colors.white),
            ),
          );
        }
        return _VideoShell(
          child: Stack(
            fit: StackFit.expand,
            children: [
              Positioned.fill(
                child: _VideoFrame(
                  controller: controller,
                  fit: BoxFit.cover,
                ),
              ),
              Positioned.fill(
                child: GestureDetector(
                  behavior: HitTestBehavior.translucent,
                  onTap: _toggleControls,
                ),
              ),
              _SlidingVideoControls(
                visible: _controlsVisible,
                child: _VideoControls(
                  controller: controller,
                  speed: _speed,
                  segmentStart: _segmentStart,
                  segmentEnd: _segmentEnd(controller),
                  onTogglePlay: _togglePlay,
                  onSpeedSelected: _setSpeed,
                  onFullscreen: _openFullscreen,
                  overlay: true,
                  compact: true,
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Duration get _segmentStart => _secondsToDuration(widget.startTimeSec);

  Duration? _segmentEnd(VideoPlayerController controller) {
    final rawEnd = widget.endTimeSec == null
        ? null
        : _secondsToDuration(widget.endTimeSec);
    if (rawEnd == null || rawEnd <= _segmentStart) {
      return null;
    }
    final duration = controller.value.duration;
    if (duration > Duration.zero && rawEnd > duration) {
      return duration;
    }
    return rawEnd;
  }
}

class _QtVideoCache {
  static const _directoryName = 'qt-video-cache';
  static const _filePrefix = 'qt-video-';

  static Future<File?> existingFile({
    required String cacheKey,
    required String videoUrl,
  }) async {
    final uri = Uri.tryParse(videoUrl);
    if (uri == null || (!uri.isScheme('http') && !uri.isScheme('https'))) {
      return null;
    }

    try {
      final directory = await _cacheDirectory();
      final fileName = _fileNameFor(cacheKey);
      await _deleteStaleFiles(directory, keepFileName: fileName);

      final file = File(_join(directory.path, fileName));
      if (await file.exists() && await file.length() > 0) {
        return file;
      }

      return null;
    } catch (_) {
      return null;
    }
  }

  static Future<void> download({
    required String cacheKey,
    required String videoUrl,
  }) async {
    final uri = Uri.tryParse(videoUrl);
    if (uri == null || (!uri.isScheme('http') && !uri.isScheme('https'))) {
      return;
    }

    try {
      final directory = await _cacheDirectory();
      final fileName = _fileNameFor(cacheKey);
      await _deleteStaleFiles(directory, keepFileName: fileName);

      final file = File(_join(directory.path, fileName));
      if (await file.exists() && await file.length() > 0) {
        return;
      }
      await _download(uri, file);
    } catch (_) {
      // Network cache is an optimization; playback can continue streaming.
    }
  }

  static Future<Directory> _cacheDirectory() async {
    final root = await getTemporaryDirectory();
    final directory = Directory(_join(root.path, _directoryName));
    if (!await directory.exists()) {
      await directory.create(recursive: true);
    }
    return directory;
  }

  static Future<void> _deleteStaleFiles(
    Directory directory, {
    required String keepFileName,
  }) async {
    if (!await directory.exists()) {
      return;
    }

    await for (final entity in directory.list()) {
      if (entity is! File) {
        continue;
      }
      final name =
          entity.uri.pathSegments.isEmpty ? '' : entity.uri.pathSegments.last;
      if (!name.startsWith(_filePrefix)) {
        continue;
      }
      if (name != keepFileName) {
        await _deleteQuietly(entity);
      }
    }
  }

  static Future<File?> _download(Uri uri, File destination) async {
    final tempFile = File('${destination.path}.part');
    if (await tempFile.exists()) {
      await _deleteQuietly(tempFile);
    }

    final client = HttpClient();
    try {
      final request = await client.getUrl(uri);
      request.followRedirects = true;
      final response = await request.close();
      final isSuccess = response.statusCode == HttpStatus.ok ||
          response.statusCode == HttpStatus.partialContent;
      if (!isSuccess) {
        return null;
      }

      await response.pipe(tempFile.openWrite());
      if (await destination.exists()) {
        await _deleteQuietly(destination);
      }
      await tempFile.rename(destination.path);
      return destination;
    } finally {
      client.close(force: true);
      if (await tempFile.exists()) {
        await _deleteQuietly(tempFile);
      }
    }
  }

  static Future<void> _deleteQuietly(File file) async {
    try {
      await file.delete();
    } catch (_) {
      // Cache cleanup should never block playback.
    }
  }

  static String _fileNameFor(String cacheKey) {
    final safeKey = cacheKey.replaceAll(RegExp(r'[^A-Za-z0-9._-]'), '_');
    if (safeKey.toLowerCase().endsWith('.mp4')) {
      return safeKey;
    }
    return '$safeKey.mp4';
  }

  static String _join(String left, String right) {
    if (left.endsWith(Platform.pathSeparator)) {
      return '$left$right';
    }
    return '$left${Platform.pathSeparator}$right';
  }
}

class _QtVideoFullscreen extends StatefulWidget {
  final VideoPlayerController controller;
  final double initialSpeed;
  final Duration segmentStart;
  final Duration? segmentEnd;

  const _QtVideoFullscreen({
    required this.controller,
    required this.initialSpeed,
    required this.segmentStart,
    required this.segmentEnd,
  });

  @override
  State<_QtVideoFullscreen> createState() => _QtVideoFullscreenState();
}

class _QtVideoFullscreenState extends State<_QtVideoFullscreen> {
  late double _speed;
  bool _controlsVisible = true;
  Timer? _controlsHideTimer;

  @override
  void initState() {
    super.initState();
    _speed = widget.initialSpeed;
    widget.controller.addListener(_handleTick);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.black,
      systemNavigationBarColor: Colors.black,
      systemNavigationBarDividerColor: Colors.black,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarIconBrightness: Brightness.light,
      systemStatusBarContrastEnforced: false,
      systemNavigationBarContrastEnforced: false,
    ));
    SystemChrome.setPreferredOrientations(const [
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);
  }

  void _handleTick() {
    if (mounted) {
      setState(() {});
    }
  }

  @override
  void dispose() {
    _controlsHideTimer?.cancel();
    widget.controller.removeListener(_handleTick);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarDividerColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
      systemNavigationBarIconBrightness: Brightness.dark,
      systemStatusBarContrastEnforced: false,
      systemNavigationBarContrastEnforced: false,
    ));
    SystemChrome.setPreferredOrientations(DeviceOrientation.values);
    super.dispose();
  }

  Future<void> _togglePlay() async {
    if (widget.controller.value.isPlaying) {
      await widget.controller.pause();
      _showControls();
    } else {
      if (widget.segmentEnd != null &&
          widget.controller.value.position >= widget.segmentEnd!) {
        await widget.controller.seekTo(widget.segmentStart);
      }
      await widget.controller.play();
      _scheduleControlsHide();
    }
  }

  Future<void> _setSpeed(double speed) async {
    await widget.controller.setPlaybackSpeed(speed);
    if (mounted) {
      setState(() => _speed = speed);
    }
    _scheduleControlsHide();
  }

  void _toggleControls() {
    _controlsHideTimer?.cancel();
    setState(() => _controlsVisible = !_controlsVisible);
    if (_controlsVisible) {
      _scheduleControlsHide();
    }
  }

  void _showControls() {
    _controlsHideTimer?.cancel();
    if (mounted) {
      setState(() => _controlsVisible = true);
    }
  }

  void _scheduleControlsHide() {
    _controlsHideTimer?.cancel();
    if (!widget.controller.value.isPlaying) {
      return;
    }
    _controlsHideTimer = Timer(const Duration(seconds: 3), () {
      if (mounted) {
        setState(() => _controlsVisible = false);
      }
    });
  }

  void _close() {
    Navigator.of(context).pop(_speed);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: ColoredBox(
        color: Colors.black,
        child: Stack(
          fit: StackFit.expand,
          children: [
            Positioned.fill(
              child: _VideoFrame(
                controller: widget.controller,
                fit: BoxFit.cover,
              ),
            ),
            Positioned.fill(
              child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: _toggleControls,
              ),
            ),
            Align(
              alignment: Alignment.topLeft,
              child: SafeArea(
                bottom: false,
                right: false,
                child: IconButton(
                  tooltip: '뒤로',
                  onPressed: _close,
                  icon: const Icon(Icons.arrow_back, color: Colors.white),
                ),
              ),
            ),
            _SlidingVideoControls(
              visible: _controlsVisible,
              child: _VideoControls(
                controller: widget.controller,
                speed: _speed,
                segmentStart: widget.segmentStart,
                segmentEnd: widget.segmentEnd,
                onTogglePlay: _togglePlay,
                onSpeedSelected: _setSpeed,
                onFullscreen: _close,
                fullscreenIcon: Icons.fullscreen_exit,
                overlay: true,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _VideoFrame extends StatelessWidget {
  final VideoPlayerController controller;
  final BoxFit fit;

  const _VideoFrame({
    required this.controller,
    required this.fit,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final maxWidth = constraints.maxWidth;
        final maxHeight = constraints.maxHeight;
        final aspectRatio = _safeAspectRatio(controller);

        if (maxWidth <= 0 || maxHeight <= 0) {
          return const SizedBox.shrink();
        }

        final boxAspectRatio = maxWidth / maxHeight;
        late final double width;
        late final double height;

        if (fit == BoxFit.cover) {
          if (boxAspectRatio > aspectRatio) {
            width = maxWidth;
            height = width / aspectRatio;
          } else {
            height = maxHeight;
            width = height * aspectRatio;
          }
        } else {
          if (boxAspectRatio > aspectRatio) {
            height = maxHeight;
            width = height * aspectRatio;
          } else {
            width = maxWidth;
            height = width / aspectRatio;
          }
        }

        return ClipRect(
          child: Center(
            child: SizedBox(
              width: width,
              height: height,
              child: VideoPlayer(controller),
            ),
          ),
        );
      },
    );
  }
}

class _VideoShell extends StatelessWidget {
  final Widget child;

  const _VideoShell({required this.child});

  @override
  Widget build(BuildContext context) {
    return AspectRatio(
      aspectRatio: 16 / 9,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: Colors.black,
          borderRadius: BorderRadius.circular(8),
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: child,
        ),
      ),
    );
  }
}

class _SlidingVideoControls extends StatelessWidget {
  final bool visible;
  final Widget child;

  const _SlidingVideoControls({
    required this.visible,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Positioned(
      left: 0,
      right: 0,
      bottom: 0,
      child: IgnorePointer(
        ignoring: !visible,
        child: ClipRect(
          child: AnimatedSlide(
            offset: visible ? Offset.zero : const Offset(0, 1.2),
            duration: const Duration(milliseconds: 220),
            curve: Curves.easeOutCubic,
            child: AnimatedOpacity(
              opacity: visible ? 1 : 0,
              duration: const Duration(milliseconds: 180),
              child: child,
            ),
          ),
        ),
      ),
    );
  }
}

class _VideoControls extends StatelessWidget {
  final VideoPlayerController controller;
  final double speed;
  final Duration segmentStart;
  final Duration? segmentEnd;
  final VoidCallback onTogglePlay;
  final ValueChanged<double> onSpeedSelected;
  final VoidCallback onFullscreen;
  final IconData fullscreenIcon;
  final bool overlay;
  final bool compact;

  const _VideoControls({
    required this.controller,
    required this.speed,
    required this.segmentStart,
    required this.segmentEnd,
    required this.onTogglePlay,
    required this.onSpeedSelected,
    required this.onFullscreen,
    this.fullscreenIcon = Icons.fullscreen,
    this.overlay = false,
    this.compact = false,
  });

  @override
  Widget build(BuildContext context) {
    final value = controller.value;
    final duration = value.duration;
    final effectiveEnd = segmentEnd ?? duration;
    final segmentDuration =
        effectiveEnd > segmentStart ? effectiveEnd - segmentStart : duration;
    final absolutePosition =
        value.position > effectiveEnd ? effectiveEnd : value.position;
    final relativePosition = absolutePosition > segmentStart
        ? absolutePosition - segmentStart
        : Duration.zero;
    final maxMs = segmentDuration.inMilliseconds <= 0
        ? 1.0
        : segmentDuration.inMilliseconds.toDouble();
    final currentMs =
        relativePosition.inMilliseconds.clamp(0, maxMs.toInt()).toDouble();
    final horizontalPadding = compact ? 6.0 : 8.0;
    final bottomPadding = compact ? 3.0 : 8.0;
    final sliderHeight = compact ? 20.0 : 36.0;
    final iconSize = compact ? 18.0 : 24.0;
    final controlSize = compact ? 30.0 : 40.0;
    final textSize = compact ? 10.0 : 12.0;

    return ColoredBox(
      color: overlay ? Colors.black.withValues(alpha: 0.62) : Colors.black,
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          horizontalPadding,
          0,
          horizontalPadding,
          bottomPadding,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              height: sliderHeight,
              child: SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  trackHeight: compact ? 1.6 : 2,
                  thumbShape: RoundSliderThumbShape(
                    enabledThumbRadius: compact ? 4 : 5,
                  ),
                  overlayShape: RoundSliderOverlayShape(
                    overlayRadius: compact ? 10 : 14,
                  ),
                ),
                child: Slider(
                  value: currentMs,
                  min: 0,
                  max: maxMs,
                  onChanged: (value) {
                    controller.seekTo(
                      segmentStart + Duration(milliseconds: value.round()),
                    );
                  },
                ),
              ),
            ),
            Row(
              children: [
                IconButton(
                  tooltip: value.isPlaying ? '일시정지' : '재생',
                  onPressed: onTogglePlay,
                  iconSize: iconSize,
                  visualDensity: VisualDensity.compact,
                  constraints: BoxConstraints.tightFor(
                    width: controlSize,
                    height: controlSize,
                  ),
                  padding: EdgeInsets.zero,
                  icon: Icon(
                    value.isPlaying ? Icons.pause : Icons.play_arrow,
                    color: Colors.white,
                  ),
                ),
                Text(
                  '${_format(relativePosition)} / ${_format(segmentDuration)}',
                  style: TextStyle(color: Colors.white, fontSize: textSize),
                ),
                const Spacer(),
                PopupMenuButton<double>(
                  tooltip: '배속',
                  initialValue: speed,
                  padding: EdgeInsets.zero,
                  onSelected: onSpeedSelected,
                  itemBuilder: (context) => const [
                    PopupMenuItem(value: 0.75, child: Text('0.75x')),
                    PopupMenuItem(value: 1.0, child: Text('1.0x')),
                    PopupMenuItem(value: 1.25, child: Text('1.25x')),
                    PopupMenuItem(value: 1.5, child: Text('1.5x')),
                    PopupMenuItem(value: 2.0, child: Text('2.0x')),
                  ],
                  child: Padding(
                    padding: EdgeInsets.symmetric(
                      horizontal: compact ? 7 : 10,
                      vertical: compact ? 4 : 8,
                    ),
                    child: Text(
                      _formatSpeed(speed),
                      style: TextStyle(color: Colors.white, fontSize: textSize),
                    ),
                  ),
                ),
                IconButton(
                  tooltip: '전체화면',
                  onPressed: onFullscreen,
                  iconSize: iconSize,
                  visualDensity: VisualDensity.compact,
                  constraints: BoxConstraints.tightFor(
                    width: controlSize,
                    height: controlSize,
                  ),
                  padding: EdgeInsets.zero,
                  icon: Icon(fullscreenIcon, color: Colors.white),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

double _safeAspectRatio(VideoPlayerController controller) {
  final aspectRatio = controller.value.aspectRatio;
  if (aspectRatio.isNaN || aspectRatio <= 0) {
    return 16 / 9;
  }
  return aspectRatio;
}

Duration _secondsToDuration(double? seconds) {
  if (seconds == null || seconds <= 0) {
    return Duration.zero;
  }
  return Duration(milliseconds: (seconds * 1000).round());
}

String _formatSpeed(double speed) {
  if (speed == speed.roundToDouble()) {
    return '${speed.toStringAsFixed(0)}x';
  }
  if ((speed * 10) == (speed * 10).roundToDouble()) {
    return '${speed.toStringAsFixed(1)}x';
  }
  return '${speed.toStringAsFixed(2)}x';
}

String _format(Duration duration) {
  final minutes = duration.inMinutes.remainder(60).toString().padLeft(2, '0');
  final seconds = duration.inSeconds.remainder(60).toString().padLeft(2, '0');
  if (duration.inHours > 0) {
    return '${duration.inHours}:$minutes:$seconds';
  }
  return '$minutes:$seconds';
}

String qtVideoCacheKey(int qtPassageId, String videoUrl) {
  final uri = Uri.tryParse(videoUrl);
  final fileName = uri == null || uri.pathSegments.isEmpty
      ? 'video.mp4'
      : uri.pathSegments.last;
  final safeFileName = fileName.replaceAll(RegExp(r'[^A-Za-z0-9._-]'), '_');
  return 'qt-video-$qtPassageId-$safeFileName';
}
