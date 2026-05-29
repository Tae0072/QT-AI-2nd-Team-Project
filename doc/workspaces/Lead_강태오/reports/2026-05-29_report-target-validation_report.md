# 리포트 — report 신고 대상 존재성 검증 (B)

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `feature/report-target-validation` → PR 대상 `dev`
- 선행: report 도메인(#140)

## 1. 한 줄 요약

신고 접수 시 대상의 실제 존재·가시성을 대상 도메인 api로 검증하도록 보강했다. 검증 가능한 POST(나눔글)·AI_QA_REQUEST 2종을 구현하고, COMMENT·AI_ASSET은 해당 도메인 api 부재로 후속 과제로 남겼다. 전체 회귀 통과.

## 2. 변경 사항

| 구분 | 파일 | 내용 |
|------|------|------|
| 수정 | `report/internal/ReportService.java` | `validateTargetExists` 추가 — POST: `GetSharingPostUseCase.getDetail`, AI_QA_REQUEST: `GetAiQaResultUseCase.getAiQaResult`로 존재 검증. NOT_FOUND/FORBIDDEN → `REPORT_TARGET_NOT_FOUND` 변환 |
| 수정 | `common/exception/ErrorCode.java` | `REPORT_TARGET_NOT_FOUND`(R0002, 404) 추가 |
| 수정 | `test/.../report/internal/ReportServiceTest.java` | 4-arg 생성자 갱신 + 대상없음 검증 테스트 2건 |

## 3. 검증 범위 (대상 4종)

| targetType | 검증 | 방식 |
|-----------|------|------|
| POST | ✅ | sharing `GetSharingPostUseCase.getDetail(memberId, postId)` (없거나 HIDDEN/DELETE → 404) |
| AI_QA_REQUEST | ✅ | ai `GetAiQaResultUseCase.getAiQaResult(memberId, requestId)` |
| COMMENT | ❌ 후속 | sharing `CommentUseCase` 미구현 — 사용자용 존재 확인 api 생기면 보강 |
| AI_ASSET | ❌ 후속 | ai 사용자용 단건 조회 api 미제공(관리자 전용만 존재) — 보강 대상 |

## 4. 도메인 경계

- report가 sharing·ai의 `api/UseCase`만 호출(Long FK only, internal 직접 접근 없음). 실제 구현체가 dev에 있어 client Mock 불필요(기존 stale Mock 스텁은 미사용 — 별도 정리 가능).
- 검증 순서: 대상 타입 파싱 → **대상 존재 검증** → 중복 차단 → 저장.

## 5. 테스트 결과

| 케이스 | 결과 |
|--------|------|
| POST 대상 없음 → REPORT_TARGET_NOT_FOUND | PASS |
| AI_QA 대상 없음 → REPORT_TARGET_NOT_FOUND | PASS |
| (기존) 접수 성공 / 중복 / TOCTOU / 잘못된 타입 | PASS |

- `./gradlew test --no-daemon` (전체) → BUILD SUCCESSFUL (39s).

## 6. 남은 후속

- COMMENT·AI_ASSET 검증은 sharing(CommentUseCase)·ai(사용자용 asset 조회)가 사용자용 존재 확인 api를 노출하면 `validateTargetExists`에 추가(담당자 조율).
