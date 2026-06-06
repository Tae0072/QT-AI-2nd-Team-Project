/// 큐레이션 찬양곡 모델.
class PraiseSong {
  final int id;
  final String title;
  final String artist;
  final String sourceType;
  final String status;

  PraiseSong({
    required this.id,
    required this.title,
    required this.artist,
    required this.sourceType,
    required this.status,
  });

  factory PraiseSong.fromJson(Map<String, dynamic> json) {
    return PraiseSong(
      id: json['id'] as int,
      title: json['title'] as String? ?? '',
      artist: json['artist'] as String? ?? '',
      sourceType: json['sourceType'] as String? ?? '',
      status: json['status'] as String? ?? '',
    );
  }
}

/// 내 찬양곡 모델.
class MyPraiseSong {
  final int id;
  final int? praiseSongId;
  final String displayTitle;
  final String title;
  final String artist;
  final String sourceType;

  MyPraiseSong({
    required this.id,
    this.praiseSongId,
    required this.displayTitle,
    required this.title,
    required this.artist,
    required this.sourceType,
  });

  factory MyPraiseSong.fromJson(Map<String, dynamic> json) {
    return MyPraiseSong(
      id: json['id'] as int,
      praiseSongId: json['praiseSongId'] as int?,
      displayTitle: json['displayTitle'] as String? ?? '',
      title: json['title'] as String? ?? '',
      artist: json['artist'] as String? ?? '',
      sourceType: json['sourceType'] as String? ?? '',
    );
  }
}
