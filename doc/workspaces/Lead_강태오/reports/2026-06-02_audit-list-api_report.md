# 리포트 — 감사 로그 조회 API (audit)

- 작업자: Lead 강태오 · 날짜: 2026-06-02
- 브랜치: `feature/audit-list-api` → PR 대상 `dev`
- 관련: 체크리스트 #27

## 1. 한 줄 요약

스텁이던 `AuditController`를 구현해 관리자 감사 로그 조회 API(`GET /api/v1/audit`)를 완성했다. 필터(행위자/액션/기간)+페이징, ROLE_ADMIN 전용. 전체 회귀 630건 통과.

## 2. API

| Method | URL | 권한 |
|--------|-----|------|
| GET | `/api/v1/audit?actorType=&actorId=&actionType=&from=&to=&page=&size=` | ROLE_ADMIN |

- 응답: `ApiResponse<Page<AuditLogResponse>>` — id·adminUserId·actorType·actorId·actorLabel·actionType·targetType·targetId·createdAt (before/after json 제외).
- 정렬: createdAt desc 기본. from/to는 ISO-8601 date-time.

## 3. 변경 파일

| 구분 | 파일 |
|------|------|
| 신규 | `audit/api/dto/AuditLogFilter.java`, `audit/web/AuditControllerTest.java` |
| 채움 | `audit/api/dto/AuditLogResponse.java`(엔티티 필드 반영), `audit/api/ListAuditUseCase.java`, `audit/web/AuditController.java`(구현) |
| 수정 | `audit/internal/AuditRepository.java`(search 추가), `audit/internal/AuditService.java`(list 구현), `audit/internal/AuditServiceTest.java`(테스트 추가) |

## 4. 정책

- `@PreAuthorize("hasRole('ADMIN')")`(AdminController 패턴, method security 활성)로 관리자 전용 강제. 일반 사용자 미노출(§5).
- read `@Transactional(readOnly=true)`. api↔internal 경계: 엔티티→DTO 매핑은 internal(AuditService).
- audit는 강태오 단독 소유(CODEOWNERS)라 타인 리뷰 대기 없음.

## 5. 검증

- `*Audit*` 7건 통과. 전체 `./gradlew test` → **630건 통과(실패 0)** — ArchUnit 경계·컨텍스트 로드 포함.
