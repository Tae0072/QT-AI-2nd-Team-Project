import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:qtai_app/core/storage/secure_storage.dart';
import 'package:qtai_app/features/auth/services/auth_repository.dart';
import 'package:qtai_app/features/auth/services/kakao_auth_client.dart';

/// FlutterSecureStorage method channel mock — in-memory 저장소로 대체.
Map<String, String> setupSecureStorageMock() {
  final Map<String, String> store = {};
  const channel = MethodChannel('plugins.it_nomads.com/flutter_secure_storage');

  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
    switch (methodCall.method) {
      case 'read':
        return store[methodCall.arguments['key'] as String];
      case 'write':
        store[methodCall.arguments['key'] as String] =
            methodCall.arguments['value'] as String;
        return null;
      case 'delete':
        store.remove(methodCall.arguments['key'] as String);
        return null;
      case 'deleteAll':
        store.clear();
        return null;
      default:
        return null;
    }
  });
  return store;
}

/// 카카오 SDK 가짜 구현 — 호출 기록 + 실패 주입.
class FakeKakaoAuthClient implements KakaoAuthClient {
  final List<String> calls = [];
  List<Prompt>? capturedPrompts;
  bool kakaoTalkInstalled = false;
  bool throwOnUnlink = false;
  bool throwOnLogout = false;

  /// 설정 시 [loginWithKakaoTalk] 가 이 예외를 던진다(폴백 테스트용).
  Object? throwOnTalkLogin;

  @override
  Future<bool> isKakaoTalkAvailable() async => kakaoTalkInstalled;

  @override
  Future<String> loginWithKakaoTalk() async {
    calls.add('loginWithKakaoTalk');
    if (throwOnTalkLogin != null) throw throwOnTalkLogin!;
    return 'kakao-token-talk';
  }

  @override
  Future<String> loginWithKakaoAccount({List<Prompt>? prompts}) async {
    calls.add('loginWithKakaoAccount');
    capturedPrompts = prompts;
    return 'kakao-token-account';
  }

  @override
  Future<void> logout() async {
    if (throwOnLogout) throw Exception('kakao logout failed');
    calls.add('logout');
  }

