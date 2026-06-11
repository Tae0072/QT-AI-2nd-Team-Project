# qtai_app

QT-AI Flutter 앱 (사용자용 — Today QT·노트·나눔·마이페이지).

## 실행 (로컬 개발)

카카오 네이티브 앱 키는 저장소에 커밋하지 않는다(CLAUDE.md §8). 실행 시 `--dart-define`으로 주입한다:

```bash
# 모바일(에뮬레이터/디바이스) — 카카오 로그인 포함 전체 기능
flutter run --dart-define=KAKAO_NATIVE_APP_KEY=<dev 네이티브 앱 키>

# 키 없이 실행 — 카카오 로그인만 비활성(경고 로그), 나머지 개발 가능
flutter run
```

- dev 키 값은 팀 공유 채널(또는 담당자 DM)로 받는다. 코드·문서에 평문으로 적지 않는다.
- prod/staging 빌드는 키 미주입 시 기동 자체가 실패한다(빠른 실패 — `AppConfig.initialize`).
- 웹 개발: 카카오 dart SDK가 웹 미지원이라 키가 필요 없다. 로그인 우회는 저장소 루트의
  `run-dev-web.ps1`(또는 `.sh`)이 `WEB_DEV_NO_LOGIN=true`로 처리한다.

### 자주 쓰는 dart-define

| 키 | 기본값 | 용도 |
|---|---|---|
| `KAKAO_NATIVE_APP_KEY` | (없음) | 카카오 로그인. dev에서 비우면 로그인만 비활성 |
| `ENV` | `dev` | `dev`/`staging`/`prod` |
| `API_BASE_URL` | env별 기본 | 서버 주소 오버라이드 |
| `WEB_DEV_NO_LOGIN` | `false` | 웹 dev 로그인 우회(삼중 게이트) |
| `WEB_DEV_USER_ID` | `1` | 웹 dev 우회 시 인증할 memberId |

## 검증

```bash
flutter analyze
flutter test
```
