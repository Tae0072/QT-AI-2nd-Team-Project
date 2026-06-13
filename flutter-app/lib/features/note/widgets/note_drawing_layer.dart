import 'package:flutter/material.dart';

import '../models/note_drawing.dart';

/// 원고(공책) 모드 배경 — 가로 줄 + 왼쪽 여백선을 옅게 그린다.
class RuledLinesPainter extends CustomPainter {
  final Color lineColor;
  final Color marginColor;
  final double lineGap;

  const RuledLinesPainter({
    required this.lineColor,
    required this.marginColor,
    this.lineGap = 30,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final linePaint = Paint()
      ..color = lineColor
      ..strokeWidth = 1;
    for (var y = lineGap; y < size.height; y += lineGap) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), linePaint);
    }
    // 왼쪽 여백 세로선(공책 느낌).
    final marginPaint = Paint()
      ..color = marginColor
      ..strokeWidth = 1;
    const marginX = 36.0;
    canvas.drawLine(Offset(marginX, 0), Offset(marginX, size.height), marginPaint);
  }

  @override
  bool shouldRepaint(RuledLinesPainter oldDelegate) =>
      oldDelegate.lineColor != lineColor ||
      oldDelegate.marginColor != marginColor ||
      oldDelegate.lineGap != lineGap;
}

/// 손그림(획) 오버레이.
///
/// - [enabled]가 true면 펜/손가락 입력을 받아 획을 그린다.
/// - false면 입력을 아래(텍스트 에디터)로 통과시키되 기존 획은 계속 보여준다.
/// - 좌표는 비율(0~1)로 저장/복원해 크기 변화에도 위치가 유지된다.
class NoteDrawingLayer extends StatefulWidget {
  final List<DrawingStroke> strokes;
  final bool enabled;
  final int colorValue;
  final double strokeWidth;
  final ValueChanged<List<DrawingStroke>> onStrokesChanged;

  const NoteDrawingLayer({
    super.key,
    required this.strokes,
    required this.enabled,
    required this.colorValue,
    required this.strokeWidth,
    required this.onStrokesChanged,
  });

  @override
  State<NoteDrawingLayer> createState() => _NoteDrawingLayerState();
}

class _NoteDrawingLayerState extends State<NoteDrawingLayer> {
  // 그리는 중인 획의 정규화 좌표(0~1).
  final List<Offset> _current = <Offset>[];

  Offset _normalize(Offset local, Size size) {
    final w = size.width <= 0 ? 1 : size.width;
    final h = size.height <= 0 ? 1 : size.height;
    return Offset(
      (local.dx / w).clamp(0.0, 1.0),
      (local.dy / h).clamp(0.0, 1.0),
    );
  }

  void _start(Offset local, Size size) {
    setState(() {
      _current
        ..clear()
        ..add(_normalize(local, size));
    });
  }

  void _move(Offset local, Size size) {
    setState(() => _current.add(_normalize(local, size)));
  }

  void _end() {
    if (_current.isEmpty) return;
    final stroke = DrawingStroke(
      points: List<Offset>.of(_current),
      colorValue: widget.colorValue,
      width: widget.strokeWidth,
    );
    widget.onStrokesChanged(<DrawingStroke>[...widget.strokes, stroke]);
    setState(_current.clear);
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final size = Size(constraints.maxWidth, constraints.maxHeight);
        final canvas = CustomPaint(
          size: Size.infinite,
          painter: _StrokesPainter(
            strokes: widget.strokes,
            current: _current,
            currentColor: widget.colorValue,
            currentWidth: widget.strokeWidth,
          ),
        );
        if (!widget.enabled) {
          // 펜 꺼짐: 입력은 통과(텍스트 편집), 그림은 계속 표시.
          return IgnorePointer(child: canvas);
        }
        return Listener(
          behavior: HitTestBehavior.opaque,
          onPointerDown: (e) => _start(e.localPosition, size),
          onPointerMove: (e) => _move(e.localPosition, size),
          onPointerUp: (_) => _end(),
          onPointerCancel: (_) => _end(),
          child: canvas,
        );
      },
    );
  }
}

class _StrokesPainter extends CustomPainter {
  final List<DrawingStroke> strokes;
  final List<Offset> current;
  final int currentColor;
  final double currentWidth;

  const _StrokesPainter({
    required this.strokes,
    required this.current,
    required this.currentColor,
    required this.currentWidth,
  });

  void _paintStroke(Canvas canvas, Size size, List<Offset> norm, int color,
      double width) {
    if (norm.isEmpty) return;
    final paint = Paint()
      ..color = Color(color)
      ..strokeWidth = width
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round
      ..style = PaintingStyle.stroke;
    final denorm = [
      for (final p in norm) Offset(p.dx * size.width, p.dy * size.height),
    ];
    if (denorm.length == 1) {
      // 점 하나는 작은 원으로(탭 한 번).
      canvas.drawCircle(denorm.first, width / 2, paint..style = PaintingStyle.fill);
      return;
    }
    final path = Path()..moveTo(denorm.first.dx, denorm.first.dy);
    for (var i = 1; i < denorm.length; i++) {
      path.lineTo(denorm[i].dx, denorm[i].dy);
    }
    canvas.drawPath(path, paint);
  }

  @override
  void paint(Canvas canvas, Size size) {
    for (final stroke in strokes) {
      _paintStroke(canvas, size, stroke.points, stroke.colorValue, stroke.width);
    }
    _paintStroke(canvas, size, current, currentColor, currentWidth);
  }

  @override
  bool shouldRepaint(_StrokesPainter oldDelegate) =>
      oldDelegate.strokes != strokes ||
      oldDelegate.current.length != current.length ||
      oldDelegate.currentColor != currentColor ||
      oldDelegate.currentWidth != currentWidth;
}
