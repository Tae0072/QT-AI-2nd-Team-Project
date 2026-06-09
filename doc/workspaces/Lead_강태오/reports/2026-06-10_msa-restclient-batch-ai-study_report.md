# 리포트 — MSA 배치 RestClient ①-c service-ai → study (승인 해설 게시/숨김/조회)

작성일: 2026-06-10 / 작성: Claude (Lead 강태오) / 브랜치: `feature/msa-restclient-batch`

## 1. 요약

service-ai의 AI 자산 검수·시딩이 study 승인 해설을 게시/숨김/조회할 때 쓰던 Mock 3종을 service-bible HTTP
어댑터로 교체했다. 수신 엔드포인트가 없어 신설했고, 쓰기·AI 자산 메타 노출이라 **SYSTEM_BATCH 전용**으로 보호했다.
ai→bible/qt에 이은 ① 트랙의 마지막 호출쌍.

## 2. 변경 내역

### 신규 (service-bible)
- `study/web/VerseExplanationInternalController` — `@PreAuthorize("hasRole('SYSTEM_BATCH')")`
  - `POST /api/v1/study/verse-explanations` (게시, `@Valid`)
  - `POST /api/v1/study/verse-explanations/hide` (숨김)
  - `GET /api/v1/study/verse-explanations?verseIds=` (승인 조회)

### 신규 (service-ai)
- `ai/client/study/VerseExplanationRestClientAdapter` — `Publish`/`Hide`/`List ApprovedVerseExplanationUseCase`
  3계약을 한 클래스로 구현. 시스템 토큰 발급, POST/GET, `RestClientException`만 캐치, 빈 verseIds 단락.

### 삭제 (service-ai)
- `PublishApprovedVerseExplanationUseCaseMock`, `HidePublishedVerseExplanationUseCaseMock`,
  `ListApprovedVerseExplanationUseCaseMock`.

### 테스트
- service-ai: `VerseExplanationRestClientAdapterTest` 6건.
- service-bible: `VerseExplanationInternalApiTest` 5건(MockMvc).

## 3. 설계 결정

| 결정 | 근거 |
|---|---|
| 수신 SYSTEM_BATCH 전용 | 게시/숨김=상태 변경 쓰기, 조회=aiAssetId 메타 포함 → 승인 게이트·참조자료 미노출(§7·§10). |
| 단일 어댑터가 3 계약 구현 | 같은 리소스(verse-explanations)라 RestClient·토큰 공유. Mock 3종을 한 빈으로 대체. |
| 시스템 토큰·ObjectProvider·@Autowired·RestClientException-only·빈 입력 단락 | PR #441/#443 패턴. |

## 4. 검증

`:service-ai:build` + `:service-bible:build` **BUILD SUCCESSFUL**. 어댑터 6 + 수신 MockMvc 5 통과.

## 5. 리스크 & 후속

- 배포 env `SECURITY_JWT_SYSTEM_SECRET` 정합은 배포 세션 후속(코드 PR 범위 밖).
- `StudyPublishClient`/`StudyPublishClientMock`(ai 자체 포트, 미사용으로 보임)·글로서리/시뮬레이터 publish·hide
  UseCase는 ai 내부 미호출이라 범위 밖(무관 리팩터링 금지 §9).
- 후속: PR② retention purge, PR③ audit/admin.
