# Flutter 브라우저(웹) 실행 지원 작업 리포트

> **작성일:** 2026-06-07
> **브랜치:** `feat/flutter-web-run-support` (base `dev`)
> **검증 환경:** Flutter 3.44.1 / Dart 3.12.1 (Windows). 코드 작성: Claude / 빌드·정적분석·커밋·푸시·PR: Windows에서 T가 직접 실행.
> **목적:** 에뮬레이터 대신 크롬/엣지/웨일 같은 브라우저에서 Flutter 앱을 띄운다. (+ 웹 TTS 음성 재생 지원)

---

## 1. 배경 — 왜 코드를 고쳐야 했나 (입문자용)

브라우저에서 도는 "Flutter 웹"은 에뮬레이터와 실행 환경이 다르다. 그대로 두면 웹 빌드가 깨지거나 서버에 못 붙는 지점이 있었다.

1. **`dart:io` 미지원**: 파일 저장/플랫폼 분기에 쓰는 `dart:io`는 웹에서 동작하지 않는다. 이걸 import한 파일이 있으면 `flutter run -d chrome` 자체가 컴파일 에러로 멈춘다(`app_config`, `tts_repository`, `note_share_sheet`).
2. **`10.0.2.2` 주소**: 안드로이드 에뮬레이터에서만 호스트 PC의 `localhost`로 매핑되는 특수 주소다. 브라우저(웹)는 `localhost`를 그대로 써야 한다.
3. **CORS**: 브라우저는 보안상 "다른 출처(origin)"로의 요청을 서버가 허용해야만 받는다.

## 2. 변경 요약

| 파일 | 변경 내용 |
| --- | --- |
| `lib/core/platform/file_storage.dart` (신규) | 조건부 import 파사드. 플랫폼별 구현을 한 곳에서 고른다. |
| `lib/core/platform/file_storage_io.dart` (신규) | 모바일/데스크톱용. 실제 `dart:io`로 파일 캐시/임시파일 처리. |
| `lib/core/platform/file_storage_stub.dart` (신규) | 웹용. 파일 시스템이 없으므로 안전하게 미지원 처리. |
| `lib/core/config/app_config.dart` | `dart:io`/`Platform` 제거 → `kIsWeb`·`defaultTargetPlatform`로 교체. 웹/iOS는 `localhost`, Android 에뮬레이터만 `10.0.2.2`. |
| `lib/features/tts/services/tts_repository.dart` | `dart:io` 직접 의존 제거. **웹은 음성을 bytes로 받아 data URI로 반환**(파일 저장 불가 대응). 기기는 기존 파일 다운로드 유지. |
| `lib/features/tts/widgets/qt_tts_button.dart` | 재생을 분기 — 웹은 `setUrl`(data URI), 기기는 `setFilePath`. |
| `lib/features/note/widgets/note_share_sheet.dart` | 노트 이미지 공유 분기. 웹은 `XFile.fromData`로 바이트 직접 공유, 기기는 기존 임시파일 경로 공유 유지. |
| `test/core/config/app_config_test.dart` | Android→`10.0.2.2`, iOS→`localhost` 호스트 분기 단위 테스트 2건 추가. |

**핵심 설계:** 앱 코드가 `dart:io`를 직접 import하지 않도록 `file_storage` 한 곳으로 몰았다. 웹 빌드 때는 `dart:io`가 든 `_io.dart`가 아예 트리에 포함되지 않아 빌드가 깨지지 않는다.

## 3. 실행 방법 (T가 Windows에서)

전제: Docker로 `qtai-server`(8080)가 떠 있어야 한다.

```bash
cd flutter-app
flutter pub get
flutter run -d chrome --web-port=3000     # 크롬
flutter run -d edge   --web-port=3000     # 엣지

# (선택) 웹에서 로그인 없이 앱에 바로 진입 — 개발 편의용. 웹+dev에서만 동작(§9).
flutter run -d chrome --web-port=3000 --dart-define=WEB_DEV_NO_LOGIN=true
```

