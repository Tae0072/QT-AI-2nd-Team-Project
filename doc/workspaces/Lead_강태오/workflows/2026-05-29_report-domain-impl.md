# 워크플로우 — report 도메인 구현 (신고 접수 API)

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 관련 F-ID: F-신고(나눔/AI 답변 신고), 연결 화면 S-03 / Q-04
- 기준 문서: `02_ERD_문서.md` v2.2 §2.18 reports, `04_API_명세서.md` v1.7 §4.4.7, `CLAUDE.md` §3~§5

## 1. 배경

W2 마감 점검에서 report 도메인이 "테스트 0건"으로 분류됐으나, 실제 확인 결과 **구현 자체가 비어 있는 스텁**(엔티티·서비스·리포지토리·컨트롤러 전부 TODO 주석)이었다. 게다가 남아있던 스텁 설계(`shareSnapshotId` 단일 대상)는 현재 ERD(`target_type` + `target_id` 다형 참조)와 어긋났다.

→ 테스트만 추가하는 것이 불가능하므로 **ERD/API 명세 기준으로 도메인을 구현한 뒤 테스트를 작성**하는 순서로 진행한다.

## 2. 작업 범위

- 사용자 API는 `POST /api/v1/reports`(신고 접수) 1건만 명세에 존재. 관리자 신고 처리(`/api/v1/admin/reports*`)는 admin 도메인 책임이라 본 작업에서 제외.
- 스텁의 GET 조회 엔드포인트는 명세에 없어 구현하지 않음(`GetReportUseCase` 인터페이스 스텁은 보존).

## 3. 구현 절차

1. `dev` 최신화 후 `feature/report-create-api` 브랜치 생성
2. ERD 기준 enum 추가: `ReportTargetType`(POST/COMMENT/AI_QA_REQUEST/AI_ASSET), `ReportStatus`(RECEIVED/REVIEWING/RESOLVED/REJECTED)
3. `Report` 엔티티: `reporter_member_id` + (`target_type`,`target_id`) + `reason`/`detail`/`status`/`processed_*`/`created_at`/`updated_at`, UNIQUE `(reporter_member_id, target_type, target_id)` 및 ERD 인덱스 3종
4. `ReportRepository`: `JpaRepository` 확장 + `existsByReporterMemberIdAndTargetTypeAndTargetId`
5. `ReportCreateRequest`/`ReportResponse` DTO를 명세 §4.4.7에 맞게 정의(요청: targetType/targetId/reason/detail, 응답: id/status/createdAt)
6. `CreateReportUseCase` 포트 + `ReportService` 구현(대상 타입 검증 → 중복 차단 → INSERT, status=RECEIVED)
7. `ReportController`: `POST /api/v1/reports`, `@AuthenticationPrincipal` + `ApiResponse` 포장, 201 Created
8. `ErrorCode.DUPLICATE_REPORT`(R0001, 409) 추가
9. 단위 테스트(ReportService) + 슬라이스 테스트(ReportController) 작성
10. 전체 테스트 통과 확인 후 PR

## 4. 도메인 경계 / 정책 준수

- `internal` 외부 노출 없음, 다른 도메인 Entity/Service 직접 import 없음, Long FK만 보관(CLAUDE.md §3).
- write 경로에 `@Transactional`, read 기본 `@Transactional(readOnly = true)` (§9).
- 중복 신고는 사전 `existsBy` 검사 + 동시성(TOCTOU) 대비 `DataIntegrityViolationException` → `DUPLICATE_REPORT` 변환.
- 대상 존재성(나눔글/AI 산출물) 교차 검증은 각 대상 도메인 client 어댑터로 후속 처리 — 본 MVP 접수 경로에서는 형식 검증 + 중복 차단만 수행(후속 과제로 명시).

## 5. 검증 명령

```powershell
cd qtai-server
.\gradlew.bat test --tests "*Report*" --console=plain   # 신고 도메인 테스트
.\gradlew.bat test --no-daemon --console=plain          # 전체 회귀(경계/DDL 포함)
```
