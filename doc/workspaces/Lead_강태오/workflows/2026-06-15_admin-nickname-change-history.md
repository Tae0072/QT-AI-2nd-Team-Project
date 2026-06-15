# 닉네임 변경 이력 (F-04/F-10) — PR② (2026-06-15)

## 1. 배경
회원 상세에서 닉네임 "변경 이력"을 보고 싶다는 요청. 기존엔 마지막 변경 시각(`nicknameChangedAt`)만 있어
이력 목록이 불가했다. → 이력 추적 인프라를 신설(앞으로의 변경부터 누적, 과거분은 없음).

## 2. 구조 (원본=service-user, 조회=admin-server, 스키마=admin-server Flyway)
- 스키마: `V49__create_nickname_change_history.sql` (admin-server 단독 소유). append-only 테이블
  (member_id, old_nickname, new_nickname, changed_at, created_at, idx(member_id, changed_at)).
- 엔티티 `NicknameChangeHistory` + 리포지토리: 양쪽 서비스에 매핑(같은 단일 DB).
  - service-user: 저장 전용. `MemberService.changeNicknameInternal`에서 변경 성공 후 같은 트랜잭션에 1행 기록
    (old=변경 전 닉네임, new=변경 후, changed_at=clock).
  - admin-server: 조회 전용. `findByMemberIdOrderByChangedAtDesc`(페이징).
- admin-server 조회 경로: `member.api.ListNicknameHistoryForAdminUseCase`(+`NicknameHistoryItem`) /
  `NicknameHistoryQueryService`(impl) / `AdminMemberController.GET /{id}/nickname-history`(OPERATOR).
- 프런트: `api/members.ts`에 `listMemberNicknameHistory`, `MembersPage` 상세에 "닉네임 이력" 탭(이전→이후, 시각).

## 3. 검증
- admin-server: compile + `:admin-server:test` 전체 BUILD SUCCESSFUL.
- service-user: compile + `:service-user:test` 전체 BUILD SUCCESSFUL.
  - 기록 추가로 `MemberServiceTest`에 `NicknameChangeHistoryRepository` @Mock 보강(연속 변경 성공 경로 NPE 수정).
- admin-web: tsc 무오류 + build + 계약 테스트 PASS.

## 4. 참고/한계
- 이력은 이번 배포 이후의 닉네임 변경부터 쌓인다(과거 변경분은 기록이 없어 표시되지 않음).
- 관리자 화면은 변경 시각·이전/이후 닉네임만 노출(개인정보 추가 노출 없음).
