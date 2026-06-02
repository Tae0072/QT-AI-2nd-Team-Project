# 워크플로우 — 감사 로그 조회 API (audit)

- 작업자: Lead 강태오
- 날짜: 2026-06-02
- 대상: QT-AI-2nd-Team-Project (`qtai-server`)
- 기준: 체크리스트 #27. audit 도메인(강태오 단독 소유). CLAUDE.md §5(관리자 권한)·§9

## 1. 배경

`AuditController`가 완전 스텁이라 감사 로그 조회 API(`GET /api/v1/audit`)가 미구현이었다. write(`WriteAuditLogUseCase`)·엔티티·테이블은 이미 존재. 조회(`ListAuditUseCase`)만 채운다.

## 2. 작업 범위

- `GET /api/v1/audit` — 행위자(actorType/actorId)·액션(actionType)·기간(from/to) 필터 + 페이징(최신순). ROLE_ADMIN 전용.
- 변경 전/후 원문(before/after json)은 목록 응답에서 제외(로그 양·상세 영역 자료).

## 3. 절차

1. `feature/audit-list-api` 브랜치(dev 기준). git index 손상 복구 후 클린 체크아웃.
2. `AuditLogResponse`(엔티티 필드 반영) + `AuditLogFilter` DTO + `ListAuditUseCase` 인터페이스 정의.
3. `AuditRepository.search`(null-safe @Query, 정렬은 Pageable 위임) 추가.
4. `AuditService`에 `ListAuditUseCase` 구현(`@Transactional(readOnly=true)`, 엔티티→DTO 매핑).
5. `AuditController` 구현 — `@PreAuthorize("hasRole('ADMIN')")`(AdminController 패턴), `@PageableDefault(sort=createdAt desc)`, `@DateTimeFormat` ISO 날짜.
6. 테스트: 서비스(필터 전달·매핑·null 필터) + 컨트롤러 슬라이스(200·필터 전달).

## 4. 정책 준수

- 관리자 전용: `@PreAuthorize("hasRole('ADMIN')")`(method security 활성). 일반 사용자 미노출.
- read 경로 `@Transactional(readOnly=true)`. api↔internal 경계 준수(매핑은 internal).

## 5. 검증

```powershell
cd qtai-server
.\gradlew.bat test --tests "*Audit*" --no-daemon
.\gradlew.bat test --no-daemon
```
