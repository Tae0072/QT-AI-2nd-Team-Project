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
  late final Animation<double> _burst; // 중앙 빛 폭발(스케일)
  late final Animation<double> _veil; // 전 화면 하양(피크 후 사라짐)
  late final Animation<double> _logoFade; // 로고 등장
  late final Animation<double> _crossGlow; // 십자가 빛남
  late final Animation<double> _star; // 8각 별 반짝임

  bool _completed = false;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(vsync: this, duration: widget.duration);

    // 단계(총 8초 기준): 검은화면 0~0.125(1s) → 빅뱅·전화면 백색 0.125~0.375(2s)
    // → 백색에서 T 십자가로 빛이 모임 0.375~0.75(3s) → 8각 별 반짝임 0.75~1.0(2s).
    _burst = CurvedAnimation(
        parent: _c,
        curve: const Interval(0.125, 0.375, curve: Curves.easeOutCubic));
    // 백색 막: 0.125~0.375 차오름(전 화면 백색) → 0.45~0.70 거둬짐(십자가로 모임).
    _veil = TweenSequence<double>([
      TweenSequenceItem(tween: ConstantTween(0.0), weight: 12.5),
      TweenSequenceItem(tween: Tween(begin: 0.0, end: 1.0), weight: 25),
      TweenSequenceItem(tween: ConstantTween(1.0), weight: 7.5),
      TweenSequenceItem(tween: Tween(begin: 1.0, end: 0.0), weight: 25),
      TweenSequenceItem(tween: ConstantTween(0.0), weight: 30),
    ]).animate(_c);
    _logoFade = CurvedAnimation(
        parent: _c, curve: const Interval(0.45, 0.62, curve: Curves.easeOut));
    _crossGlow = CurvedAnimation(
        parent: _c, curve: const Interval(0.46, 0.75, curve: Curves.easeOut));
    _star = CurvedAnimation(
        parent: _c, curve: const Interval(0.75, 1.0, curve: Curves.easeOut));

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
    final burstMax = size.longestSide * 1.6;

    return Scaffold(
      backgroundColor: Colors.black,
      body: AnimatedBuilder(
        animation: _c,
        builder: (context, _) {
          return Stack(
            fit: StackFit.expand,
            children: [
              // 로고 — 빛이 거둬지며 드러난다.
              Center(
                child: Opacity(
                  opacity: _logoFade.value,
                  child: _LogoRow(
                    crossGlow: _crossGlow.value,
                    starTwinkle: _star.value,
                  ),
                ),
              ),
              // 중앙 빅뱅 빛 폭발(퍼지는 원형 빛).
              if (_veil.value > 0.001 || _burst.value < 1.0)
                IgnorePointer(
                  child: Center(
                    child: Container(
                      width: burstMax * _burst.value,
                      height: burstMax * _burst.value,
                      decoration: const BoxDecoration(
                        shape: BoxShape.circle,
                        gradient: RadialGradient(
                          colors: [Colors.white, Color(0x00FFFFFF)],
                          stops: [0.55, 1.0],
                        ),
                      ),
                    ),
                  ),
                ),
              // 전 화면 하양(피크 후 거둬짐).
              if (_veil.value > 0.001)
                IgnorePointer(
                  child: ColoredBox(
                    color: Colors.white.withValues(alpha: _veil.value),
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}

/// 'Q' + 빛나는 십자가(T) + 8각 별(·) + 'AI'.
class _LogoRow extends StatelessWidget {
  final double crossGlow;
  final double starTwinkle;

  const _LogoRow({required this.crossGlow, required this.starTwinkle});

  @override
  Widget build(BuildContext context) {
    const double fontSize = 58;
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
