import 'dart:developer';
import 'package:flutter/foundation.dart'
    show kIsWeb, defaultTargetPlatform, TargetPlatform;

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
    // 키 기본값 없음(CLAUDE.md §8 — plain 키 커밋 금지, 코드리뷰 TODO 4).
    // 실행 시 --dart-define=KAKAO_NATIVE_APP_KEY=... 로 주입한다(README 참고).
    // dev에서 빈 키면 아래에서 경고 후 카카오 로그인만 비활성 — 나머지 개발은 가능.
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
      ttsBaseUrl: _ttsBaseUrlFor(env),
    );
  }

  /// 테스트용 초기화. 기존 인스턴스를 교체한다.
  static void initializeForTest({
    Environment environment = Environment.dev,
    String baseUrl = 'http://localhost:8080/api/v1',
    String kakaoNativeAppKey = 'test-key',
    String ttsBaseUrl = 'http://localhost:8091',
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
  /// - 미지정 시 웹/iOS는 `localhost`, Android 에뮬레이터는 `10.0.2.2`로 자동 분기.
  static String _baseUrlFor(Environment env) {
    switch (env) {
      case Environment.dev:
        const override = String.fromEnvironment('DEV_BASE_URL', defaultValue: '');
        if (override.isNotEmpty) return override;
        // 웹/iOS는 호스트의 localhost에 직접 접근,
        // Android 에뮬레이터만 10.0.2.2로 호스트 localhost에 매핑된다.
        final host = _devHost();
        return 'http://$host:8080/api/v1';
      case Environment.staging:
        return 'https://staging-api.qtai.com/api/v1';
      case Environment.prod:
        return 'https://api.qtai.com/api/v1';
    }
  }

  /// 배포된 TTS 서버(Render). 무료 인스턴스라 미사용 시 잠들어 첫 요청은 콜드스타트로
  /// 수십 초 지연될 수 있다. 로컬 TTS(8091)로 쓰려면 아래 override를 사용한다.
  static const String _hostedTtsUrl = 'https://qt-ai-2nd-team-project.onrender.com';

  /// TTS 서버 URL 결정.
  /// `--dart-define=TTS_BASE_URL=...` 으로 override 가능(예: 로컬 `http://10.0.2.2:8091`).
  ///
  /// 기본값은 배포된 Render TTS 서버 — 로컬 TTS 서버를 띄우지 않아도 기기에서 바로 동작한다.
  static String _ttsBaseUrlFor(Environment env) {
    const override = String.fromEnvironment('TTS_BASE_URL', defaultValue: '');
    if (override.isNotEmpty) return override;
    return _hostedTtsUrl;
  }

  /// dev 환경에서 접근할 서버 호스트를 결정한다.
  ///
  /// - 웹(브라우저)·iOS 시뮬레이터: 호스트의 `localhost`에 직접 접근.
  /// - Android 에뮬레이터: 호스트 `localhost`가 `10.0.2.2`로 매핑되므로 그 주소 사용.
  static String _devHost() {
    if (kIsWeb || defaultTargetPlatform == TargetPlatform.iOS) {
      return 'localhost';
    }
    return '10.0.2.2';
  }

  bool get isDev => environment == Environment.dev;
  bool get isProd => environment == Environment.prod;
}
