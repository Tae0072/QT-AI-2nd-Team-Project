enum Environment { dev, staging, prod }

class AppConfig {
  final Environment environment;
  final String baseUrl;
  final String kakaoNativeAppKey;

  const AppConfig._({
    required this.environment,
    required this.baseUrl,
    required this.kakaoNativeAppKey,
  });

  static late final AppConfig instance;

  static void initialize() {
    const envName = String.fromEnvironment('ENV', defaultValue: 'dev');
    const kakaoKey = String.fromEnvironment('KAKAO_NATIVE_APP_KEY', defaultValue: '');

    final env = Environment.values.firstWhere(
      (e) => e.name == envName,
      orElse: () => Environment.dev,
    );

    instance = AppConfig._(
      environment: env,
      baseUrl: _baseUrlFor(env),
      kakaoNativeAppKey: kakaoKey,
    );
  }

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