- **웨일**은 Flutter가 기기로 인식하지 못한다 → `flutter run -d web-server --web-port=3000` 후 웨일에서 `http://localhost:3000`.
- 포트를 **3000**으로 고정하는 이유: 서버 CORS 허용 출처가 기본 `http://localhost:3000` 하나(`SecurityConfig.java`). 3000으로 띄우면 서버를 안 건드려도 API가 통한다.

## 4. 웹에서 "되는 것 / 안 되는 것"

- **됨:** 화면/네비게이션, REST API 호출(콘텐츠 조회 등), 노트 텍스트·이미지 공유.
- **TTS 음성**: 코드상 웹 재생까지 구현됨. 단, 브라우저가 TTS 서버(`localhost:8090`)를 다른 출처로 호출하므로 **그 서버에 CORS 허용(`http://localhost:3000`)을 켜야** 실제로 들린다(그 서버가 떠 있어야 함). TTS 서버 위치: `D:\ai엔진\tts 프로그램`.
- **카카오 로그인**: 이 앱이 쓰는 `kakao_flutter_sdk_user`가 **웹 미지원**(pub.dev 지원 플랫폼 = Android/iOS만). 이 SDK로는 웹 로그인이 안 된다. 자세한 건 §5.

## 5. 카카오 웹 로그인 — 현황과 선택지

- **사실:** `kakao_flutter_sdk_user`(2.x 포함)는 공식적으로 Android/iOS만 지원. 공식 README도 "웹은 추후 지원 예정". 따라서 `KakaoSdk.init`/`loginWithKakaoAccount`는 웹에서 동작하지 않는다.
- **선택지**
  - **(A) 카카오 JavaScript SDK 직접 연동(JS interop):** `web/index.html`에 `kakao.min.js`를 로드하고, 웹 전용 Dart 코드로 `Kakao.Auth` 로그인을 호출 → 받은 액세스 토큰을 기존 `POST /api/v1/auth/kakao`로 전달. **서버 변경 없음.** 단, 웹 전용 커스텀 코드(샌드박스에서 런타임 검증 불가) + 카카오 **JavaScript 키** + 콘솔에 Web 도메인/Redirect 등록이 필요.
  - **(B) REST OAuth:** 서버에 인가코드 교환 엔드포인트 추가 등 서버 변경 동반. 범위가 더 크다.
- **권장:** 개발/시연 단계에서 웹은 UI·API·TTS 확인용으로 쓰고, 실제 카카오 로그인은 에뮬레이터/기기에서 한다. 웹 로그인이 꼭 필요하면 (A)를 별도 브랜치 작업으로 진행.

## 6. 검증 체크리스트 (Windows에서 T가 실행)

```bash
cd flutter-app
flutter pub get
flutter analyze        # 기대: No issues found!
flutter test test/core/config/app_config_test.dart
flutter run -d chrome --web-port=3000
```

- [ ] `flutter analyze` 무경고
- [ ] 크롬에서 앱 진입 → 로그인 이후(에뮬레이터에서 받은 토큰/임시 우회) 오늘 QT·성경·노트가 서버 데이터로 뜸(API/CORS OK)
- [ ] 노트 → 이미지로 공유 동작
- [ ] (TTS 서버 CORS 허용 후) 오늘 QT 읽기 버튼 재생
- [ ] 안드로이드 에뮬레이터에서도 기존대로 정상(회귀 없음)

> 정적 분석/로직은 코드 리뷰로 검증(아래 §7). 런타임은 SDK가 샌드박스에 없어 직접 실행 못 함 — T가 띄워서 확인 필요.

## 7. 검토 내역 (2~3회)

1. 잔여 `dart:io`/`File`/`Directory`/`Platform`/`path_provider` 사용 grep → `file_storage_io.dart` 외 0건.
2. TTS 호출부(`qt_tts_button.dart`)가 try/catch로 감싸져 웹 예외 시에도 앱이 죽지 않음 확인.
3. 서브에이전트 독립 리뷰 → `note_share_sheet.dart`의 `kIsWeb` import 누락 1건 발견·수정. (나머지 지적은 오탐.)
4. Flutter 원본(`widgets.dart`) 확인 → foundation에서 `Brightness, UniqueKey`만 재노출 → `kIsWeb`은 `flutter/foundation.dart` 명시 import 필요 확정. 모든 `kIsWeb` 사용 파일에 import 매칭 확인.
5. 카카오 웹 지원 여부는 pub.dev 지원 플랫폼으로 교차확인(Android/iOS만).

