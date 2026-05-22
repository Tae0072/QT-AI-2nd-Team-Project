import 'dart:developer';

/// 실행 환경 enum.
enum Environment { dev, staging, prod }

/// 환경별 설정.
///
/// [initialize]는 앱 시작 시 한 번만 실행되어야 한다.
/// 이중 실행 시 [StateError]를 던져 구성 오염을 방지한다.
class AppConfig {
  final Environment environment;
  final String baseUrl;
  final String kakaoNativeAppKey;

  const AppConfig._({
    required this.environment,
    required this.baseUrl,
    required this.kakaoNativeAppKey,
  });

  static AppConfig? _instance;
  static AppConfig get instance {
    if (_instance == null) {
      throw StateError('AppConfig.initialize()가 실행되지 않았습니다.');
    }
    return _instance!;
  }

  /// 앱 설정 초기화. 이중 실행 시 [StateError] 발생.
  static void initialize() {
    if (_instance != null) {
      throw StateError('AppConfig.initialize()가 이미 실행되었습니다.');
    }

    const envName = String.fromEnvironment('ENV', defaultValue: 'dev');
    const kakaoKey =
        String.fromEnvironment('KAKAO_NATIVE_APP_KEY', defaultValue: '');

    final env = Environment.values.firstWhere(
      (e) => e.name == envName,
      orElse: () => Environment.dev,
    );

    // prod/staging에서 카카오 키 미입력 시 빠른 실패
    if (env != Environment.dev && kakaoKey.isEmpty) {
      throw StateError(
        'KAKAO_NATIVE_APP_KEY가 설정되지 않았습니다. '
        '--dart-define=KAKAO_NATIVE_APP_KEY=... 로 주입하세요.',
      );
    }

    // dev 환경에서는 KAKAO 키가 비어있을 수 있음:
    // Kakao SDK 초기화를 건너뛰고 네트워크/UI 개발에 집중하기 위한 의도적 허용.
    // 빈 키 상태에서 Kakao 로그인 시도 시 AuthService에서 명시적 에러를 반환한다.
    if (env == Environment.dev && kakaoKey.isEmpty) {
      log('WARNING: KAKAO_NATIVE_APP_KEY가 비어있습니다. '
          'Kakao 로그인 기능은 동작하지 않습니다.',
          name: 'AppConfig');
    }

    _instance = AppConfig._(
      environment: env,
      baseUrl: _baseUrlFor(env),
      kakaoNativeAppKey: kakaoKey,
    );
  }

  /// 테스트용 초기화. 기존 인스턴스를 교체한다.
  static void initializeForTest({
    Environment environment = Environment.dev,
    String baseUrl = 'http://localhost:8080/api/v1',
    String kakaoNativeAppKey = 'test-key',
  }) {
    _instance = AppConfig._(
      environment: environment,
      baseUrl: baseUrl,
      kakaoNativeAppKey: kakaoNativeAppKey,
    );
  }

  /// 테스트 후 인스턴스 리셋.
  static void reset() => _instance = null;

  static String _baseUrlFor(Environment env) {
    switch (env) {
      case Environment.dev:
        return 'http://10.0.2.2:8080/api/v1';
      case Environment.staging:
        return 'https://staging-api.qtai.com/api/v1';
      case Environment.prod:
        return 'https://api.qtai.com/api/v1';
    }
  }

  bool get isDev => environment == Environment.dev;
  bool get isProd => environment == Environment.prod;
}