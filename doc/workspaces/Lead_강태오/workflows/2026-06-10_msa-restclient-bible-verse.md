# 2026-06-10 MSA RestClient 통합 ① — bible 구절 조회 (note→bible)

> 작성: 강태오(Lead, AI 보조). 기준: 2026-06-09 회의록 §3·§4 + `CLAUDE.md` §4·§5.
> Day3 RestClient 통합의 **첫 호출 쌍(파일럿)**. cross-domain `UseCaseMock` → 실제 HTTP `RestClientAdapter` 교체 패턴을 여기서 확립한다.

## 0. 배경

MSA 분리(Day1~2) 동안 각 서비스는 다른 도메인의 `api/UseCase`를 호출자 쪽 `client/{도메인}/...UseCaseMock`으로 임시 구현해 왔다(CLAUDE.md §4). Day3에서 이 Mock들을 RestClient(동기) 어댑터로 교체한다. 회의록 §3대로 통신은 RestClient만, Kafka는 AI 영역만 유지한다.

교체 대상 Mock은 서비스 모듈 기준 약 26개(루트 `src/`·`admin-server` 모놀리식 복사본 제외). 한 번에 다 하지 않고 **호출 쌍 단위로 PR을 쪼갠다**. 가장 단순한 "읽기" 쌍인 **note → bible 구절 조회**를 첫 PR로 삼아 패턴을 확립한다.

## 1. 이번 PR 범위 (호출 쌍 = note → bible)

- ① **lib-common**: 서비스 base-url 설정 추가(`ServiceEndpointsProperties`, `qtai.services.*`). 기존 `RestClientConfig`(빌더) 재사용.
- ② **service-bible**: 서비스 간 호출용 내부 구절 조회 엔드포인트 노출.
- ③ **service-note**: `note/client/bible/GetBibleVerseRestClientAdapter`가 `bible.api.GetBibleVerseUseCase`를 HTTP로 구현.
- ④ **Mock 삭제**: `note/client/bible/GetBibleVerseUseCaseMock` 제거(CLAUDE.md §4).

## 2. 설계 결정

- **엔드포인트 위치**: `CLAUDE.md §5`대로 모든 HTTP는 `/api/v1/**`. 별도 `/internal` 경로를 새로 만들지 않고 `BibleController`에 추가한다.
  - `GET /api/v1/bible/verses/{verseId}` — 단건
  - `GET /api/v1/bible/verses/by-ids?ids=1,2,3` — 다건(리터럴 경로라 `{verseId}`보다 우선 매칭)
  - 범위 조회 `GET /api/v1/bible/verses?bookCode&chapter` 는 기존 엔드포인트 재사용
- **서비스 간 인증**: 회의록 §5/§81 — JWT는 유저가 발급, 각 서비스가 **공유키로 필터 검증**(유저 서비스 재호출 없음). 호출자(note)는 들어온 요청의 `Authorization` 헤더를 그대로 전달한다.
- **단일 DB**: 읽기 참조라 보상 트랜잭션 불필요(회의록 §5).
- **오류 처리(CLAUDE.md §9)**: 광범위 `catch(Exception)` 금지 → `RestClientException`만 잡아 공통 예외로 감쌈. bible 404 → `BIBLE_VERSE_NOT_FOUND`, 그 외 → `EXTERNAL_API_FAILURE`(C0006, 502).
- **응답 계약**: bible 내부 서비스는 다건을 요청 순서 보존 + all-or-404로 반환한다(기존 `BibleService` 그대로). Mock의 기대 계약과 동일하게 유지.

## 3. 작업 체크리스트

- [x] 선행: admin-server PR #433 머지 확인 + `git merge origin/dev-msa`(532a611) 최신화
- [x] ① `ServiceEndpointsProperties` + `RestClientConfig @EnableConfigurationProperties`
- [x] ② `BibleController`에 단건/다건 엔드포인트 추가
- [x] ③ `GetBibleVerseRestClientAdapter` 구현(인증 전달·오류 변환)
- [x] ④ `GetBibleVerseUseCaseMock` 삭제 + `service-note/application.yml`에 `qtai.services.bible-base-url`(env) 추가
- [x] 테스트: lib-common 바인딩(2) · bible MockMvc(4) · note 어댑터 MockRestServiceServer(6)
- [x] 빌드 GREEN(`:lib-common:build :service-bible:build :service-note:build`, 46s) · 2~3회 검토
- [ ] PR `feature/msa-restclient-integration` → `dev-msa` 생성 · 첫 푸시 자동머지

## 4. 검증

```powershell
$env:JAVA_HOME='D:\workspace\tools\jdk\jdk-21.0.11+10'
cd D:\workspace\QT-AI-restclient\qtai-server
.\gradlew.bat :lib-common:build :service-bible:build :service-note:build --no-daemon
```

- 신규 테스트 12개 통과(실패/에러/스킵 0). ArchUnit 도메인 경계(note→bible.api 허용, internal 직접의존 금지) GREEN.

## 5. 남은 호출 쌍 로드맵 (다음 PR들 — 호출 쌍 단위 분할)

| 호출자 서비스 | 대상 | 교체할 Mock | 비고 |
|---|---|---|---|
| service-ai | bible | `GetBibleVerseUseCaseMock`, `BibleVerseClientMock` | 이번 패턴 그대로(읽기) |
| service-ai | qt | `GetQtUseCaseMock`, `GetQtPassageContentContextUseCaseMock` | 읽기 |
| service-ai | study | `List/Publish/HidePublished...`, `StudyPublishClientMock` | 쓰기(게시/숨김) |
| service-ai | audit/admin | `WriteAuditLogUseCaseMock`, `Verify/AdminAuth...` | 대상=admin-server. audit는 기록(쓰기) |
| service-bible(qt) | ai/member/note | `GenerateAiResponse/GetMember/GetNote...Mock` | |
| service-note | qt/member/notification | `GetQtUseCaseMock`, `sharing GetMember/SendNotification` | |
| service-user | admin/note/praise/report/sharing | `Verify/Purge.../ListMemberPraiseSong...` | retention 가드 활성화 동반 |

> 원칙: 읽기 쌍부터, 쓰기/권한 쌍은 검증·테스트 부담이 커 뒤로. audit/admin 호출 대상은 admin-server. Kafka는 AI 영역만(확장 금지).
