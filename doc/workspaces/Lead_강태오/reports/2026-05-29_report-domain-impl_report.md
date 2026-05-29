# 리포트 — report 도메인 구현 (신고 접수 API)

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `feature/report-create-api` → PR 대상 `dev`
- 관련 F-ID: 신고(S-03 신고 바텀시트, Q-04 AI 답변 신고)

## 1. 한 줄 요약

비어 있던 report 도메인을 ERD/API 명세 기준으로 구현하고(신고 접수 `POST /api/v1/reports`) 단위·슬라이스 테스트 7건을 추가했다. 전체 테스트 회귀 통과.

## 2. 변경 파일

| 구분 | 파일 | 내용 |
|------|------|------|
| 신규 | `domain/report/internal/ReportTargetType.java` | 대상 타입 enum (POST/COMMENT/AI_QA_REQUEST/AI_ASSET) |
| 신규 | `domain/report/internal/ReportStatus.java` | 상태 enum (RECEIVED/REVIEWING/RESOLVED/REJECTED) |
| 구현 | `domain/report/internal/Report.java` | ERD reports 테이블 매핑(다형 대상 + UNIQUE + 인덱스 3종) |
| 구현 | `domain/report/internal/ReportRepository.java` | JpaRepository + 중복 검사 메서드 |
| 구현 | `domain/report/internal/ReportService.java` | CreateReportUseCase 구현(검증·중복차단·TOCTOU) |
| 구현 | `domain/report/api/CreateReportUseCase.java` | 포트 시그니처 |
| 구현 | `domain/report/api/dto/ReportCreateRequest.java` | 요청 DTO + Bean Validation |
| 구현 | `domain/report/api/dto/ReportResponse.java` | 응답 DTO (id/status/createdAt) |
| 구현 | `domain/report/web/ReportController.java` | `POST /api/v1/reports` |
| 수정 | `common/exception/ErrorCode.java` | `DUPLICATE_REPORT`(R0001, 409) 추가 |
| 신규 | `test/.../report/internal/ReportServiceTest.java` | 단위 테스트 4건 |
| 신규 | `test/.../report/web/ReportControllerTest.java` | 슬라이스 테스트 3건 |

## 3. API 계약 (명세 §4.4.7 일치)

요청 `POST /api/v1/reports` (USER):

```json
{ "targetType": "POST", "targetId": 300, "reason": "INAPPROPRIATE", "detail": "부적절한 표현" }
```

응답 `201 Created`:

```json
{ "id": 900, "status": "RECEIVED", "createdAt": "2026-05-29T12:00:00" }
```

중복 신고 시 `409 DUPLICATE_REPORT(R0001)`.

## 4. 테스트 결과

| 테스트 | 케이스 | 결과 |
|--------|--------|------|
| ReportServiceTest | 접수 성공(RECEIVED) | PASS |
| | 중복 신고 차단(사전 검사) | PASS |
| | 동시 INSERT UK 위반(TOCTOU) → DUPLICATE_REPORT | PASS |
| | 지원하지 않는 대상 타입 → INVALID_INPUT | PASS |
| ReportControllerTest | 201 접수 | PASS |
| | 409 중복 | PASS |
| | 400 targetType 누락(검증) | PASS |

- `./gradlew test --tests "*Report*"` → BUILD SUCCESSFUL
- `./gradlew test --no-daemon` (전체) → BUILD SUCCESSFUL (33s) — ArchUnit 경계·엔티티 DDL 포함 회귀 통과

## 5. 남은 리스크 / 후속 과제

- **대상 존재성 교차 검증 미구현**: 신고 대상(나눔글/댓글/AI Q&A/AI 산출물)의 실제 존재·가시성 검증은 각 대상 도메인의 `api/UseCase`를 client 어댑터로 호출해 후속 보강 필요. 현재는 형식 검증 + 중복 차단까지만.
- **관리자 신고 처리**(`GET /api/v1/admin/reports`, `POST /api/v1/admin/reports/{id}/resolve`)는 admin 도메인 책임 — 별도 작업.
- `GetReportUseCase` 인터페이스는 명세에 사용자 조회가 없어 스텁으로 보존(향후 필요 시 admin 연동).

## 6. 빌드 중 발견한 환경 메모

`build/test-results/test` 디렉터리 잠금으로 `test` 태스크가 간헐적으로 `Unable to delete directory` 실패 → `gradlew --stop` 후 해당 폴더 삭제, 또는 `--no-daemon` 실행으로 회피. 코드 문제 아님.
