/// 카카오 웹 로그인 facade (서버 OAuth B안 · DRAFT).
///
/// 웹에서는 `kakao_web_login_web.dart`(js_interop)가, 그 외 플랫폼에서는
/// `kakao_web_login_stub.dart`(no-op)가 사용된다. 모바일/데스크톱 빌드에는 영향이 없다.
library;

export 'kakao_web_login_stub.dart'
    if (dart.library.html) 'kakao_web_login_web.dart';
