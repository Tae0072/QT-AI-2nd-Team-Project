import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qtai_app/core/config/app_config.dart';
import 'package:qtai_app/core/network/api_client.dart';

void main() {
  tearDown(() {
    AppConfig.reset();
  });

  test('dev 환경에서는 서버 dev-bypass용 X-Dev-User-Id 헤더를 붙인다', () {
    AppConfig.initializeForTest(environment: Environment.dev);
    final container = ProviderContainer();
    addTearDown(container.dispose);

    final dio = container.read(dioProvider);

    expect(dio.options.headers['X-Dev-User-Id'], '1');
  });

  test('prod 환경에서는 X-Dev-User-Id 헤더를 붙이지 않는다', () {
    AppConfig.initializeForTest(environment: Environment.prod);
    final container = ProviderContainer();
    addTearDown(container.dispose);

    final dio = container.read(dioProvider);

    expect(dio.options.headers.containsKey('X-Dev-User-Id'), isFalse);
  });
}
