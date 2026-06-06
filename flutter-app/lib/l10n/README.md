# 국제화(i18n) 가이드 — QT-AI 앱

본 폴더는 다국어 문자열의 원본(ARB)이다. 화면 문자열을 코드에서 분리해 `app_ko.arb`/`app_en.arb`로 옮긴다.

## 1. 동작 방식

- `l10n.yaml` 설정으로 `flutter pub get`(또는 `flutter gen-l10n`) 시 `AppLocalizations`가 `lib/l10n/app_localizations*.dart`로 생성된다. (Flutter 3.44+는 구 `flutter_gen` 합성 패키지를 쓰지 않는다.)
- 코드에서 사용:
  ```dart
  import 'package:qtai_app/l10n/app_localizations.dart';
  // ...
  Text(AppLocalizations.of(context).splashSubtitle)
  ```
- 기본 언어는 한국어(`locale: const Locale('ko')`), 영어(en) 지원.

## 2. 문자열 추가 절차

1. `app_en.arb`(템플릿)에 키와 영어값 + `@키` 설명 추가
2. `app_ko.arb`에 같은 키와 한국어값 추가
3. `flutter gen-l10n` 실행 → `AppLocalizations.of(context).키` 사용

키 네이밍: `화면영역동작` 카멜케이스 (예: `noteEditTitle`, `loginKakaoButton`, `commonSave`).

## 3. 화면별 이전 체크리스트 (한글 문자열 많은 순)

- [x] `main.dart` (스플래시 subtitle)
- [ ] `features/note/screens/note_detail_screen.dart`
- [ ] `features/note/screens/note_edit_screen.dart`
- [ ] `features/note/screens/note_list_screen.dart`
- [ ] `features/sharing/screens/sharing_detail_screen.dart`
- [ ] `features/sharing/screens/my_sharing_screen.dart`
- [ ] `features/mypage/screens/profile_edit_screen.dart`
- [ ] `features/mypage/screens/mypage_screen.dart`
- [ ] `features/tts/widgets/qt_tts_button.dart`
- [ ] `features/auth/screens/login_screen.dart`
- [ ] `features/auth/screens/nickname_setup_screen.dart`
- [ ] `features/onboarding/**`
- [ ] `features/bible/**`, `features/home/**`
- [ ] 에러/검증 메시지(repository·service·interceptor) — 사용자 노출 문자열만

> 참고: repository/service의 로그·예외 메시지 중 **사용자에게 직접 보이지 않는 것**은 이전 대상에서 제외한다.

## 4. 로컬 검증

```bash
flutter pub get
flutter gen-l10n
flutter analyze
```
