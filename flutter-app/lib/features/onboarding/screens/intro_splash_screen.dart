import 'dart:math' as math;

import 'package:flutter/material.dart';

/// 앱 시작 인트로(로딩) 애니메이션.
///
/// 시퀀스(요청):
/// 1) 검은 화면 → 2) 중앙에서 빅뱅처럼 빛이 퍼져 전 화면이 하얘짐
/// → 3) 하얀빛을 거두듯 'QT·AI'의 T가 빛나는 십자가로 드러남
/// → 4) 'QT','AI' 사이 점(·)이 8각 빛으로 반짝이며 로딩 완료.
///
/// 애니메이션이 끝나고 [preload](백그라운드 데이터 준비)가 모두 끝나면 [onComplete]를 호출한다.
class IntroSplashScreen extends StatefulWidget {
  final VoidCallback onComplete;

  /// 로딩 동안 미리 받아둘 작업(인증 확인·오늘 QT 등). 실패해도 인트로는 끝난다.
  final Future<void> Function()? preload;

  /// 전체 재생 길이(기본 8초 = 검은1+빅뱅2+십자가3+별2).
  final Duration duration;

  const IntroSplashScreen({
    super.key,
    required this.onComplete,
    this.preload,
    this.duration = const Duration(seconds: 8),
  });

  @override
  State<IntroSplashScreen> createState() => _IntroSplashScreenState();
}

