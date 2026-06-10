# 리포트 — MSA RestClient 통합 ① note→bible 구절 조회 (2026-06-10)

## 요약

Day3 RestClient 통합의 첫 호출 쌍. service-note가 bible 구절 메타를 가져올 때 쓰던 임시 `GetBibleVerseUseCaseMock`을, service-bible의 HTTP 엔드포인트를 호출하는 실제 `RestClientAdapter`로 교체했다. 이후 모든 cross-domain Mock 교체가 따를 패턴을 확립한다.

## 변경 파일

신규
- `lib-common/.../common/config/ServiceEndpointsProperties.java` — `qtai.services.*` base-url(5개 서비스, env 오버라이드)
- `service-note/.../domain/note/client/bible/GetBibleVerseRestClientAdapter.java` — bible HTTP 호출 어댑터
- 테스트 3종: `ServiceEndpointsPropertiesTest`(2), `BibleVerseLookupApiTest`(4), `GetBibleVerseRestClientAdapterTest`(6)

수정
- `lib-common/.../common/config/RestClientConfig.java` — `@EnableConfigurationProperties(ServiceEndpointsProperties.class)`
- `service-bible/.../domain/bible/web/BibleController.java` — 단건 `/verses/{verseId}`, 다건 `/verses/by-ids` 추가
- `service-note/src/main/resources/application.yml` — `qtai.services.bible-base-url`(env)

삭제
- `service-note/.../domain/note/client/bible/GetBibleVerseUseCaseMock.java`

## 설계 근거

- 통신: RestClient 동기(회의록 §3). 단일 DB·읽기 참조라 보상 트랜잭션 없음(§5).
- 인증: 요청 JWT를 그대로 전달, 대상 서비스가 공유키로 필터 검증(§5/§81). 유저 서비스 재호출 없음.
- 경로: `/api/v1/**` 유지(CLAUDE.md §5). 별도 internal 경로 신설하지 않음.
- 오류: `RestClientException`만 구체 캐치 → 공통 예외. 404→`BIBLE_VERSE_NOT_FOUND`, 그 외→`EXTERNAL_API_FAILURE`(C0006).
- 경계: 어댑터는 `note.client.bible`에 위치, `bible.api` 계약만 의존(ArchUnit 통과).

## 검증 결과

- `:lib-common:build :service-bible:build :service-note:build` → **BUILD SUCCESSFUL (46s)**.
- 신규 테스트 12개 전부 통과(실패 0/에러 0/스킵 0).
  - lib-common: 기본값·env 오버라이드 바인딩
  - service-bible MockMvc: 단건 200·다건 200·미존재 404·미인증 401
  - service-note 어댑터: 다건/단건 정상·404 변환·5xx 변환·빈목록 단락·Authorization 전달
- 첫 푸시 통과 체크리스트 자가점검: 광범위 catch 없음, `hasRole` 단독 없음, 금지 토큰 없음, 삭제 Mock 잔여 코드 참조 없음(javadoc 1줄만).

## 남은 작업

- 다음 호출 쌍 PR(읽기 우선): service-ai→bible, service-note→qt 등. 워크플로우 §5 로드맵 참고.
- audit/admin 쓰기·권한 쌍은 검증 부담이 커 뒤 순서. Kafka는 AI 영역만 유지.
