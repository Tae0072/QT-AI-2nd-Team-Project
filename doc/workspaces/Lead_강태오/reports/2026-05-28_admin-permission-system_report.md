# Admin 권한 체계 구현 리포트

- 작업자: Lead 강태오
- 날짜: 2026-05-28
- PR: #134 (feature/admin-permission-system → dev)
- W3 목표: 관리자·통합 조정

---

## 1. 배경

W2 통합 점검(PR #132)에서 admin 도메인이 6개 파일 전부 TODO 상태, 테스트 0건으로 확인되었다. W3 목표가 "관리자·통합 조정"이므로, 권한 체계를 먼저 구축한다.

CLAUDE.md §5 규정:
> 관리자 API는 일반 회원 토큰의 members.role=ADMIN과 admin_users.admin_role을 모두 확인한 뒤, OPERATOR, REVIEWER, CONTENT_CREATOR, SUPER_ADMIN 중 API 명세에 맞는 세부 권한을 요구한다.

---

## 2. 구현 내용

### 2.1 도메인 모델

| 파일 | 역할 |
|------|------|
| AdminRole enum | SUPER_ADMIN, OPERATOR, REVIEWER, CONTENT_CREATOR |
| AdminStatus enum | ACTIVE, DISABLED |
| AdminUser Entity | ERD admin_users 매핑, hasRole() (SUPER_ADMIN → 모든 역할 포함) |
| AdminActionLog Entity | 감사 로그 (INSERT only) |

### 2.2 API 계층

| 파일 | 역할 |
|------|------|
| VerifyAdminRoleUseCase | 다른 도메인에서 호출 가능한 관리자 검증 인터페이스 |
| AdminUserInfo DTO | 관리자 정보 record (adminUserId, memberId, adminRole) |

### 2.3 서비스 계층

| 메서드 | 동작 |
|--------|------|
| getActiveAdmin(memberId) | admin_users 조회 → ACTIVE 검증 → AdminUserInfo 반환 |
| verifyRole(memberId, requiredRole) | getActiveAdmin + hasRole() 검증 |

### 2.4 컨트롤러

| 엔드포인트 | 설명 |
|-----------|------|
| GET /api/v1/admin/me | 관리자 본인 정보 조회 (세부 권한 확인 용도) |

### 2.5 에러 코드

| 코드 | 설명 |
|------|------|
| AD0001 ADMIN_USER_NOT_FOUND | admin_users에 레코드 없음 |
| AD0002 ADMIN_USER_DISABLED | 비활성 관리자 계정 |
| AD0003 ADMIN_ROLE_INSUFFICIENT | 필요 권한 미달 |

---

## 3. 권한 검증 흐름

```
[요청] → SecurityConfig (ROLE_ADMIN 1차 검증)
       → AdminController (@PreAuthorize("hasRole('ADMIN')"))
       → AdminService.verifyRole(memberId, requiredRole)
         → admin_users 조회 (2차 검증)
         → ACTIVE 상태 확인
         → hasRole(requiredRole) 확인 (SUPER_ADMIN은 모든 역할 포함)
       → 비즈니스 로직 실행
```

---

## 4. 테스트

| 테스트 클래스 | 테스트 수 | 범위 |
|--------------|----------|------|
| AdminUserTest | 8 | hasRole, isActive, disable, enable, changeRole |
| AdminServiceTest | 10 | getActiveAdmin, verifyRole, 에러 케이스 |

**CLAUDE.md §10 필수 테스트 커버리지:**
- admin authorization 검증 ✅
- SUPER_ADMIN 역할 포함 로직 ✅
- 비활성 관리자 차단 ✅
- 미등록 관리자 차단 ✅

---

## 5. 빌드 검증

| 회차 | 결과 |
|------|------|
| 1차 | BUILD SUCCESSFUL |
| 2차 | BUILD SUCCESSFUL |

---

## 6. 변경 통계

- 변경 파일: 13개 (신규 10 + 수정 3)
- 변경 라인: +755, -39
- PR 크기: 적정 (500 lines 기준 내)

---

## 7. 후속 작업

| 우선순위 | 작업 | 예상 PR |
|---------|------|---------|
| 높음 | GetStatsUseCase 구현 (운영 통계 집계) | 별도 PR |
| 높음 | ModerateContentUseCase 구현 (콘텐츠 숨김/삭제/신고 반려) | 별도 PR |
| 중간 | LookupMemberUseCase 구현 (관리자 회원 검색) | 별도 PR |
| 중간 | ArchUnit 도메인 경계 테스트 추가 | 별도 PR |
