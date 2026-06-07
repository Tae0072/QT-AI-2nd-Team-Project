# 2026-06-06 study 사용자 응답 내부 PK(aiAssetId) 비노출 결과 보고

리뷰 §3 study 단독 처리. **브랜치 1개**(`fix/study-hide-internal-aiassetid`, 커밋 315d6bf), 서버 전체 스위트 그린.
워크플로우: `workflows/2026-06-06_study-aiassetid-hide.md`

## 문제
`GET /api/v1/qt/{id}/study-content` 응답의 `ExplanationItem`이 내부 AI 자산 PK `aiAssetId`를 사용자에게 그대로 노출(계약/보안). 내부 식별자가 외부 계약으로 굳어지면 MSA 분리 시 키 체계 변경이 전 서비스로 파급되는 문제와도 연결된다(§5.2 #6).

## 조치
- `QtStudyContentResponse.ExplanationItem`: `aiAssetId` 필드 제거 — 사용자 노출은 `verseId/summary/explanation/sourceLabel`만.
- `QtStudyContentService.toExplanationItem`: 내부 read 모델(`ApprovedVerseExplanationResponse`)의 `aiAssetId`를 사용자 DTO로 전달하지 않음(내부 추적용으로만 보존).
- OpenAPI `QtStudyExplanationItem`: `aiAssetId` 속성 제거. **관리자/시스템 스키마(`SystemAiValidationLogRequest`)의 `aiAssetId`는 유지**(admin 경로는 정상 사용).
- 회귀 가드: 컨트롤러 테스트에 `$.data.explanations[0].aiAssetId` 부재(`doesNotExist`) 검증 추가.

## 매뉴얼 통합테스트 재구성(핵심)
`AiBibleVerseAssetCustomerExposureFlowManualTest`(대형 `@SpringBootTest`)가 "승인 자산이 사용자 노출 해설의 출처인지"를 사용자 DTO의 `exposure.aiAssetId()`로 검증하고 있었다. 사용자 DTO에서 필드를 제거했으므로:
- 자산 연계 검증을 **내부 승인 read 모델** `ListApprovedVerseExplanationUseCase.listApprovedByVerseIds(verseId)`(여전히 `aiAssetId` 보유)로 전환 — 승인된 해설의 자산 id == 승인한 asset id를 단언.
- 진단용 JSON 요약의 `aiAssetId` 출력 라인 제거.
- 사용자 노출 존재 여부(`exposure != null`) 검증은 그대로 유지.

## 검증
- 전체 스위트 그린(0 failures). 매뉴얼 통합테스트(승인→사용자 노출 전 과정) 포함 통과.
- OpenAPI: Windows git diff 기준 정확히 5줄 삭제(QtStudyExplanationItem.aiAssetId)만 반영, 파일 정상.

## 머지 메모
- 단독 브랜치, 다른 P2 브랜치와 파일 교집합 없음. push·PR은 T가 직접.
- 사용자 응답 계약 변경이므로 Flutter 측에서 `aiAssetId`를 읽고 있었다면 무해(필드 미사용 추정) — 클라이언트 점검 권장.
