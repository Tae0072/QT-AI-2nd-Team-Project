# 2026-06-09 MSA Phase 1 — bible 컷오버 런북 (Inc5a)

## 목표
모놀리식 소비자(qt/note/ai)의 bible 호출을 in-process `BibleService`에서 **bible-service HTTP 호출로 실제 전환(컷오버)** 한다. Inc5b(모놀리식 bible 도메인·테이블 제거)의 **선행 조건** — 컷오버가 모든 배포 환경에서 soak되기 전에는 제거하지 않는다.

## 배경 — 현재 상태(컷오버 전)
- 모놀리식 bible client 기본값 `qtai.bible.client.mode=inprocess` → 운영/테스트 모두 여전히 in-process `BibleService`가 담당.
- HTTP 어댑터(`external/bible`)는 `mode=http`일 때만 `@Primary`로 등록(`GetBibleVerse`/`ListBibleBooks`). 어댑터·클라이언트·재시도·Circuit Breaker·fail-fast 가드 **이미 구현·검증 완료**(Inc3~Inc3d).
- bible-service inbound·persistence 기본값 off(`QTAI_BIBLE_INBOUND_ENABLED:false`, `QTAI_BIBLE_PERSISTENCE_ENABLED:false`) → 전용 DB·서빙 아직 비활성.
- Inc4: 모놀리식 크로스 도메인 FK 4개 제거 + bible-service 시드(V1 DDL→V2 books→V3 verses) 준비 완료.

즉 코드/안전장치는 준비됐고, 컷오버는 **env 플래그 활성화(운영 작업)** 다. 본 런북은 그 순서·검증·롤백을 정의한다.

## 컷오버 env 플래그

### bible-service (서빙·데이터 소유측)
| env | 컷오버 값 | 의미 |
|-----|-----------|------|
| `QTAI_BIBLE_PERSISTENCE_ENABLED` | `true` | 전용 DB 연결 활성 |
| `QTAI_BIBLE_DB_URL` / `_USERNAME` / `_PASSWORD` / `_DRIVER_CLASS_NAME` / `_DIALECT` | (전용 DB) | bible-service 전용 MySQL |
| `QTAI_BIBLE_DB_DDL_AUTO` | `validate` | 스키마 검증(Flyway가 DDL 소유) |
| `QTAI_BIBLE_DB_FLYWAY_ENABLED` | `true` | V1 DDL→V2 books→V3 verses 시드 실행 |
| `QTAI_BIBLE_INBOUND_ENABLED` | `true` | `/api/v1/bible/**` 서빙 활성 |
| `QTAI_BIBLE_GATEWAY_SHARED_TOKEN` | (시크릿) | SYSTEM 호출 인증 `X-Gateway-Token` 기대값 |
| `QTAI_BIBLE_GATEWAY_PREVIOUS_TOKEN` | (회전 중에만) | 무중단 토큰 회전 grace window |

### 모놀리식 (소비자측)
| env | 컷오버 값 | 의미 |
|-----|-----------|------|
| `QTAI_BIBLE_CLIENT_MODE` | `http` | HTTP 어댑터를 `@Primary`로 전환 |
| `QTAI_BIBLE_CLIENT_BASE_URL` | (게이트웨이/bible-service URL) | 호출 대상 |
| `QTAI_BIBLE_CLIENT_GATEWAY_TOKEN` | (시크릿) | `bible-service`의 `SHARED_TOKEN`과 **동일 값** |
| `QTAI_BIBLE_CLIENT_TIMEOUT_MS` / `RETRY_*` / `CB_*` | (기본값 사용 가능) | 타임아웃·재시도·CB 튜닝 |

> 가드: `mode=http`인데 `base-url` 또는 `gateway-token`이 비면 **부팅 시점 fast-fail**(`BibleHttpClientConfiguration` / `BibleClientProperties`). 잘못 전환된 환경이 조용히 실패하지 않는다. (Inc5a에서 base-url 누락 케이스 테스트 보강.)

