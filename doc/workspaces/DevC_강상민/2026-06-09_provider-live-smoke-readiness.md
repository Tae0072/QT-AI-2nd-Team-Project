# Provider Live Smoke Readiness

## 1. 목적

provider `/api/v1/system/**` endpoint가 열렸을 때 AI HTTP adapter가 실제 provider를 호출할 수 있는지 확인하는 live smoke 준비 문서다.

현재 단계에서는 provider endpoint가 아직 늦게 열릴 수 있으므로 실제 live 호출을 하지 않는다. 이 문서는 live smoke를 실행하기 전 필요한 환경 변수, 입력값, 중단 기준, read/write 분리 원칙을 고정한다.

## 2. 실행 원칙

| 항목 | 기준 |
| --- | --- |
| 기본 상태 | provider live smoke 실행 금지 |
| 실행 토글 | `QTAI_PROVIDER_SMOKE_ENABLED=true` |
| wrapper guard | 토글이 없으면 provider 호출 전 중단 |
| 기본 검증 | `-AllowSkip` 옵션으로 guard만 확인 |
| smoke 범위 | read endpoint만 우선 실행 |
| write endpoint | 테스트 데이터와 idempotency 승인 후 별도 PR |

## 3. Read Smoke 대상

| Provider | Client | Endpoint |
| --- | --- | --- |
| QT | `QtContextClientHttpAdapter` | `GET /api/v1/system/qt/passages/{passageId}/context` |
| QT | `QtContextClientHttpAdapter` | `GET /api/v1/system/qt/passages/today/status?date=YYYY-MM-DD` |
| Bible | `BibleVerseClientHttpAdapter` | `GET /api/v1/system/bible/verses/{verseId}` |
| Bible | `BibleVerseClientHttpAdapter` | `POST /api/v1/system/bible/verses:batch` |
| Bible | `BibleVerseClientHttpAdapter` | `GET /api/v1/system/bible/verses?book={book}&chapter={chapter}&startVerse={startVerse}&endVerse={endVerse}` |
| Admin/Auth | `AdminAuthClientHttpAdapter` | `GET /api/v1/system/admin/auth/active?memberId={id}` |
| Admin/Auth | `AdminAuthClientHttpAdapter` | `GET /api/v1/system/admin/auth/verify?memberId={id}&role={role}` |
| Admin/Auth | `AdminAuthClientHttpAdapter` | `GET /api/v1/system/admin/auth/verify-any?memberId={id}&roles={roles}` |

## 4. Write Smoke 제외

| Endpoint | 제외 사유 | 후속 조건 |
| --- | --- | --- |
| `POST /api/v1/system/study/verse-explanations:publish` | 사용자 노출 read model에 영향을 줄 수 있음 | 테스트 전용 verse/asset과 idempotency key 정책 승인 |
| `POST /api/v1/system/study/verse-explanations:hide` | 기존 노출본을 숨길 수 있음 | 테스트 전용 aiAssetId와 rollback 절차 승인 |
| `POST /api/v1/system/audit/logs` | 감사 로그를 실제로 남김 | 테스트 actor/target 정책 승인 |

## 5. 필수 환경 변수

값은 문서나 로그에 남기지 않고 실행 환경에서만 주입한다.

| 구분 | 변수 |
| --- | --- |
| 실행 토글 | `QTAI_PROVIDER_SMOKE_ENABLED` |
| 인증 | `QTAI_AI_CLIENT_SERVICE_TOKEN` |
| Provider base URL | `QTAI_AI_CLIENT_QT_BASE_URL`, `QTAI_AI_CLIENT_BIBLE_BASE_URL`, `QTAI_AI_CLIENT_ADMIN_AUTH_BASE_URL` |
| Timeout | `QTAI_PROVIDER_SMOKE_TIMEOUT_MS` |
| QT 입력 | `QTAI_PROVIDER_SMOKE_QT_PASSAGE_ID`, `QTAI_PROVIDER_SMOKE_QT_DATE` |
| Bible 입력 | `QTAI_PROVIDER_SMOKE_BIBLE_VERSE_ID`, `QTAI_PROVIDER_SMOKE_BIBLE_BATCH_VERSE_IDS`, `QTAI_PROVIDER_SMOKE_BIBLE_BOOK`, `QTAI_PROVIDER_SMOKE_BIBLE_CHAPTER`, `QTAI_PROVIDER_SMOKE_BIBLE_START_VERSE`, `QTAI_PROVIDER_SMOKE_BIBLE_END_VERSE` |
| Admin/Auth 입력 | `QTAI_PROVIDER_SMOKE_ADMIN_MEMBER_ID`, `QTAI_PROVIDER_SMOKE_ADMIN_ROLE`, `QTAI_PROVIDER_SMOKE_ADMIN_ROLES` |

## 6. 실행 방법

Provider가 아직 열리지 않은 상태에서는 guard만 확인한다.

```powershell
cd qtai-server
powershell -ExecutionPolicy Bypass -File .\scripts\provider-live-smoke-readiness.ps1 -AllowSkip
```

Provider가 열리고 모든 환경 변수가 준비된 뒤에는 토글을 켜고 실행한다.

```powershell
cd qtai-server
powershell -ExecutionPolicy Bypass -File .\scripts\provider-live-smoke-readiness.ps1
```

직접 Gradle로 실행할 수도 있다.

```powershell
cd qtai-server
.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiProviderSmokeTest
```

## 7. 중단 기준

다음 상황에서는 live smoke를 실패로 보고 provider 연결 단계로 넘어가지 않는다.

- 401 또는 403 응답
- timeout 또는 connection refused
- HTTP 5xx
- malformed envelope
- `ApiResponse<T>` envelope 누락
- Bible 응답에 허용되지 않은 번역본/본문 데이터 포함
- Admin/Auth role 검증 결과가 입력 role과 불일치
- Today QT status에서 `cacheStatus`가 계약 enum이 아님
- provider error code가 AI `AiClientException`으로 매핑되지 않음

## 8. 완료 기준

| 항목 | 기준 |
| --- | --- |
| QT context | `passageId`, `passageReference`, `passageContext` 확인 |
| Today QT status | `qtDate`, `exists`, `cacheStatus` 확인 |
| Bible single | `verseId`, `reference`, `koreanText`, `englishText` 확인 |
| Bible batch | 요청한 verse id 목록 포함 |
| Bible range | book/chapter와 verses 확인 |
| Admin active | `adminUserId`, `memberId`, `adminRole` 확인 |
| Admin verify | 단일 role 검증 응답 확인 |
| Admin verify-any | 복수 role 중 하나 충족 응답 확인 |

## 9. 후속 작업

- provider endpoint open 후 실제 `ai-provider-live-smoke` 실행
- Study/Audit write smoke 정책 승인
- gateway route enable 전 ai-service cutover runbook과 연결
- event-driven 전환 설계 전 HTTP 유지 대상 확정