## 8. 커밋·PR 안내

- 커밋/푸시/PR은 Windows PowerShell에서 T가 진행(구현 저장소는 리눅스 샌드박스 git이 막혀 있음).
- 스테이지 대상(8개):

```powershell
git add flutter-app/lib/core/platform/ `
        flutter-app/lib/core/config/app_config.dart `
        flutter-app/lib/features/tts/services/tts_repository.dart `
        flutter-app/lib/features/tts/widgets/qt_tts_button.dart `
        flutter-app/lib/features/note/widgets/note_share_sheet.dart `
        flutter-app/test/core/config/app_config_test.dart
git commit -m "feat(fe): 브라우저(웹) 실행 지원 — dart:io 제거·호스트/공유 분기·TTS 웹 재생"

# 웹 개발용 로그인 우회(임시·게이트됨)는 별도 커밋으로 분리 — 추후 제거 쉽게(§9):
git add flutter-app/lib/core/dev/web_dev_access.dart flutter-app/lib/main.dart
git commit -m "chore(fe): [임시] 웹 개발용 로그인 우회 (kIsWeb+dev 게이트, 추후 제거)"

git push -u origin feat/flutter-web-run-support
```

- `flutter-app/android/gradle.properties`(기존 수정분)·리포트/워크플로우 문서는 stage 제외(Git 규칙 §12).
- PR base: `dev`. 관련 화면: 노트 공유(N-04, §19.1), TTS. 커밋이 `index.lock` 에러면 `del .git\index.lock` 후 재시도.

---

## 9. 웹 개발용 로그인 우회 (임시 · 제거 가능)

카카오 로그인이 웹 미지원이라, 웹 개발 중 로그인 화면을 건너뛰고 앱에 진입하기 위한 **임시 장치**.

- **파일:** `lib/core/dev/web_dev_access.dart` (단일 파일) + `main.dart` 호출부 2줄(주석 `[WEB_DEV_ACCESS]`).
- **삼중 게이트:** `kIsWeb` + `AppConfig.isDev` + `--dart-define=WEB_DEV_NO_LOGIN=true`. 셋 다 참일 때만 동작 → **모바일·prod·release 빌드에서는 항상 무효**(보안 영향 없음).
- **실행:** `flutter run -d chrome --web-port=3000 --dart-define=WEB_DEV_NO_LOGIN=true`
- **한계:** "화면 진입"만 시켜준다. 서버 콘텐츠 API는 토큰이 필요해 토큰 없이는 데이터가 비거나 401이 난다(로그인으로 튕기진 않음). 실제 데이터까지 필요하면 **서버 dev-login**이 별도로 필요(요청 시 설계·구현).
- **제거(개발 종료 시):** `web_dev_access.dart` 삭제 + `main.dart`에서 `[WEB_DEV_ACCESS]` 주석 3곳(import 1, 호출부 2) 제거. `grep -rn "WEB_DEV_ACCESS" lib/`로 전부 찾을 수 있음.

## 10. TTS 서버 CORS (별도 서버 — 위치 확인 필요)

웹 TTS가 실제로 들리려면 TTS 서버(8090)가 웹 출처를 CORS 허용해야 한다. 그런데 mount된 `D:\ai엔진\tts 프로그램`에는 `venv`만 있고 **서버 소스가 없어** 여기서 직접 못 고친다. 서버 `main.py`(앱 생성 파일) 위치를 알려주면 반영 가능. 그동안 직접 추가할 스니펫:

**FastAPI인 경우** — `app = FastAPI(...)` 바로 아래:

```python
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://127.0.0.1:3000"],
    allow_methods=["*"],
    allow_headers=["*"],   # Authorization, Content-Type 포함
)
```

**Flask인 경우** — `pip install flask-cors` 후:

```python
from flask_cors import CORS
CORS(app, resources={r"/*": {"origins": ["http://localhost:3000", "http://127.0.0.1:3000"]}})
```

추가 후 TTS 서버를 재시작하면 웹에서 음성이 재생된다.

## 11. 웹에서 실데이터 보기 — dev 인증 재사용 (서버 dev 프로파일)

웹 우회(§9)는 화면 진입만 시켜준다. **실제 데이터까지** 보려면 서버 인증이 필요한데, 새 토큰 엔드포인트를 만들지 않고 **이미 있는 dev 우회(`X-Dev-User-Id`)를 재사용**한다(서버에 의도적으로 마련돼 있던 메커니즘 — `DevSecurityConfig`/`DevUserIdHeaderFilter`).

**작동 방식**
- 서버를 `dev` 프로파일로 띄우면(`qtai.security.dev-bypass=true`는 이미 설정됨) `DevSecurityConfig`가 켜져 모든 요청 permitAll + `X-Dev-User-Id` 헤더로 그 회원 인증.
- Flutter 웹 우회 모드에서 모든 요청에 `X-Dev-User-Id: <id>`를 자동으로 붙인다 → 서버가 그 회원으로 인증 → 실데이터.

**변경(최소)**
- 서버: `DevSecurityConfig`에 **CORS 추가**(웹 출처 `localhost:3000` 허용). dev-bypass 모드엔 CORS가 없어 브라우저가 못 붙던 갭 보완. dev 전용 파일이라 운영 무관(3중 가드 유지).
- Flutter(웹+dev+플래그에서만): `X-Dev-User-Id` 헤더 주입(`api_client.dart`), 인증상태로 취급(`auth_providers.dart`).

**실행**
```bash
# 1) 인프라 (호스트에 3306/6379 공개됨)
docker compose up -d mysql redis