## 활성화 순서 (안전 우선)
1. **전용 DB 프로비저닝 + 시드** — bible-service에 persistence env + `DB_URL` 설정, `FLYWAY_ENABLED=true`로 배포 → Flyway가 V1(DDL)→V2(books 66권)→V3(verses) 적재. **books=66, verses 행수**를 모놀리식과 대조 검증.
2. **inbound 서빙 활성** — `QTAI_BIBLE_INBOUND_ENABLED=true` + `QTAI_BIBLE_GATEWAY_SHARED_TOKEN` 설정. 게이트웨이 `/api/v1/bible/**` 라우트(Inc2)로 `X-Gateway-Token` 동반 호출 시 응답 확인.
3. **모놀리식 전환** — `QTAI_BIBLE_CLIENT_MODE=http` + `BASE_URL` + `GATEWAY_TOKEN`(=shared-token) 설정 후 배포. 이 시점부터 소비자 호출이 bible-service로 간다.
4. **검증·soak** — 아래 검증 통과 후 일정 기간 관측. 통과·안정 확인 시 Inc5b(제거) 착수 가능.

각 단계는 환경별(dev→demo→prod)로 순차 적용한다.

## 검증
- bible-service: `/actuator/health` UP, 시드 행수 대조(books=66, verses 일치).
- 게이트웨이: `/api/v1/bible/**`가 `X-Gateway-Token`으로 인증되어 bible-service로 라우팅, 토큰 없으면 거부.
- 모놀리식(mode=http): `GetBibleVerseUseCase`/`ListBibleBooksUseCase` 빈이 HTTP 어댑터로 해석됨(`BibleHttpClientConfigurationTest`가 wiring 보장). 스모크: ① Today QT 범위 조회 ② 노트 절 스냅샷 ③ AI 해설 job의 본문 조회가 정상.
- 장애 주입: bible-service 5xx 시 재시도→지속 장애 시 CB OPEN fast-fail, 4xx는 CB 미기록(`BibleServiceClientTest` 검증 동작).

## 롤백
- `QTAI_BIBLE_CLIENT_MODE=inprocess`로 되돌리면 모놀리식이 **즉시 in-process `BibleService`로 복귀**(모놀리식 bible 테이블·도메인은 Inc5b 전까지 그대로 존재하므로 무손실). 이 즉시 롤백 가능성이 **Inc5b를 컷오버 soak 이후로 미루는 이유**다.

## Inc5b(제거) 착수 전 readiness 체크리스트
- [ ] 모든 배포 환경이 `mode=http`로 전환·soak 완료
- [ ] bible-service persistence live + 시드 행수 대조 통과
- [ ] 게이트웨이 `/api/v1/bible/**` 라우트 + 토큰 인증 정상
- [ ] 런타임에 in-process `BibleService`에 의존하는 소비자 없음(소비자 단위 테스트는 인터페이스 Mock이라 무관)
- [ ] 토큰 회전 시 `PREVIOUS_TOKEN` grace window 정상

## Inc5b 예정 작업(참고)
- 모놀리식 `domain.bible.internal`(`BibleService`/엔티티/Repository)·`web/BibleController`·web 전용 UseCase(`ListChapters`/`SearchBible`)·DTO(`BibleSearchRequest`) 제거.
- `api` 계약은 **유지**: `GetBibleVerseUseCase`/`ListBibleBooksUseCase` + DTO(`BibleBookResponse`/`BibleVerseResponse`/`BibleVerseRangeResponse`/`BibleVerseBookResponse`) — 소비자·HTTP 어댑터가 의존.
- HTTP 어댑터를 무조건 활성(=`mode` 분기 제거)으로 전환, 기본값 http 고정.
- `V31__drop_bible_tables.sql`(bible_books/bible_verses DROP) — **ops 전용 DB 분리·시드 완료 전제** 명시.
- ArchUnit/Modulith 경계 갱신.

## 검증 명령
- `gradlew :test --tests "com.qtai.external.bible.*"` — 어댑터/클라이언트/config 17건(BUILD SUCCESSFUL).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
