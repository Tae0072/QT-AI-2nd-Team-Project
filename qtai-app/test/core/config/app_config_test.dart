import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/core/config/app_config.dart';

void main() {
  setUp(() => AppConfig.reset());
  tearDown(() => AppConfig.reset());

  group('AppConfig', () {
    test('initializeForTest로 dev 환경 설정', () {
      AppConfig.initializeForTest();
      expect(AppConfig.instance.isDev, isTrue);
      expect(AppConfig.instance.isProd, isFalse);
      expect(AppConfig.instance.baseUrl, contains('localhost'));
    });

    test('initializeForTest로 prod 환경 설정', () {
      AppConfig.initializeForTest(
        environment: Environment.prod,
        baseUrl: 'https://api.qtai.com/api/v1',
      );
      expect(AppConfig.instance.isProd, isTrue);
      expect(AppConfig.instance.isDev, isFalse);
    });

    test('initialize() 전 instance 접근 시 StateError', () {
      expect(() => AppConfig.instance, throwsStateError);
    });

    test('이중 initialize() 호출 시 StateError', () {
      AppConfig.initializeForTest();
      expect(
        () => AppConfig.initialize(),
        throwsStateError,
      );
    });

    test('reset() 후 다시 초기화 가능', () {
      AppConfig.initializeForTest();
      expect(AppConfig.instance.isDev, isTrue);

      AppConfig.reset();
      AppConfig.initializeForTest(
        environment: Environment.staging,
        baseUrl: 'https://staging-api.qtai.com/api/v1',
      );
      expect(AppConfig.instance.environment, Environment.staging);
    });
  });
}