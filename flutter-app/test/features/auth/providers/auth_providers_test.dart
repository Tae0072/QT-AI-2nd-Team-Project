import 'package:flutter_test/flutter_test.dart';

import 'package:qtai_app/features/auth/providers/auth_providers.dart';
import 'package:qtai_app/features/auth/services/auth_repository.dart';

void main() {
  group('AuthStatus enum', () {
    test('3가지 상태가 정의되어 있다', () {
      expect(AuthStatus.values.length, 3);
      expect(AuthStatus.values, contains(AuthStatus.unknown));
      expect(AuthStatus.values, contains(AuthStatus.authenticated));
      expect(AuthStatus.values, contains(AuthStatus.unauthenticated));
    });
  });

  group('LoginResult', () {
    test('필수 필드가 정상 생성된다', () {
      const result = LoginResult(
        accessToken: 'access-123',
        refreshToken: 'refresh-456',
        isNewMember: true,
      );

      expect(result.accessToken, 'access-123');
      expect(result.refreshToken, 'refresh-456');
      expect(result.isNewMember, isTrue);
    });

    test('isNewMember false로 생성된다', () {
      const result = LoginResult(
        accessToken: 'a',
        refreshToken: 'r',
        isNewMember: false,
      );

      expect(result.isNewMember, isFalse);
    });
  });
}
