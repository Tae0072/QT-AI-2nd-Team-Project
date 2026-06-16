import 'dart:ui';

/// 노트 페이지 표시 모드.
/// - [plain]: 일반(배경 줄 없음)
/// - [manuscript]: 원고(공책처럼 가로 줄이 그어진 배경)
enum NotePageMode {
  plain,
  manuscript;

  String get storageValue => name;

  static NotePageMode fromStorage(String? value) {
    return NotePageMode.values.firstWhere(
      (m) => m.name == value,
      orElse: () => NotePageMode.plain,
    );
  }
}

/// 손가락/스타일러스(S펜·애플펜슬)로 그린 한 획.
///
/// 좌표는 캔버스 크기에 대한 비율(0.0~1.0)로 저장한다 → 화면 회전·크기 변화·
/// 다른 기기 해상도에서도 같은 위치에 다시 그려진다.
class DrawingStroke {
  /// 정규화 좌표(각 점의 dx,dy 모두 0~1).
  final List<Offset> points;

  /// 선 색(ARGB 32bit 정수).
  final int colorValue;

  /// 선 두께(논리 픽셀, 정규화 안 함 — 화면 차이는 작아 허용).
  final double width;

  const DrawingStroke({
    required this.points,
    required this.colorValue,
    required this.width,
  });

  Map<String, dynamic> toJson() => {
        'c': colorValue,
        'w': width,
        // 점은 [dx, dy] 쌍의 배열로 평탄하게 저장한다.
        'p': [
          for (final point in points) [point.dx, point.dy],
        ],
      };

  factory DrawingStroke.fromJson(Map<String, dynamic> json) {
    final rawPoints = (json['p'] as List<dynamic>? ?? const []);
    return DrawingStroke(
      colorValue: (json['c'] as num?)?.toInt() ?? 0xFF111827,
      width: (json['w'] as num?)?.toDouble() ?? 3.0,
      points: [
        for (final p in rawPoints)
          Offset(
            ((p as List<dynamic>)[0] as num).toDouble(),
            (p[1] as num).toDouble(),
          ),
      ],
    );
  }
}
