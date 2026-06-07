/// 비웹(모바일/데스크톱)용 no-op 스텁.
///
/// 웹 전용 카카오 로그인은 이 플랫폼에서 동작하지 않는다(네이티브는 카카오 SDK 사용).

// ignore: avoid_unused_constructor_parameters
void ensureKakaoInit(String jsKey) {}

void redirectToKakaoLogin({required String jsKey, required String redirectUri}) {}

String? readKakaoAuthCode() => null;
