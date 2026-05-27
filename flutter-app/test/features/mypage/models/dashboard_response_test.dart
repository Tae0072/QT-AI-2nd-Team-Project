import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/mypage/models/dashboard_response.dart';

void main() {
  group('DashboardResponse.fromJson', () {
    test('전체 필드 파싱', () {
      final json = {
        'profile': {'memberId': 1, 'nickname': '홍길동'},
        'stats': {
          'week': {'savedNoteCount': 2, 'meditationDays': 5},
          'month': {'savedNoteCount': 8, 'meditationDays': 15},
          'meditationStreakDays': 3,
        },
        'unreadNotificationCount': 10,
        'praiseSummary': {'savedSongCount': 4},
        'widgetErrors': ['calendar'],
      };

      final result = DashboardResponse.fromJson(json);

      expect(result.profile?.memberId, 1);
      expect(result.profile?.nickname, '홍길동');
      expect(result.stats?.week.savedNoteCount, 2);
      expect(result.stats?.week.meditationDays, 5);
      expect(result.stats?.month.meditationDays, 15);
      expect(result.stats?.meditationStreakDays, 3);
      expect(result.unreadNotificationCount, 10);
      expect(result.praiseSummary?.savedSongCount, 4);
      expect(result.widgetErrors, ['calendar']);
    });

    test('null 필드 안전 처리', () {
      final json = <String, dynamic>{
        'profile': null,
        'stats': null,
        'unreadNotificationCount': null,
        'praiseSummary': null,
        'widgetErrors': null,
      };

      final result = DashboardResponse.fromJson(json);

      expect(result.profile, isNull);
      expect(result.stats, isNull);
      expect(result.unreadNotificationCount, 0);
      expect(result.praiseSummary, isNull);
      expect(result.widgetErrors, isEmpty);
    });
  });

  group('MemberResponse', () {
    // MemberResponse 모델 파싱은 mypage_repository_test.dart에서 검증
  });
}
