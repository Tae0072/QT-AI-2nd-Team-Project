/// 사용자 설정 응답 모델.
class SettingsData {
  final bool notificationEnabled;
  final String fontSize;
  final bool musicEnabled;
  final int musicVolume;
  final String musicCategory;

  SettingsData({
    required this.notificationEnabled,
    required this.fontSize,
    required this.musicEnabled,
    required this.musicVolume,
    required this.musicCategory,
  });

  factory SettingsData.fromJson(Map<String, dynamic> json) {
    return SettingsData(
      notificationEnabled: json['notificationEnabled'] as bool? ?? true,
      fontSize: json['fontSize'] as String? ?? 'MEDIUM',
      musicEnabled: json['musicEnabled'] as bool? ?? true,
      musicVolume: (json['musicVolume'] as num?)?.toInt() ?? 70,
      musicCategory: json['musicCategory'] as String? ?? 'BGM',
    );
  }
}
