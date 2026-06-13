# QUICKSTART — 클론 직후 한 번에 실행 (Windows)

이 문서는 **GitHub에서 갓 클론받은 직후**, 백엔드(MSA)와 Flutter 앱까지 한 번에 띄우는 방법입니다.

## 0. 사전 준비 (한 번만)

- **Docker Desktop** 설치 후 실행 중
- **JDK 21** (`java -version` 이 21, `JAVA_HOME`이 21을 가리킬 것)
- **Flutter SDK** (`flutter` 가 PATH에)
- (실기기로 띄울 경우) **Android SDK platform-tools의 adb**, USB 디버깅 켠 기기 연결
- **openssl은 필요 없음** — JWT 키는 host에 openssl이 있으면 그걸 쓰고, 없으면 **Docker(mysql 이미지)로 자동 생성**합니다.

> AI 성경 해설 데이터는 별도 핸드오프 패키지입니다. 받았다면 압축을 풀어 레포 루트의 `db-import/` 에 두면 스크립트가 자동 적재합니다. 없으면 그 단계만 건너뜁니다.

## 1. 한 줄 실행

레포 루트에서:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1
```

이 명령 하나가 순서대로 처리합니다:

1. 사전 점검 (Docker / JDK21 / Flutter)
2. `.env` + JWT 키 생성 (없을 때만)
3. 서비스 jar 빌드 (`gradlew bootJar`, 빌드 락 자동 정리·재시도)
4. Docker 이미지 빌드 + MySQL/Redis 기동
5. **DB 스키마 + 성경 seed** (빈 DB에 Flyway 마이그레이션을 직접 적용 — 이미 시드돼 있으면 건너뜀)
6. 전체 서비스 + 게이트웨이 기동 (게이트웨이 재시작으로 502 방지)
7. **AI 해설 import** (`db-import/` 있고 아직 안 했을 때만)
8. Flutter 실행 (기기 자동 선택 + 종류별 백엔드 주소 자동 설정)

접속 진입점은 게이트웨이 **http://localhost:8080** 입니다.

## 2. 자주 쓰는 옵션

```powershell
# 백엔드까지만 (Flutter 안 띄움)
.\scripts\dev-up.ps1 -BackendOnly

# 특정 기기로 실행 (flutter devices 의 id)
.\scripts\dev-up.ps1 -Device R5KL3064NWM

# AI 해설 import 건너뛰기
.\scripts\dev-up.ps1 -SkipImport

# DB를 완전히 비우고 처음부터 (주의: 로컬 DB 데이터 삭제)
.\scripts\dev-up.ps1 -Recreate
```

## 3. 기기 자동 선택 (cascade)

`-Device`를 지정하지 않으면 아래 순서로 자동 선택합니다:

1. **USB 실기기**(갤럭시 탭 등)가 연결돼 있으면 → 그 기기
2. 없고 **이미 실행 중인 에뮬레이터**가 있으면 → 그 에뮬레이터
3. 둘 다 없으면 → **등록된 AVD를 자동 실행**하고 부팅을 기다림(최대 180초)
4. AVD도 없으면 → **웹(chrome/edge)** 으로 폴백

특정 기기로 강제하려면 `-Device <id>` (예: `-Device chrome`, `-Device Pixel_10_Pro_XL`의 실행 후 id).

기기 종류별 백엔드 접속은 자동 분기됩니다:

| 대상 | 백엔드 접속 | 스크립트가 해주는 것 |
| --- | --- | --- |
| USB 실기기(갤럭시 탭 등) | `localhost:8080` | `adb reverse tcp:8080` + `DEV_BASE_URL=localhost` 자동 |
| 안드로이드 에뮬레이터 | `10.0.2.2:8080` | 앱 기본값 사용(추가 설정 없음) |
| 웹(chrome/edge) | `localhost:8080` | 추가 설정 없음 |

카카오 네이티브 앱 키는 AndroidManifest 값(`53e5afb2d90048af9e71332e47f387fa`)을 기본으로 주입합니다. 다른 키면 `-KakaoKey <키>`.

## 4. 포트 한눈에

게이트웨이 8080 · service-user 8081 · service-bible 8082 · service-note 8083 · service-ai 8084 · admin-server 8090 · MySQL **3307** · Redis **6380**

## 5. 자주 막히는 곳 (스크립트가 자동 처리하지만, 수동 실행 시 참고)

- **jar를 안 만들고 docker만 띄움** → 각 Dockerfile은 빌드된 jar를 복사만 한다. 반드시 `gradlew bootJar` 먼저.
- **DB에 성경 데이터가 없음** → `docker compose`는 Flyway를 꺼두므로 seed가 안 들어간다. 빈 DB에 마이그레이션을 직접 적용해야 한다(스크립트 5단계).
- **API가 전부 502** → 게이트웨이(nginx)가 옛 서비스 IP를 캐시한 것. `docker restart qtai-gateway`. (정상은 인증 필요 API에서 **401**)
- **Flutter l10n 빌드 실패(`doesn't allow reading and writing`)** → `lib/l10n` 폴더의 Windows ReadOnly 표식 때문. 스크립트가 실행 전 자동 제거. 수동이면:
  `(Get-Item .\flutter-app\lib\l10n).Attributes='Directory'`
- **빌드 폴더 삭제 실패(`Unable to delete ...mergeDebugAssets`)** → 라이브 프로세스 락이 아니라 `build` 폴더 파일의 **Windows ReadOnly 표식** 때문에 gradle이 삭제하지 못하는 것(진단으로 확인). 스크립트가 Flutter 실행 직전 build 트리의 ReadOnly를 자동 제거한다. 수동이면: `Remove-Item .\flutter-app\build -Recurse -Force` 후 재실행.
- **실기기에서 데이터 안 보임** → 앱은 안드로이드에서 기본적으로 에뮬레이터 주소(10.0.2.2)를 쓴다. 실기기는 `adb reverse` + `--dart-define=DEV_BASE_URL=http://localhost:8080/api/v1` 필요(스크립트가 자동).

## 6. 수동으로 단계별 실행하고 싶다면

```powershell
# 1) .env + JWT 키
.\scripts\generate-keys.ps1
# 2) jar 빌드
cd qtai-server; .\gradlew.bat bootJar; cd ..
# 3) 전체 스택
docker compose up -d --build
# 4) (필요시) 게이트웨이 재시작
docker restart qtai-gateway
# 5) Flutter (실기기 예시)
adb reverse tcp:8080 tcp:8080
cd flutter-app
flutter run -d <deviceId> `
  --dart-define=DEV_BASE_URL=http://localhost:8080/api/v1 `
  --dart-define=KAKAO_NATIVE_APP_KEY=53e5afb2d90048af9e71332e47f387fa
```

> 참고: 레포에 있던 `run-dev-web.ps1` 은 모놀리식 시절 기준이라 MSA 전체 스택을 띄우지 못합니다. `scripts/dev-up.ps1` 을 사용하세요.
