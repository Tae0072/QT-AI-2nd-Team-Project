# 2026-06-10 MSA 배치 RestClient ①-c service-ai → study 워크플로우

> 작업 브랜치: `feature/msa-restclient-batch` (origin/dev-msa 기준, worktree `D:\workspace\QT-AI-batch-rc`)
> 선행: PR #441(ai→bible)·#443(ai→qt). 동일 시스템 토큰 패턴 + 수신 엔드포인트 신설(SYSTEM_BATCH 전용).

## 목표

service-ai의 AI 자산 검수(승인→게시, 반려→숨김)·해설 시딩이 study 콘텐츠(verse_explanations)에 승인본을
반영할 때 쓰는 Mock 3종을 service-bible(study) HTTP 어댑터로 교체한다.

- `PublishApprovedVerseExplanationUseCaseMock` — AiAssetReviewService 승인 게시
- `HidePublishedVerseExplanationUseCaseMock` — AiAssetReviewService 반려 숨김
- `ListApprovedVerseExplanationUseCaseMock` — AiDailyQtVerseExplanationSeedService 시딩 시 기게시 판별

## 분석

세 UseCase 모두 service-bible의 `VerseExplanationService`(internal)가 구현하지만 **HTTP 노출이 없다**
(StudyController는 TODO stub, QtStudyContentController는 사용자 읽기 GET만). 수신 엔드포인트를 신설한다.

## TODO

- [x] **1. 수신 컨트롤러(service-bible)** — `study/web/VerseExplanationInternalController`
  - `POST /api/v1/study/verse-explanations` (게시, `@Valid` 명령)
  - `POST /api/v1/study/verse-explanations/hide` (숨김)
  - `GET /api/v1/study/verse-explanations?verseIds=` (승인 조회)
  - 전부 **`@PreAuthorize("hasRole('SYSTEM_BATCH')")`**
- [x] **2. 어댑터(service-ai)** — `ai/client/study/VerseExplanationRestClientAdapter` (3 UseCase를 한 클래스로 구현, RestClient/토큰 공유)
- [x] **3. Mock 3종 삭제**
- [x] **4. 테스트** — 어댑터 단위 6 + 수신 MockMvc 5
- [x] **5. 빌드** — `:service-ai:build` + `:service-bible:build` GREEN
- [x] **6. 문서**
- [ ] **7. dev-msa 정합 → 커밋·푸시·PR → 리뷰**

## 설계 결정·근거

- **수신 SYSTEM_BATCH 전용**: 게시/숨김은 콘텐츠 상태 변경 쓰기이고, 조회 응답은 AI 자산 메타(`aiAssetId`)를
  포함한다. 승인 게이트·검증 참조자료 미노출(§7·§10) 원칙상 일반 사용자에게 열면 안 된다 → 메서드 보안으로 배치만 허용.
- **한 어댑터가 3 계약 구현**: 같은 수신 리소스(verse-explanations)에 대한 게시/숨김/조회라 RestClient·토큰
  발급을 공유하는 단일 어댑터가 자연스럽다(Mock 3종을 한 빈으로 대체). bean은 3개 UseCase 타입으로 등록된다.
- **인증·오류·토큰**: 시스템 토큰 발급(ObjectProvider+@Autowired), `RestClientException`만 캐치→`EXTERNAL_API_FAILURE`,
  빈 verseIds는 HTTP 없이 단락. PR #441/#443과 동일.
- **검증**: 수신 컨트롤러는 `@Valid`로 명령을 1차 검증하고, `VerseExplanationService`가 2차 방어한다.

## 검증 결과

`:service-ai:build` + `:service-bible:build` **BUILD SUCCESSFUL**.
- 어댑터 단위 6: 게시 POST(+Bearer)·숨김 POST /hide·조회 GET 매핑, 빈 verseIds 단락, 5xx→EXTERNAL_API_FAILURE, 토큰 미설정.
- 수신 MockMvc 5: SYSTEM_BATCH 게시/숨김/조회 200, 사용자 403, 미인증 401.

## 다음

PR② user retention 배치 → note/praise/report/sharing purge, PR③ ai → audit/admin.
