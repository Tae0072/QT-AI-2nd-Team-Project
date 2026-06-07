// ⚠️ [WEB_DEV_ACCESS] 개발 편의용 임시 장치 — 웹에서만 로그인 없이 앱에 진입한다.
//
// 목적: 카카오 로그인이 웹 미지원이라, 웹 개발 중 로그인 화면을 건너뛰고
//       앱 화면으로 바로 들어가기 위함.
//
// 안전장치(삼중 게이트): 웹(kIsWeb) + dev 빌드(AppConfig.isDev) +
//       실행 플래그(--dart-define=WEB_DEV_NO_LOGIN=true)가 모두 참일 때만 동작한다.
//       모바일·prod·release 빌드에서는 항상 false라 영향이 없다.
//
// 제거 방법(개발 종료 시):
//   1) 이 파일(web_dev_access.dart)을 삭제한다.
//   2) main.dart에서 "[WEB_DEV_ACCESS]" 주석이 달린 import 1줄과 호출부 2줄을 제거한다.
//
// 주의: 이 우회는 "화면 진입"만 시켜준다. 서버 콘텐츠 API는 인증 토큰이
//       필요하므로, 토큰 없이는 데이터가 비거나 401이 난다(로그인 화면으로
//       튕기지는 않는다). 실제 데이터까지 필요하면 서버 dev-login이 별도로 필요하다.

import 'package:flutter/foundation.dart' show kIsWeb;

import '../config/app_config.dart';

/// 실행 플래그. 기본 false. 켜려면 `--dart-define=WEB_DEV_NO_LOGIN=true`.
const bool _webDevNoLoginFlag =
    bool.fromEnvironment('WEB_DEV_NO_LOGIN', defaultValue: false);

/// 웹 + dev + 플래그가 모두 만족될 때만 true. (모바일/prod에서는 항상 false)
///
/// `&&` 단락 평가로, 플래그가 꺼진 일반 빌드에서는 `AppConfig.instance`를
/// 건드리지 않는다.
bool get webDevNoLogin =>
    kIsWeb && _webDevNoLoginFlag && AppConfig.instance.isDev;

/// dev 서버 인증에 사용할 memberId. 기본 '1'. `--dart-define=WEB_DEV_USER_ID=2`로 변경.
///
/// 서버 `dev` 프로파일 + `qtai.security.dev-bypass=true`에서 `X-Dev-User-Id`
/// 헤더로 전송되어 해당 memberId로 인증된다. (그 memberId의 회원이 DB에 있어야 함)
const String webDevUserId =
    String.fromEnvironment('WEB_DEV_USER_ID', defaultValue: '1');
