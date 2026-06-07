# Flutter 브라우저(웹) 실행 지원 작업 워크플로우

> 2026-06-07 · 브랜치 `feat/flutter-web-run-support` (base `dev`) · 리포트: `reports/2026-06-07_flutter-web-run-support_report.md`

## 요청

에뮬레이터 대신 크롬/엣지/웨일 등 브라우저로 Flutter 앱을 실행하고 싶다. 코드를 고쳐야 하나?

## 진행 절차

1. **현황 파악**: 실행 명령은 `flutter run -d chrome`로 가능하나, 웹은 에뮬레이터와 달라 3가지 손질이 필요함을 확인 — ① `dart:io` 미지원, ② `10.0.2.2` 대신 `localhost`, ③ CORS.
2. **코드 진단(grep)**: `dart:io` import 파일 3개(`app_config`, `tts_repository`, `note_share_sheet`)와 서버 CORS 기본값(`http://localhost:3000`) 확인.
3. **브랜치 생성**: `dev`에서 `feat/flutter-web-run-support`.
4. **헬퍼 추가**: 조건부 import 파사드 `core/platform/file_storage.dart`(+`_io`/`_stub`)로 `dart:io`를 한 곳에 격리.
5. **3개 파일 수정**: `dart:io` 직접 의존 제거, `kIsWeb`/`defaultTargetPlatform`로 분기. 기기 동작은 그대로 유지.
6. **테스트 보강**: `app_config_test`에 Android/iOS 호스트 분기 단위 테스트 추가.
7. **검토(2~3회)**: grep 재점검 → 호출부 try/catch 확인 → 서브에이전트 리뷰 → Flutter 원본으로 `kIsWeb` 노출 여부 확정 → `note_share` import 1건 수정.
8. **마감**: 본 리포트/워크플로우 작성. 커밋·PR은 Windows에서 T가 진행.

## 변경 파일

- 신규: `lib/core/platform/file_storage.dart`, `file_storage_io.dart`, `file_storage_stub.dart`
- 수정: `lib/core/config/app_config.dart`, `lib/features/tts/services/tts_repository.dart`, `lib/features/tts/widgets/qt_tts_button.dart`, `lib/features/note/widgets/note_share_sheet.dart`, `test/core/config/app_config_test.dart`
- TTS 웹: 음성을 bytes→data URI로 재생(`setUrl`). TTS 서버(8090) CORS 허용 필요.
- 웹 개발용 로그인 우회(임시): `lib/core/dev/web_dev_access.dart` + `main.dart`(`[WEB_DEV_ACCESS]`). 삼중 게이트(kIsWeb+isDev+`WEB_DEV_NO_LOGIN`), 별도 커밋 권장, 추후 삭제. 화면 진입만(데이터는 토큰 필요).
- TTS 서버 CORS: mount된 `D:\ai엔진\tts 프로그램`엔 venv만 있어 서버 소스 미발견 → 위치 확인 후 반영(스니펫은 리포트 §10).
- 실데이터용 dev 인증: 새 엔드포인트 대신 **기존 `X-Dev-User-Id` 우회 재사용**. 서버 `DevSecurityConfig`에 웹 CORS만 추가, Flutter는 웹우회 시 헤더 주입. 서버 `dev` 프로파일로 실행(리포트 §11).
- **함정(JWT 키 — 2단):** ① 호스트 gradle 실행은 `.env`의 `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` 필요(없으면 `Could not resolve placeholder`). `.env`가 아예 없던 상태였음. ② **키 포맷**: `JwtProvider`는 private=**PKCS#8** 기대인데 `openssl genpkey -outform DER`가 환경에 따라 **PKCS#1**을 뱉어 `algid parse error, not a sequence`로 실패. 반드시 `openssl pkcs8 -topk8 -nocrypt`로 PKCS#8 변환해야 함(`openssl pkey` 검증은 PKCS#1도 통과시켜 못 걸러내니 주의). `run-dev-web.*`가 PKCS#8로 생성/로드. `.env`는 gitignore 확인됨.
- **함정(gradlew 위치):** `gradlew`는 레포 루트가 아니라 `qtai-server/`에만 있음. `-p qtai-server` 중복 금지, PowerShell은 `.\gradlew.bat`.

## 검증 방식

- 코드 작성·정적 리뷰: Claude(샌드박스). 빌드(`flutter analyze`/`flutter test`)·런타임·커밋·푸시·PR: Windows에서 T.
- 게이트 목표: `flutter analyze` = No issues found! / 추가 테스트 통과 / 에뮬레이터 회귀 없음.

## 주의 (gotcha)

- **웹은 `dart:io` 컴파일 불가** → 앱 코드가 직접 import하지 않게 조건부 import로 격리해야 함.
- **`kIsWeb`은 `material.dart`로 안 들어온다** → `widgets.dart`가 foundation에서 `Brightness, UniqueKey`만 재노출. `package:flutter/foundation.dart`에서 명시 import 필요.
- **포트 3000 고정**: 서버 CORS 기본 허용이 `http://localhost:3000` 하나. 다른 포트면 `cors.allowed-origins` 추가 필요.
- **구현 저장소 git은 리눅스 샌드박스에서 불가**: 마운트가 `.git/index.lock` 해제(unlink)를 막음("Operation not permitted"). 커밋/푸시/PR은 Windows PowerShell로. 만약 Windows에서 커밋이 막히면 `del .git\index.lock` 후 재시도.
- **카카오 SDK 웹 미지원**: `kakao_flutter_sdk_user`는 Android/iOS 전용(pub.dev 확인). 웹 카카오 로그인은 이 SDK로 불가 → JS SDK 직접 연동(JS interop) 또는 REST OAuth 별도 작업 필요. 리포트 §5 참조.
