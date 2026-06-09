# 2026-06-10 MSA 배치 RestClient ①-b service-ai → qt 워크플로우

> 작업 브랜치: `feature/msa-restclient-batch` (origin/dev-msa 기준, worktree `D:\workspace\QT-AI-batch-rc`)
> 선행: PR #441(ai→bible, 시스템 토큰 첫 소비자) 머지. 동일 패턴 재사용.

## 목표

service-ai 해설 생성 배치·00:05 시딩이 QT 본문 컨텍스트(verseId·제목·공개여부)를 가져올 때 쓰는
`GetQtPassageContentContextUseCaseMock`을 service-bible(qt) HTTP 호출 어댑터로 교체한다.

## bible 쌍과의 차이: 수신 엔드포인트 신설 필요

ai→bible은 수신 엔드포인트(`/api/v1/bible/verses/**`)가 PR #435에 이미 있었지만, **qt 콘텐츠 컨텍스트는
HTTP로 노출돼 있지 않았다**(QtController는 사용자용 `/today`·`/passages/{id}`만 — `TodayQtResponse` 반환).
`GetQtPassageContentContextUseCase`(getContentContext/findContentContextByDate)는 service-bible의
`QtService`가 구현하지만 컨트롤러가 없어, **수신 엔드포인트를 신설**한다.

## TODO

- [x] **0. 분석** — `ExplanationGenerationJobHandler`·`AiDailyQtVerseExplanationSeedService`가 `GetQtPassageContentContextUseCase` 주입. 수신 노출 부재 확인.
- [x] **1. 수신 컨트롤러 신설(service-bible)** — `qt/web/QtContentContextController`
  - `GET /api/v1/qt/passages/{id}/content-context` (id 조회)
  - `GET /api/v1/qt/content-context?qtDate=` (날짜 조회, 없으면 404)
  - **`@PreAuthorize("hasRole('SYSTEM_BATCH')")`** — 미공개 본문(published=false)도 반환하므로 일반 사용자 노출 차단(§6·§8). admin denyAll과 별개의 배치 전용 게이트.
- [x] **2. 어댑터(service-ai)** — `ai/client/qt/GetQtPassageContentContextRestClientAdapter`
- [x] **3. Mock 삭제** — `GetQtPassageContentContextUseCaseMock`
- [x] **4. 테스트** — 어댑터 단위 6 + 수신 컨트롤러 MockMvc 6(SYSTEM_BATCH 200·사용자 403·미인증 401·404)
- [x] **5. 빌드** — `:service-ai:build` + `:service-bible:build` GREEN
- [x] **6. 문서**
- [ ] **7. dev-msa 정합 → 커밋·푸시·PR → 리뷰**

## 설계 결정·근거

- **수신 SYSTEM_BATCH 전용**: 컨텍스트는 선등록 미공개 본문도 돌려준다(사전 생성용). 사용자에게 verseId·미공개 본문이 새면 §8 위반 → 메서드 보안 `hasRole('SYSTEM_BATCH')`로 배치만 허용. bible SecurityConfig는 `authenticated()`라, 이 게이트가 없으면 일반 사용자도 읽을 수 있었다.
- **404→Optional 보존**: `findContentContextByDate`는 "해당 날짜 없음"을 `Optional.empty()`로 줘야 한다. 수신은 없을 때 404를 던지고, 어댑터는 `RestClient.exchange()`로 404를 직접 분기해 empty로 변환한다(`retrieve().onStatus`로는 정상/404 본문 구분이 번거로움). `getContentContext`는 404→`QT_PASSAGE_NOT_FOUND`.
- **시스템 토큰·ObjectProvider·@Autowired·RestClientException-only**: ai→bible(PR #441)과 동일 패턴.

## 검증 결과

`.\gradlew.bat --no-daemon :service-ai:build :service-bible:build` → **BUILD SUCCESSFUL**.
- 어댑터 단위 6: id 200 매핑(+Bearer), id 404→NOT_FOUND, 날짜 200, 날짜 404→empty, 5xx→EXTERNAL_API_FAILURE, 토큰 미설정 실패.
- 수신 MockMvc 6: SYSTEM_BATCH id/날짜 200, 없는 id/날짜 404, 사용자 403, 미인증 401.

## 다음

ai→study(Publish/List/Hide ApprovedVerseExplanationUseCase — 쓰기, service-bible study 수신 엔드포인트 신설), 이후 user retention purge, ai→audit/admin.
