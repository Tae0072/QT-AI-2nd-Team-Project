/// 사용자 설정 응답 모델.
class SettingsData {
  final bool notificationEnabled;
  final String fontSize;

  SettingsData({
    required this.notificationEnabled,
    required this.fontSize,
  });

  factory SettingsData.fromJson(Map<String, dynamic> json) {
    return SettingsData(
      notificationEnabled: json['notificationEnabled'] as bool? ?? true,
      fontSize: json['fontSize'] as String? ?? 'MEDIUM',
    );
  }
}
