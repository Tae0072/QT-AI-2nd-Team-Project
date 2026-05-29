# 리포트 — report 신고 대상 존재성 검증 (B)

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `feature/report-target-validation` → PR 대상 `dev`
- 선행: report 도메인(#140)

## 1. 한 줄 요약

신고 접수 시 대상의 실제 존재·가시성을 대상 도메인 api로 검증하도록 보강했다. 검증 가능한 **POST(나눔글) 1종**을 구현(sharing `GetSharingPostUseCase`)하고, AI_QA_REQUEST(구현 빈 부재)·COMMENT·AI_ASSET은 해당 도메인 api 부재로 후속 과제로 남겼다. 전체 회귀 통과.

## 2. 변경 사항

| 구분 | 파일 | 내용 |
|------|------|------|
| 수정 | `report/internal/ReportService.java` | `validateTargetExists` 추가 — POST: `GetSharingPostUseCase.getDetail`로 존재 검증, `SHARING_POST_NOT_FOUND`만 `REPORT_TARGET_NOT_FOUND`로 변환(그 외 BusinessException 재던짐). 그 외 대상 타입은 검증 보류 |
| 수정 | `common/exception/ErrorCode.java` | `REPORT_TARGET_NOT_FOUND`(R0002, 404) 추가 |
| 수정 | `test/.../report/internal/ReportServiceTest.java` | 3-arg 생성자 갱신 + POST 검증 테스트 2건(대상없음 변환 / 비대상예외 전파) |

## 3. 검증 범위 (대상 4종)

| targetType | 검증 | 방식 |
|-----------|------|------|
| POST | ✅ | sharing `GetSharingPostUseCase.getDetail(memberId, postId)` (없거나 HIDDEN/DELETE → 404) |
| AI_QA_REQUEST | ❌ 후속 | `GetAiQaResultUseCase` 구현 빈 미등록(AiController 스텁) — 주입 시 컨텍스트 기동 실패하므로 보류. 구현되면 추가 |
| COMMENT | ❌ 후속 | sharing `CommentUseCase` 미구현 — 사용자용 존재 확인 api 생기면 보강 |
| AI_ASSET | ❌ 후속 | ai 사용자용 단건 조회 api 미제공(관리자 전용만 존재) — 보강 대상 |

> 정정: 초안은 AI_QA_REQUEST도 검증한다고 기재했으나, `GetAiQaResultUseCase` 구현 빈이 없어(스텁) 주입 시 앱 기동이 실패한다(자동 리뷰 #147 BLOCK). POST만 검증하도록 수정하고, `catch`도 `SHARING_POST_NOT_FOUND`만 변환하도록 좁혔다.

## 4. 도메인 경계

- report가 sharing의 `api/UseCase`(GetSharingPostUseCase)만 호출(Long FK only, internal 직접 접근 없음). 실제 구현체(SharingPostService)가 dev에 있어 client Mock 불필요.
- 검증 순서: 대상 타입 파싱 → **대상 존재 검증** → 중복 차단 → 저장.

## 5. 테스트 결과

| 케이스 | 결과 |
|--------|------|
| POST 대상 없음(SHARING_POST_NOT_FOUND) → REPORT_TARGET_NOT_FOUND | PASS |
| POST 검증 중 비대상 예외(FORBIDDEN)는 변환 없이 그대로 전파 | PASS |
| (기존) 접수 성공 / 중복 / TOCTOU / 잘못된 타입 | PASS |

- `./gradlew test --no-daemon` (전체) → BUILD SUCCESSFUL.

> 정정: 초안 §5에 존재하지 않던 "AI_QA 대상 없음 PASS" 행과 §2 "4-arg/2건" 표기가 코드와 어긋나 자동 리뷰(#147)가 지적. AI_QA 검증 제거(POST만)에 맞춰 §1/§2/§4/§5를 정합화하고, 신규 테스트는 POST 2건(대상없음 변환 / 비대상예외 전파)으로 명시.

## 6. 남은 후속

- COMMENT·AI_ASSET 검증은 sharing(CommentUseCase)·ai(사용자용 asset 조회)가 사용자용 존재 확인 api를 노출하면 `validateTargetExists`에 추가(담당자 조율).
