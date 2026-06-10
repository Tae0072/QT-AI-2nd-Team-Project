# 워크플로우 — 수정 브랜치 일괄 push · PR 생성 (2026-06-06)

## 목표
리뷰 액션아이템(P0/P1/P2) 수정 브랜치 32개를 충돌 없이 `dev`로 통합할 수 있는 상태로 만들고,
팀 규칙(브랜치별 PR)에 맞춰 PR 32개를 생성한다.

## 진행 단계

1. **충돌 4건 사전 해소 (rebase 스택)**
   통합 검증에서 발견된 git 충돌 4쌍은 버그가 아니라 같은 파일 병렬 수정이었다. 선행 브랜치 위에 rebase 해서 충돌을 미리 풀었다.
   - fix/member-nickname-trim ← fix/member-temp-nickname-length 위 (MemberService: 예약 프리픽스 상수 + trim 로직 병합)
   - fix/journal-events-outbox ← feature/sharing-source-note-deleted 위 (NoteService: markSourceNoteDeletedUseCase + journalOutbox 동시 유지). 마이그레이션 V24→**V25** 리넘버링으로 #272 V24 시드와 충돌 회피.
   - feature/notification-wiring ← sharing 선행 3종 위 (CommentService: 탈퇴회원 닉네임 + 댓글 알림 동시 유지)
   - refactor/qt-bible-sql-boundary ← qt P0 4종 위 (QtPassageRepository/ImportService: P0-3 절매핑 유지 + 네이티브 range 쿼리 제거, QtPassageRangeView 삭제)
   각 rebase 후 도메인 테스트 + 전체 스위트 그린 확인.

2. **스캐폴딩 정리**
   rebase 베이스로 쓴 임시 병합 브랜치(tmp/sharing-prereq-base, tmp/qt-prereq-base) 삭제. 커밋은 스택 브랜치에 남아 유지됨. 커미터 신원 Tae0072 확인.

3. **push 인증 정리**
   첫 push가 Git Credential Manager 대화상자 대기로 멈춤 → `gh auth setup-git` + `GIT_TERMINAL_PROMPT=0`으로 비대화식 인증 전환 후 정상 push.

4. **전 브랜치 push**
   32개 브랜치 origin push 완료(0 실패).

5. **PR 32개 생성**
   `gh pr create --base dev`로 브랜치별 PR 생성. 본문에 건강검진 리포트 링크, 4개 스택 PR에는 머지 순서 메모, journal PR에는 V25 메모 포함. 전 PR base=`dev` 확인(master 대상 0).

## 검증
- 4개 rebase 각각 + 통합 스위트 `./gradlew test` BUILD SUCCESSFUL.
- `gh pr list` 결과 내 브랜치 32개 전부 open PR 존재, 전부 base=dev.
- 스택 4개 PR 본문에 선행 브랜치 메모 정상 표기 확인(#265, #277, #278, #286).

## 산출물
- 머지 순서 가이드: `reports/2026-06-06_pr-batch-merge-order_report.md`
- 통합 건강검진: `reports/2026-06-06_full-integration-healthcheck_report.md`

## 다음
가이드 §3 순서대로 PR 머지(체인 먼저). REQUEST_CHANGES는 브랜치별로 대응. P2 후속(audit·결정항목·Flutter·문서 drift)·MSA 물리 분리는 별도 진행.
