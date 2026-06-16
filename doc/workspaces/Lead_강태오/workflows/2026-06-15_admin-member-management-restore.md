# 관리자 회원 관리(AD-13) 복원·재배선 (2026-06-15)

## 1. 배경
관리자웹 회원 관리 기능이 다른 세션 WIP에 있었으나 dev 머지에 포함되지 않았다(배경음악 관리 #666 등만 머지됨).
이전 작업물은 `git pull` 충돌 정리 때 만든 `stash@{0}`에 보존돼 있었다. 이를 꺼내 현재 dev에 맞춰 복원했다.

## 2. 핵심 결정
- 회원 상세는 **신고·나눔 통계**(MemberReportStatsUseCase, MemberSharingStatsUseCase)에 의존한다.
  - 신고 통계 구현체 `ReportStatsService`는 독립적이라 그대로 가져왔다.
  - 나눔 통계는 원본이 `AdminSharingService`(AD-13 나눔 운영 전체, 미머지)에 묶여 있어 통째로 가져오면
    다른 미머지 기능까지 끌려온다. 그래서 **가벼운 전용 구현 `MemberSharingStatsService`를 새로 작성**해
    나눔 운영 기능과 분리했다.
- 메뉴 코드: dev에서 AD-12=배경음악, AD-14=시뮬레이터로 이미 사용 중이라 회원 관리는 **AD-13**으로 배정.

## 3. 변경 내용
### 백엔드(admin-server)
- 신규: `member/api`(ListMembers/GetMemberDetail/UpdateMemberStatus UseCase + DTO 3종),
  `member/internal/AdminMemberService`, `member/web/AdminMemberController`,
  `report/api/MemberReportStatsUseCase`, `report/internal/ReportStatsService`,
  `sharing/api/MemberSharingStatsUseCase`, `sharing/internal/MemberSharingStatsService`(신규 작성),
  테스트 `member/web/AdminMemberControllerTest`.
- 수정(가산): `Member`(suspendByAdmin/activateByAdmin), `MemberRepository`(searchForAdmin),
  `ReportRepository`(countByReporterMemberId/countByTargetTypeAndTargetIdIn),
  `SharingPostRepository`(countByMemberId/findIdsByMemberId), `CommentRepository`(findIdsByMemberId).
- API: `GET /api/v1/admin/members`(목록·검색), `GET /{id}`, `GET /{id}/detail`, `PATCH /{id}/status`(정지/해제).

### 프런트(admin-web)
- 신규: `pages/MembersPage.tsx`, `api/members.ts`.
- 배선: `App.tsx`에 `/members` 라우트, `constants/menu.ts`에 AD-13 회원 관리(OPERATOR) 메뉴.

## 4. 검증
- admin-server: `:admin-server:compileJava/compileTestJava` 성공, `:admin-server:test` 전체 BUILD SUCCESSFUL(경계/ArchUnit 포함).
- admin-web: `tsc --noEmit` 무오류, `npm run build` 성공, 계약 테스트 전체 PASS.

## 5. 한계/후속
- 회원 상세의 나눔/신고 통계는 전용 경량 구현으로 동작한다. 추후 나눔 운영(AD-13 원안)·신고 운영이
  정식 머지되면 통계 출처를 그쪽으로 일원화할지 검토.
- 메뉴 코드 AD-13은 과거 WIP에서 "나눔 공유글 관리"로 쓰던 번호다. 나눔 운영 머지 시 코드 충돌만 조정(라우팅은 path 기준이라 기능 영향 없음).
