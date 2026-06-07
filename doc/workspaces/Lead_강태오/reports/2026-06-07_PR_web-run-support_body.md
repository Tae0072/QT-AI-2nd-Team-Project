## 구현 내용

Flutter 앱을 브라우저(크롬/엣지/웨일)에서 실행할 수 있게 하는 플랫폼 호환 작업.

- `dart:io`를 앱 코드에서 직접 쓰지 않도록 조건부 import 파사드(`core/platform/file_storage`) 도입 → 웹 빌드가 깨지지 않음.
- `app_config`의 서버 호스트 분기를 `Platform`(dart:io) → `kIsWeb`/`defaultTargetPlatform`로 교체. 웹/iOS는 `localhost`, Android 에뮬레이터는 `10.0.2.2`.
- TTS: 웹은 파일 저장이 불가하므로 음성을 bytes→data URI로 받아 `setUrl`로 재생(기기는 기존 파일 경로 유지).
- 노트 이미지 공유: 웹은 `XFile.fromData`(바이트 공유), 기기는 기존 임시파일 경로 유지.

기기(에뮬레이터/실기) 동작은 변경 없음 — 모든 분기는 `kIsWeb`/플랫폼 기준으로 갈라지며 비웹 경로는 기존과 동일.

## 관련 요구사항 / 문서

- 플랫폼 호환 작업(신규 F 기능 아님). 영향 화면: 노트 공유(N-04, §19.1), TTS 읽기.
- 작업 리포트: `doc/workspaces/Lead_강태오/reports/2026-06-07_flutter-web-run-support_report.md`

## 변경 유형

- [x] 버그/호환 수정 (웹 빌드 불가 → 가능)
- [ ] 신규 기능(F-ID)
- [ ] 리팩터링
- [x] 테스트 추가

## 코드 체크리스트 (v3.1)

- [x] 도메인 경계 위반 없음 (`core/platform`만 신규, 타 도메인 internal import 없음)
- [x] `dart:io` 직접 의존 제거(웹 빌드 안전) — 잔존은 `file_storage_io.dart`(비웹 전용)뿐
- [x] 기기/모바일 경로 회귀 없음 (분기만 추가)
- [x] secret/token/key 미포함
- [x] 관련 없는 리팩터링·포맷팅 없음

## 테스트 체크리스트

- [x] `app_config` 호스트 분기 단위 테스트 추가 (Android→10.0.2.2, iOS→localhost)
- [ ] `flutter analyze` 무경고 (PR 올리기 전 실행)
- [ ] `flutter test` 통과 (PR 올리기 전 실행)
- [ ] 안드로이드 에뮬레이터 스모크(회귀 없음)

## 테스트 방법

```bash
cd flutter-app
flutter pub get
flutter analyze                       # 기대: No issues found!
flutter test test/core/config/app_config_test.dart
# 웹 실행 확인:
flutter run -d chrome --web-port=3000
```

## 담당 범위 밖 / 후속

- 웹 카카오 로그인은 카카오 SDK 웹 미지원으로 별도 설계 필요(서버측 OAuth) — 설계안만 작성, 본 PR 범위 아님: `designs/2026-06-07_web-kakao-login-server-oauth_design.md`
- 웹 개발 편의용 로그인 우회 / 서버 dev CORS / 실행 스크립트는 임시 dev 도구로 본 PR에서 제외(별도 판단).
