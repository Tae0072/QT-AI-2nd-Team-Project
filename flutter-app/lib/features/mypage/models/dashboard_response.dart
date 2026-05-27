/// 마이페이지 대시보드 응답 모델.
///
/// GET /api/v1/me/dashboard 응답에 대응한다.
/// 위젯별 부분 실패를 허용하며, 실패한 위젯명은 [widgetErrors]에 담긴다.
class DashboardResponse {
  final ProfileSummary? profile;
  final StatsWidget? stats;
  final int unreadNotificationCount;
  final PraiseSummary? praiseSummary;
  final List<String> widgetErrors;

  const DashboardResponse({
    this.profile,
    this.stats,
    this.unreadNotificationCount = 0,
    this.praiseSummary,
    this.widgetErrors = const [],
  });

  factory DashboardResponse.fromJson(Map<String, dynamic> json) {
    return DashboardResponse(
      profile: json['profile'] != null
          ? ProfileSummary.fromJson(json['profile'] as Map<String, dynamic>)
          : null,
      stats: json['stats'] != null
          ? StatsWidget.fromJson(json['stats'] as Map<String, dynamic>)
          : null,
      unreadNotificationCount:
          (json['unreadNotificationCount'] as num?)?.toInt() ?? 0,
      praiseSummary: json['praiseSummary'] != null
          ? PraiseSummary.fromJson(
              json['praiseSummary'] as Map<String, dynamic>)
          : null,
      widgetErrors: (json['widgetErrors'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          [],
    );
  }
}

/// 대시보드 프로필 요약.
class ProfileSummary {
  final int memberId;
  final String nickname;

  const ProfileSummary({
    required this.memberId,
    required this.nickname,
  });

  factory ProfileSummary.fromJson(Map<String, dynamic> json) {
    return ProfileSummary(
      memberId: (json['memberId'] as num).toInt(),
      nickname: json['nickname'] as String,
    );
  }
}

/// 통계 위젯 — 주간/월간 QT 일수·노트 수, 연속 묵상 일수.
class StatsWidget {
  final WeekMonth week;
  final WeekMonth month;
  final int meditationStreakDays;

  const StatsWidget({
    required this.week,
    required this.month,
    this.meditationStreakDays = 0,
  });

  factory StatsWidget.fromJson(Map<String, dynamic> json) {
    return StatsWidget(
      week: WeekMonth.fromJson(json['week'] as Map<String, dynamic>),
      month: WeekMonth.fromJson(json['month'] as Map<String, dynamic>),
      meditationStreakDays:
          (json['meditationStreakDays'] as num?)?.toInt() ?? 0,
    );
  }
}

/// 주간/월간 통계 항목.
class WeekMonth {
  final int savedNoteCount;
  final int meditationDays;

  const WeekMonth({
    this.savedNoteCount = 0,
    this.meditationDays = 0,
  });

  factory WeekMonth.fromJson(Map<String, dynamic> json) {
    return WeekMonth(
      savedNoteCount: (json['savedNoteCount'] as num?)?.toInt() ?? 0,
      meditationDays: (json['meditationDays'] as num?)?.toInt() ?? 0,
    );
  }
}

/// 찬양 요약 — 저장된 곡 수.
class PraiseSummary {
  final int savedSongCount;

  const PraiseSummary({this.savedSongCount = 0});

  factory PraiseSummary.fromJson(Map<String, dynamic> json) {
    return PraiseSummary(
      savedSongCount: (json['savedSongCount'] as num?)?.toInt() ?? 0,
    );
  }
}