  @override
  Future<void> unlink() async {
    if (throwOnUnlink) throw Exception('kakao unlink failed');
    calls.add('unlink');
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late Map<String, String> store;
  late Dio dio;
  late DioAdapter dioAdapter;
  late FakeKakaoAuthClient kakao;
  late AuthRepository repository;

  Map<String, dynamic> loginResponse({bool onboardingRequired = false}) => {
        'success': true,
        'data': {
          'accessToken': 'server-access',
          'refreshToken': 'server-refresh',
          'member': {
            'id': 1,
            'nickname': 'tester',
            'role': 'USER',
            'status': 'ACTIVE',
            'onboardingRequired': onboardingRequired,
          },
        },
      };

  setUp(() {
    store = setupSecureStorageMock();
    dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));
    dioAdapter = DioAdapter(dio: dio);
    kakao = FakeKakaoAuthClient();
    repository = AuthRepository(dio: dio, kakaoAuthClient: kakao);
  });

  group('loginWithKakao', () {
    test('일반 경로 — 카카오톡 미설치 시 계정 로그인(prompts 없음) 후 토큰 저장', () async {
      dioAdapter.onPost('/auth/kakao', (server) => server.reply(200, loginResponse()),
          data: Matchers.any);

      final result = await repository.loginWithKakao();

      expect(kakao.calls, contains('loginWithKakaoAccount'));
      expect(kakao.capturedPrompts, isNull); // 재인증 강제 아님
      expect(result.accessToken, 'server-access');
      expect(store['access_token'], 'server-access');
      expect(store['refresh_token'], 'server-refresh');
      expect(result.isNewMember, isFalse);
    });

    test('탈퇴 후 재인증 플래그 시 Prompt.login 강제 + 성공 시 플래그 해제(1회성)', () async {
      await SecureStorage.setForceKakaoRelogin();
      dioAdapter.onPost('/auth/kakao', (server) => server.reply(200, loginResponse()),
          data: Matchers.any);

      await repository.loginWithKakao();

      // 카카오톡 설치 여부와 무관하게 계정 재인증 경로 + Prompt.login
      expect(kakao.calls, contains('loginWithKakaoAccount'));
      expect(kakao.capturedPrompts, [Prompt.login]);
      // 1회성 — 성공 후 플래그 해제
      expect(await SecureStorage.getForceKakaoRelogin(), isFalse);
    });

    test('플래그 없으면 카카오톡 설치 시 카카오톡 로그인 경로', () async {
      kakao.kakaoTalkInstalled = true;
      dioAdapter.onPost('/auth/kakao', (server) => server.reply(200, loginResponse()),
          data: Matchers.any);

      await repository.loginWithKakao();

      expect(kakao.calls, contains('loginWithKakaoTalk'));
      expect(kakao.calls, isNot(contains('loginWithKakaoAccount')));
    });

    test('카카오톡 로그인이 실패하면 웹(계정) 로그인으로 폴백한다', () async {
      kakao.kakaoTalkInstalled = true;
      // 키 해시 미등록 등으로 카카오톡 로그인이 즉시 실패하는 상황을 모사.
      kakao.throwOnTalkLogin = PlatformException(code: 'KAKAO_LOGIN_FAILED');
      dioAdapter.onPost('/auth/kakao', (server) => server.reply(200, loginResponse()),
          data: Matchers.any);

      final result = await repository.loginWithKakao();

      // 톡 시도 후 계정(웹) 로그인으로 폴백 → 최종 성공.
      expect(kakao.calls, ['loginWithKakaoTalk', 'loginWithKakaoAccount']);
      expect(result.accessToken, 'server-access');
    });

    test('사용자가 카카오톡 화면에서 취소(CANCELED)하면 폴백하지 않고 중단한다', () async {
      kakao.kakaoTalkInstalled = true;
      kakao.throwOnTalkLogin = PlatformException(code: 'CANCELED');

      await expectLater(repository.loginWithKakao(), throwsA(isA<PlatformException>()));

      // 웹 로그인으로 폴백하지 않는다(사용자의 명시적 취소).
      expect(kakao.calls, isNot(contains('loginWithKakaoAccount')));
    });
  });

  group('logout', () {
    test('서버 폐기를 로컬 토큰 삭제보다 먼저 호출한다 (인터셉터가 토큰을 붙일 수 있도록)', () async {
      await SecureStorage.setAccessToken('server-access');
      await SecureStorage.setRefreshToken('server-refresh');

      // 서버 호출 시점에 access token이 아직 존재하는지 기록
      String? tokenAtServerCall;
      dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) async {
        tokenAtServerCall = await SecureStorage.getAccessToken();
        handler.next(options);
      }));
      dioAdapter.onPost('/auth/logout', (server) => server.reply(200, {'success': true}));

      await repository.logout();

      expect(tokenAtServerCall, 'server-access'); // 삭제 전에 서버 호출됨
      expect(store['access_token'], isNull); // 종료 시점엔 삭제 보장
      expect(store['refresh_token'], isNull);
      expect(kakao.calls, contains('logout'));
    });

    test('서버/카카오 호출이 실패해도 로컬 토큰은 반드시 삭제된다', () async {
      await SecureStorage.setAccessToken('server-access');
      kakao.throwOnLogout = true;
      dioAdapter.onPost('/auth/logout',
          (server) => server.throws(500, DioException(requestOptions: RequestOptions())));

      await repository.logout();

      expect(store['access_token'], isNull);
      expect(store['refresh_token'], isNull);
    });
  });

  group('cleanupAfterWithdraw', () {
    test('카카오 unlink + 토큰 삭제 + 재인증 강제 플래그 저장', () async {
      await SecureStorage.setAccessToken('server-access');
      await SecureStorage.setRefreshToken('server-refresh');

      await repository.cleanupAfterWithdraw();

      expect(kakao.calls, contains('unlink'));
      expect(store['access_token'], isNull);
      expect(store['refresh_token'], isNull);
      expect(await SecureStorage.getForceKakaoRelogin(), isTrue);
    });

    test('unlink 실패해도 토큰 삭제와 플래그 저장은 보장된다 (finally)', () async {
      await SecureStorage.setAccessToken('server-access');
      kakao.throwOnUnlink = true;

      await repository.cleanupAfterWithdraw();

      expect(store['access_token'], isNull);
      expect(await SecureStorage.getForceKakaoRelogin(), isTrue);
    });
  });
}
