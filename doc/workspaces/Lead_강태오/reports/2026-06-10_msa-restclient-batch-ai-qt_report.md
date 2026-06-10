# 리포트 — MSA 배치 RestClient ①-b service-ai → qt

작성일: 2026-06-10 / 작성: Claude (Lead 강태오) / 브랜치: `feature/msa-restclient-batch`

## 1. 요약

service-ai 해설 배치의 QT 콘텐츠 컨텍스트 조회를 `GetQtPassageContentContextUseCaseMock`에서
service-bible HTTP 어댑터로 교체했다. bible 쌍과 달리 **수신 엔드포인트가 없어 신설**했고, 미공개 본문
노출 방지를 위해 **SYSTEM_BATCH 전용**으로 보호했다.

## 2. 변경 내역

### 신규 (service-bible)
- `qt/web/QtContentContextController` — 배치 전용 수신 엔드포인트 2종, `@PreAuthorize("hasRole('SYSTEM_BATCH')")`.
  - `GET /api/v1/qt/passages/{id}/content-context`
  - `GET /api/v1/qt/content-context?qtDate=` (없으면 404)

### 신규 (service-ai)
- `ai/client/qt/GetQtPassageContentContextRestClientAdapter` — `GetQtPassageContentContextUseCase` 구현. 시스템 토큰 발급, `exchange`로 404→`Optional.empty()`(날짜)·`QT_PASSAGE_NOT_FOUND`(id) 분기.

### 삭제 (service-ai)
- `GetQtPassageContentContextUseCaseMock`.

### 테스트
- service-ai: `GetQtPassageContentContextRestClientAdapterTest` 6건.
- service-bible: `QtContentContextApiTest` 6건(MockMvc).

## 3. 설계 결정

| 결정 | 근거 |
|---|---|
| 수신 SYSTEM_BATCH 전용(`@PreAuthorize`) | 컨텍스트가 미공개 본문도 반환 → 사용자 노출 차단(§6·§8). hasRole('ADMIN') 단독 우회 우려와 무관(배치 역할 한정). |
| 404→Optional 보존을 `exchange`로 | `findContentContextByDate` 계약(없음=empty) 유지. retrieve로는 404 본문/정상 본문 구분이 번거로움. |
| 시스템 토큰·ObjectProvider·@Autowired·RestClientException-only | PR #441과 동일 패턴(§4·§7·§9). |

## 4. 검증

`:service-ai:build` + `:service-bible:build` **BUILD SUCCESSFUL**. 어댑터 6 + 수신 MockMvc 6 통과
(SYSTEM_BATCH 200, 사용자 403, 미인증 401, 없음 404, 5xx/토큰미설정 EXTERNAL_API_FAILURE).

## 5. 리스크 & 후속

- 배포 env 정합(`SECURITY_JWT_SYSTEM_SECRET`)은 PR #441과 동일하게 배포 세션 정합 후속.
- `GetQtUseCaseMock`(qt.api `GetQtUseCase`, 내부 미사용으로 보임)은 범위 밖(무관 리팩터링 금지 §9).
- 후속: ai→study, user retention purge, ai→audit/admin.
