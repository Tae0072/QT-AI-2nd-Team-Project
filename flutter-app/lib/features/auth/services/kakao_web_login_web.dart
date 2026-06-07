import 'dart:js_interop';

/// 웹 전용 카카오 로그인 (서버 OAuth B안 · DRAFT).
///
/// 전제: web/index.html에 카카오 JavaScript SDK(kakao.min.js)가 로드돼 있어야 하고,
/// Kakao 개발자 콘솔에 JavaScript 키 + Web 플랫폼/Redirect URI가 등록돼 있어야 한다.
///
/// 흐름: [redirectToKakaoLogin]로 카카오 인가 페이지로 이동 → 복귀 시 URL의 `code`를
/// [readKakaoAuthCode]로 읽어 서버(POST /api/v1/auth/kakao/web)로 전달 → JWT 발급.

@JS('Kakao.isInitialized')
external bool _kakaoIsInitialized();

@JS('Kakao.init')
external void _kakaoInit(String appKey);

@JS('Kakao.Auth.authorize')
external void _kakaoAuthorize(_AuthorizeParams params);

extension type _AuthorizeParams._(JSObject _) implements JSObject {
  external factory _AuthorizeParams({String redirectUri});
}

/// 카카오 SDK 초기화(중복 호출 안전).
void ensureKakaoInit(String jsKey) {
  if (!_kakaoIsInitialized()) {
    _kakaoInit(jsKey);
  }
}

/// 카카오 인가 페이지로 리다이렉트(authorization code 흐름).
void redirectToKakaoLogin({required String jsKey, required String redirectUri}) {
  ensureKakaoInit(jsKey);
  _kakaoAuthorize(_AuthorizeParams(redirectUri: redirectUri));
}

/// 리다이렉트 복귀 시 현재 URL 쿼리에서 인가 코드를 읽는다(없으면 null).
String? readKakaoAuthCode() => Uri.base.queryParameters['code'];
