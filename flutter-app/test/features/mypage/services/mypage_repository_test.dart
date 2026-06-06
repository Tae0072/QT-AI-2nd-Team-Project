import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:qtai_app/features/mypage/services/mypage_repository.dart';

void main() {
  late Dio dio;
  late DioAdapter dioAdapter;
  late MyPageRepository repository;

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'http://localhost/api/v1'));
    dioAdapter = DioAdapter(dio: dio);
    repository = MyPageRepository(dio);
  });

  group('getDashboard', () {
    test('정상 응답 파싱', () async {
      dioAdapter.onGet('/me/dashboard', (server) {
        server.reply(200, {
          'success': true,
          'data': {
            'profile': {'memberId': 1, 'nickname': '테스트유저'},
            'stats': {
              'week': {'savedNoteCount': 0, 'meditationDays': 3},
              'month': {'savedNoteCount': 0, 'meditationDays': 10},
              'meditationStreakDays': 5,
            },
            'unreadNotificationCount': 3,
            'praiseSummary': {'savedSongCount': 7},
            'widgetErrors': [],
          },
        });
      });

      final result = await repository.getDashboard();

      expect(result.profile?.memberId, 1);
      expect(result.profile?.nickname, '테스트유저');
      expect(result.stats?.week.meditationDays, 3);
      expect(result.stats?.meditationStreakDays, 5);
      expect(result.unreadNotificationCount, 3);
      expect(result.praiseSummary?.savedSongCount, 7);
      expect(result.widgetErrors, isEmpty);
    });

    test('위젯 에러 포함 응답 파싱', () async {
      dioAdapter.onGet('/me/dashboard', (server) {
        server.reply(200, {
          'success': true,
          'data': {
            'profile': null,
            'stats': null,
            'unreadNotificationCount': 0,
            'praiseSummary': null,
            'widgetErrors': ['profile', 'stats'],
          },
        });
      });

      final result = await repository.getDashboard();

      expect(result.profile, isNull);
      expect(result.stats, isNull);
      expect(result.widgetErrors, ['profile', 'stats']);
    });
  });

  group('getProfile', () {
    test('정상 응답 파싱', () async {
      dioAdapter.onGet('/me', (server) {
        server.reply(200, {
          'success': true,
          'data': {
            'id': 1,
            'nickname': '테스트유저',
            'email': 'test@example.com',
            'profileImageUrl': null,
            'status': 'ACTIVE',
            'role': 'USER',
            'nicknameUnlockAt': '2999-12-31T12:00:00',
            'createdAt': '2026-05-01T10:00:00',
          },
        });
      });

      final result = await repository.getProfile();

      expect(result.id, 1);
      expect(result.nickname, '테스트유저');
      expect(result.email, 'test@example.com');
      expect(result.status, 'ACTIVE');
      expect(result.isNicknameChangeable, isFalse); // 미래 날짜
    });

    test('nicknameUnlockAt null이면 변경 가능', () async {
      dioAdapter.onGet('/me', (server) {
        server.reply(200, {
          'success': true,
          'data': {
            'id': 2,
            'nickname': 'user_99999',
            'email': null,
            'profileImageUrl': null,
            'status': 'ACTIVE',
            'role': 'USER',
            'nicknameUnlockAt': null,
            'createdAt': '2026-05-01T10:00:00',
          },
        });
      });

      final result = await repository.getProfile();

      expect(result.isNicknameChangeable, isTrue);
    });
  });

  group('changeNickname', () {
    test('정상 변경', () async {
      dioAdapter.onPatch('/me/nickname', (server) {
        server.reply(200, {
          'success': true,
          'data': {
            'id': 1,
            'nickname': '새닉네임',
            'email': null,
            'profileImageUrl': null,
            'status': 'ACTIVE',
            'role': 'USER',
            'nicknameUnlockAt': '2026-06-03T12:00:00',
            'createdAt': '2026-05-01T10:00:00',
          },
        });
      }, data: {'nickname': '새닉네임'});

      final result = await repository.changeNickname('새닉네임');

      expect(result.nickname, '새닉네임');
    });
  });

  group('checkNicknameAvailable', () {
    test('사용 가능 닉네임 → true', () async {
      dioAdapter.onGet(
        '/me/nickname/available',
        (server) {
          server.reply(200, {'success': true, 'data': true});
        },
        queryParameters: {'nickname': '새닉네임'},
      );

      final result = await repository.checkNicknameAvailable('새닉네임');

      expect(result, isTrue);
    });

    test('중복 닉네임 → false', () async {
      dioAdapter.onGet(
        '/me/nickname/available',
        (server) {
          server.reply(200, {'success': true, 'data': false});
        },
        queryParameters: {'nickname': '기존닉네임'},
      );

      final result = await repository.checkNicknameAvailable('기존닉네임');

      expect(result, isFalse);
    });
  });

  group('withdraw', () {
    test('정상 탈퇴 (사유 없음)', () async {
      dioAdapter.onDelete('/me', (server) {
        server.reply(204, null);
      });

      await expectLater(repository.withdraw(), completes);
    });
  });
}
