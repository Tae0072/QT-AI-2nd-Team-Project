import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/auth/providers/auth_providers.dart';
import 'package:qtai_app/main.dart';

void main() {
  test('devForceHome이 켜진 dev 환경에서는 인증 전에도 home으로 진입한다', () {
    final route = resolveInitialRoute(
      onboardingComplete: false,
      authStatus: AuthStatus.unknown,
      isDev: true,
      devForceHome: true,
    );

    expect(route, '/home');
  });
}
