import 'dart:developer';
import 'dart:io' show Platform;

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
  final String ttsBaseUrl;

  const AppConfig._({
    required this.environment,
    required this.baseUrl,
    required this.kakaoNativeAppKey,
    required this.ttsBaseUrl,
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
    // dev 기본값: 네이티브 앱 키는 APK에 포함되는 공개 키로 보안 리스크 없음
    const kakaoKey = String.fromEnvironment('KAKAO_NATIVE_APP_KEY',
        defaultValue: '53e5afb2d90048af9e71332e47f387fa');

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
      ttsBaseUrl: _ttsBaseUrlFor(env),
    );
  }

  /// 테스트용 초기화. 기존 인스턴스를 교체한다.
  static void initializeForTest({
    Environment environment = Environment.dev,
    String baseUrl = 'http://localhost:8080/api/v1',
    String kakaoNativeAppKey = 'test-key',
    String ttsBaseUrl = 'http://localhost:8090',
  }) {
    _instance = AppConfig._(
      environment: environment,
      baseUrl: baseUrl,
      kakaoNativeAppKey: kakaoNativeAppKey,
      ttsBaseUrl: ttsBaseUrl,
    );
  }

  /// 테스트 후 인스턴스 리셋.
  static void reset() => _instance = null;

  /// dev 환경 baseUrl 결정.
  /// - `--dart-define=DEV_BASE_URL=...` 으로 override 가능.
  /// - 미지정 시 Android 에뮬레이터(`10.0.2.2`), iOS 시뮬레이터(`localhost`) 자동 분기.
  static String _baseUrlFor(Environment env) {
    switch (env) {
      case Environment.dev:
        const override = String.fromEnvironment('DEV_BASE_URL', defaultValue: '');
        if (override.isNotEmpty) return override;
        // Android 에뮬레이터: 10.0.2.2 → 호스트 localhost 매핑
        // iOS 시뮬레이터: 직접 localhost 사용 가능
        final host = Platform.isIOS ? 'localhost' : '10.0.2.2';
        return 'http://$host:8080/api/v1';
      case Environment.staging:
        return 'https://staging-api.qtai.com/api/v1';
      case Environment.prod:
        return 'https://api.qtai.com/api/v1';
    }
  }

  /// TTS 서버 URL 결정.
  /// `--dart-define=TTS_BASE_URL=...` 으로 override 가능.
  static String _ttsBaseUrlFor(Environment env) {
    const override = String.fromEnvironment('TTS_BASE_URL', defaultValue: '');
    if (override.isNotEmpty) return override;
    switch (env) {
      case Environment.dev:
        final host = Platform.isIOS ? 'localhost' : '10.0.2.2';
        return 'http://$host:8090';
      case Environment.staging:
        return 'https://tts.qtai.com';
      case Environment.prod:
        return 'https://tts.qtai.com';
    }
  }

  bool get isDev => environment == Environment.dev;
  bool get isProd => environment == Environment.prod;
}