# 2) dev 프로파일 서버 — gradlew는 qtai-server 폴더 안에만 있음. -p 쓰지 말 것!
cd qtai-server
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun          # Git Bash(MINGW64)
#  PowerShell이면:  $env:SPRING_PROFILES_ACTIVE="dev"; .\gradlew.bat bootRun

# 3) 웹앱 (다른 터미널, flutter-app 폴더에서)
flutter run -d chrome --web-port=3000 --dart-define=WEB_DEV_NO_LOGIN=true --dart-define=WEB_DEV_USER_ID=1
```

> 주의: `gradlew`는 레포 루트가 아니라 `qtai-server/`에만 있다(독립 Gradle 프로젝트). 루트에서 `./gradlew`는 인식 안 되고, `qtai-server` 안에서 `-p qtai-server`를 또 주면 경로가 겹쳐 실패한다. 프로파일은 `--args` 대신 환경변수 `SPRING_PROFILES_ACTIVE`로 주는 게 PowerShell 따옴표 문제를 피한다.

전제: `WEB_DEV_USER_ID`는 **DB에 존재하는 회원 id**여야 한다(에뮬레이터로 한 번 로그인해 둔 회원 id 사용). 없으면 일부 화면이 빈다.

**제거(개발 종료 시)**
- Flutter: `grep -rn "WEB_DEV_ACCESS" lib/`로 마커 전부 제거 + `web_dev_access.dart` 삭제.
- 서버: `DevSecurityConfig`의 `[WEB_DEV]` 2곳(`.cors(...)`, `devCorsConfigurationSource()`) 제거(유지해도 dev 전용이라 무해).

**커밋(분리 권장)**
- 서버 변경은 백엔드 리뷰 대상 → 별도 커밋/PR 권장: `qtai-server/.../security/DevSecurityConfig.java` → 예: `chore(dev): dev-bypass 보안체인에 웹 CORS 추가`.
- Flutter 우회: `lib/core/dev/web_dev_access.dart`, `lib/main.dart`, `lib/core/network/api_client.dart`, `lib/features/auth/providers/auth_providers.dart`.

**검증(Windows/T):** `./gradlew -p qtai-server build test` (서버), `flutter analyze` (웹).