class _IntroSplashScreenState extends State<IntroSplashScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c;

  // 단계별 구간(컨트롤러 0~1 기준).
  late final Animation<double> _expand; // 중앙 점에서 빛이 방사되어 퍼짐(0~1)
  late final Animation<double> _gather; // 전 화면 백색 → T로 빛이 모임(0~1)
  late final Animation<double> _logoFade; // 로고 등장
  late final Animation<double> _crossGlow; // 십자가 빛남
  late final Animation<double> _star; // 8각 별 반짝임

  bool _completed = false;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(vsync: this, duration: widget.duration);

    // 단계(총 8초 기준): 검은화면 0~0.125(1s)
    // → 중앙 점에서 빛이 방사되어 전 화면 백색 0.125~0.375(2s)
    // → 백색에서 T 십자가로 빛이 빨려 모임 0.375~0.72(3s) → 8각 별 반짝임 0.72~1.0(2s).
    _expand = CurvedAnimation(
        parent: _c,
        curve: const Interval(0.125, 0.375, curve: Curves.easeOutCubic));
    _gather = CurvedAnimation(
        parent: _c,
        curve: const Interval(0.375, 0.72, curve: Curves.easeInOutCubic));
    _logoFade = CurvedAnimation(
        parent: _c, curve: const Interval(0.46, 0.64, curve: Curves.easeOut));
    _crossGlow = CurvedAnimation(
        parent: _c, curve: const Interval(0.45, 0.78, curve: Curves.easeOut));
    _star = CurvedAnimation(
        parent: _c, curve: const Interval(0.78, 1.0, curve: Curves.easeOut));

    _start();
  }

  Future<void> _start() async {
    final animDone = _c.forward();
    // 로딩 동안 백그라운드 준비를 함께 실행한다(실패해도 인트로는 끝낸다).
    try {
      await widget.preload?.call();
    } catch (_) {}
    try {
      await animDone;
    } catch (_) {}
    if (mounted && !_completed) {
      _completed = true;
      widget.onComplete();
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    // T(십자가)의 화면상 대략 위치 — 로고는 화면 중앙, 십자가는 'Q' 다음(중앙에서 살짝 왼쪽).
    // 빛이 거둬질 때 이 점으로 모이게 한다.
    final focal = size.center(Offset.zero).translate(-_LogoRow.fontSize * 0.46, 0);

    return Scaffold(
      backgroundColor: Colors.black,
      body: AnimatedBuilder(
        animation: _c,
        builder: (context, _) {
          return Stack(
            fit: StackFit.expand,
            children: [
              // 로고 — 빛이 십자가로 모이며 드러난다.
              Center(
                child: Opacity(
                  opacity: _logoFade.value,
                  child: _LogoRow(
                    crossGlow: _crossGlow.value,
                    starTwinkle: _star.value,
                  ),
                ),
              ),
              // 빛: 중앙 점에서 방사되어 퍼짐 → 전 화면 백색 → T로 빨려 모임.
              if (_expand.value > 0.0 || _gather.value < 1.0)
                Positioned.fill(
                  child: IgnorePointer(
                    child: CustomPaint(
                      painter: _FlashPainter(
                        expand: _expand.value,
                        gather: _gather.value,
                        focal: focal,
                      ),
                    ),
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}

/// 빅뱅(중앙에서 방사) → 전 화면 백색 → T(focal)로 수렴하는 빛.
///
/// 부드러운 [RadialGradient]로 '점에서 빛이 번지는' 느낌과 '빛이 한 점으로 빨려드는'
/// 느낌을 함께 표현한다. 단순 원이 커지는 것이 아니라 가장자리가 흐릿하게 퍼진다.
class _FlashPainter extends CustomPainter {
  final double expand; // 0~1: 중앙에서 빛이 퍼져 전 화면 백색
  final double gather; // 0~1: 백색이 focal로 모이며 사라짐
  final Offset focal; // 수렴 지점(T 십자가)

  _FlashPainter({
    required this.expand,
    required this.gather,
    required this.focal,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    final maxR = size.longestSide * 0.75;

    if (gather <= 0.0) {
      // 확산: 작은 중앙 점 → 점점 번져 전 화면 백색.
      if (expand <= 0.0) return;
      final e = expand.clamp(0.0, 1.0);
      final r = (0.02 + 1.45 * e) * maxR; // 점 → 화면 밖까지
      final coreA = (e * 1.25).clamp(0.0, 1.0);
      final paint = Paint()
        ..shader = RadialGradient(
          colors: [
            Colors.white.withValues(alpha: coreA),
            Colors.white.withValues(alpha: coreA * 0.85),
            Colors.white.withValues(alpha: 0.0),
          ],
          stops: const [0.0, 0.45, 1.0],
        ).createShader(Rect.fromCircle(center: center, radius: r));
      canvas.drawRect(Offset.zero & size, paint);
    } else {
      // 수렴: 전 화면 백색 → 중심이 focal로 이동하며 반지름이 줄어 십자가로 빨려든다.
      final g = gather.clamp(0.0, 1.0);
      final c = Offset.lerp(center, focal, g)!;
      final r = (1.45 - 1.32 * g) * maxR; // 화면 밖 → 십자가 크기
      final a = (1.0 - g); // 전체적으로 옅어짐
      final paint = Paint()
        ..shader = RadialGradient(
          colors: [
            Colors.white.withValues(alpha: a),
            Colors.white.withValues(alpha: a * 0.8),
            Colors.white.withValues(alpha: 0.0),
          ],
          stops: const [0.0, 0.5, 1.0],
        ).createShader(Rect.fromCircle(center: c, radius: r));
      canvas.drawRect(Offset.zero & size, paint);
    }
  }

  @override
  bool shouldRepaint(covariant _FlashPainter old) =>
      old.expand != expand || old.gather != gather || old.focal != focal;
}

/// 'Q' + 빛나는 십자가(T) + 8각 별(·) + 'AI'.
class _LogoRow extends StatelessWidget {
  final double crossGlow;
  final double starTwinkle;

  const _LogoRow({required this.crossGlow, required this.starTwinkle});

  /// 로고 글자 크기(빛 수렴 지점 계산에도 쓰인다).
  static const double fontSize = 58;

  @override
  Widget build(BuildContext context) {
    const textStyle = TextStyle(
      fontFamily: 'GowunDodum',
      fontSize: fontSize,
      fontWeight: FontWeight.w400,
      color: Colors.white,
      letterSpacing: -1.0,
      height: 1.0,
    );
    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        const Text('Q', style: textStyle),
        // T 자리 — 빛나는 십자가.
        CustomPaint(
          size: const Size(fontSize * 0.62, fontSize),
          painter: _CrossPainter(glow: crossGlow),
        ),
        // · 자리 — 8각 별.
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 3),
          child: CustomPaint(
            size: const Size(fontSize * 0.42, fontSize * 0.42),
            painter: _StarPainter(twinkle: starTwinkle),
          ),
        ),
        const Text('AI', style: textStyle),
      ],
    );
  }
}

/// 빛나는 십자가(글자 T 대용). [glow] 0~1로 빛 강도 조절.
class _CrossPainter extends CustomPainter {
  final double glow;

  _CrossPainter({required this.glow});

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    final cx = w / 2;

    final vBarW = w * 0.26;
    final hBarH = h * 0.15;
    final hBarY = h * 0.24; // 가로 막대 위치(위쪽)
    const radius = Radius.circular(3);

    void drawCross(Paint p, double pad) {
      final vBar = RRect.fromRectAndRadius(
        Rect.fromLTWH(cx - vBarW / 2 - pad, -pad, vBarW + pad * 2, h + pad * 2),
        radius,
      );
      final hBar = RRect.fromRectAndRadius(
        Rect.fromLTWH(-pad, hBarY - pad, w + pad * 2, hBarH + pad * 2),
        radius,
      );
      canvas.drawRRect(vBar, p);
      canvas.drawRRect(hBar, p);
    }

    // 빛 번짐(글로우) — 따뜻한 금빛.
    if (glow > 0) {
      final glowPaint = Paint()
        ..color = const Color(0xFFFFE8A8).withValues(alpha: 0.85 * glow)
        ..maskFilter = MaskFilter.blur(BlurStyle.normal, 6 + 14 * glow);
      drawCross(glowPaint, 1.5);
    }
    // 본체 — 빛이 차오르며 또렷해진다.
    final core = Paint()..color = Colors.white.withValues(alpha: 0.35 + 0.65 * glow);
    drawCross(core, 0);
  }

  @override
  bool shouldRepaint(covariant _CrossPainter old) => old.glow != glow;
}

/// 8각 빛(별). [twinkle] 0~1로 등장 + 반짝임.
class _StarPainter extends CustomPainter {
  final double twinkle;

  _StarPainter({required this.twinkle});

  @override
  void paint(Canvas canvas, Size size) {
    if (twinkle <= 0) return;
    final c = Offset(size.width / 2, size.height / 2);
    final maxR = size.width / 2;
    // 반짝임 — 살짝 커졌다 작아지는 맥동.
    final pulse = 0.85 + 0.15 * math.sin(twinkle * math.pi * 3);
    final outer = maxR * pulse;
    final inner = outer * 0.34;

    Path starPath(int points, double rot) {
      final path = Path();
      final step = math.pi / points;
      for (int i = 0; i < points * 2; i++) {
        final r = (i.isEven) ? outer : inner;
        final a = -math.pi / 2 + rot + i * step;
        final p = c + Offset(math.cos(a) * r, math.sin(a) * r);
        if (i == 0) {
          path.moveTo(p.dx, p.dy);
        } else {
          path.lineTo(p.dx, p.dy);
        }
      }
      path.close();
      return path;
    }

    final star = starPath(8, 0);

    // 글로우
    final glowPaint = Paint()
      ..color = const Color(0xFFFFE8A8).withValues(alpha: 0.9 * twinkle)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 8);
    canvas.drawPath(star, glowPaint);
    // 본체
    final body = Paint()..color = Colors.white.withValues(alpha: twinkle);
    canvas.drawPath(star, body);
    // 중심 밝은 점
    canvas.drawCircle(
        c, inner * 0.7, Paint()..color = Colors.white.withValues(alpha: twinkle));
  }

  @override
  bool shouldRepaint(covariant _StarPainter old) => old.twinkle != twinkle;
}
