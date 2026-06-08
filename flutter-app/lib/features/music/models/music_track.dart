/// 배경음악 음원 메타데이터 모델 (GET /api/v1/music/tracks 응답 항목).
///
/// 음원 바이트는 포함하지 않는다 — 재생은 스트리밍 URL로 한다.
class MusicTrack {
  final int id;
  final String title;
  final String category; // BGM | HYMN
  final String mimeType;
  final int? durationSec;
  final int sortOrder;

  MusicTrack({
    required this.id,
    required this.title,
    required this.category,
    required this.mimeType,
    this.durationSec,
    required this.sortOrder,
  });

  factory MusicTrack.fromJson(Map<String, dynamic> json) {
    return MusicTrack(
      id: (json['id'] as num).toInt(),
      title: json['title'] as String? ?? '',
      category: json['category'] as String? ?? 'BGM',
      mimeType: json['mimeType'] as String? ?? 'audio/mpeg',
      durationSec: (json['durationSec'] as num?)?.toInt(),
      sortOrder: (json['sortOrder'] as num?)?.toInt() ?? 0,
    );
  }
}
