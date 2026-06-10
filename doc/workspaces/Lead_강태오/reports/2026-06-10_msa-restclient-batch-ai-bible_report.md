# 리포트 — MSA 배치 RestClient ① service-ai → bible (시스템 토큰 첫 소비자)

작성일: 2026-06-10 / 작성: Claude (Lead 강태오 워크스페이스) / 브랜치: `feature/msa-restclient-batch`

## 1. 요약

service-ai 해설 생성 배치의 성경 본문 조회를 임시 `GetBibleVerseUseCaseMock`에서 **service-bible HTTP 호출 어댑터**로 교체했다. 배치는 사용자 JWT가 없으므로 PR #440의 공유 HS256 시스템 토큰(`SystemTokenProvider`)을 발급해 호출한다 — **시스템 인증 토큰의 첫 실사용 소비자**.

## 2. 변경 내역

### 신규
- `service-ai/.../ai/client/bible/GetBibleVerseRestClientAdapter.java` — `GetBibleVerseUseCase` 구현. `RestClient`로 `/api/v1/bible/verses/{id}`·`/verses/by-ids`·`/verses`(범위) 호출. 시스템 토큰 Bearer 주입, 응답 `ApiResponse` 언랩.
- `service-ai/src/test/.../GetBibleVerseRestClientAdapterTest.java` — 단위테스트 7건(MockRestServiceServer).

### 삭제
- `service-ai/.../ai/client/bible/GetBibleVerseUseCaseMock.java` — 어댑터로 대체(CLAUDE.md §4).

### 무변경(확인)
- service-bible: 수신 엔드포인트(`/verses/**`)·SecurityConfig(`authenticated()`)가 SYSTEM_BATCH를 그대로 수용 → 변경 없음.

## 3. 설계 결정

| 결정 | 근거 |
|---|---|
| 시스템 토큰 발급(사용자 토큰 전달 아님) | 배치는 요청 컨텍스트 없음. `SystemTokenProvider.issueSystemToken()` HS256 단명 SYSTEM_BATCH, 수신측 필터 폴백 검증(#440). |
| `ObjectProvider<SystemTokenProvider>` + 운영 생성자 `@Autowired` | 시크릿 미설정(테스트/부팅) 시에도 빈 생성 성공, 발급 불가는 호출 시점 `EXTERNAL_API_FAILURE`. 생성자 2개 모호성 제거. |
| `RestClientException`만 캐치 | 광범위 catch 금지(§9). 404→`BIBLE_VERSE_NOT_FOUND`, 그 외→`EXTERNAL_API_FAILURE`. |
| 토큰/시크릿 미로깅 | §7·§9. |

## 4. 검증

호스트 `:service-ai:build` **BUILD SUCCESSFUL**. 단위테스트 7건 통과: 단건·다건 매핑, 시스템토큰 Bearer 헤더 주입, 404·5xx 매핑, 빈 목록 무호출 단락, 토큰 발급기 미설정 실패.

## 5. 리스크 & 후속 TODO

- **배포 env 정합**: `SECURITY_JWT_SYSTEM_SECRET`이 `docker-compose.yml`/`k8s`에 아직 없음(#440은 메커니즘만). 호출자(ai)·수신자(bible 등) 동일 시크릿 주입은 **로컬 배포 세션과 정합한 단일 배포 PR**로 처리(코드 PR 범위 밖, 본 PR 본문에 의존성 명시).
- **후속 호출쌍**: ai→qt, ai→study(쓰기 — service-bible 수신 엔드포인트 신설 필요 가능), service-user retention purge, ai→audit/admin.
- `BibleVerseClient`/`BibleVerseClientMock`(ai 자체 포트, 미사용으로 보임)은 본 PR 범위 밖(무관 리팩터링 금지 §9) — 별도 정리 검토.
