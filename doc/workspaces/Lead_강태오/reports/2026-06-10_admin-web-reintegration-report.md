# 리포트 — admin-web 재취합 + SSoT 동기화 (2026-06-10)

- 작성자: Lead 강태오
- 상태: 완료(재취합·동기화), 후속 2단계 대기

## 결과 요약

| 항목 | 결과 | 비고 |
|---|---|---|
| admin-web 재취합 | ✅ dev `86c48af` (#462 merge) | revert-the-revert로 #459 복원(76 files) |
| #459 차단 ① Lead 승인 | ✅ 충족 | #462 승인 코멘트 |
| #459 차단 ② SSoT 동기화 | ✅ 충족 | 문서저장소 #19 → master `1bbbb2a` |
| #462 자동리뷰 | ✅ APPROVE | 본문 끝 판정·정식 APPROVED 직접 확인 |
| 04_API §4.7.2 캐논 정합 | ✅ | status 5종·요청본문·04:00 KST 주석 |

## 머지 내역

- 구현 `QT-AI-2nd-Team-Project`
  - #462 `chore/restore-pr459-admin-web` → dev (merge `86c48af`)
  - 동반: #461(브랜치명 규칙으로 closed), #460 revert는 #462로 복원
- 문서 `2nd-Team-Project`
  - #19 `docs/api-04-7-2-canon-sync` → master (merge `1bbbb2a`)

## 복원/동기화 핵심 내용

- 관리자 웹 카카오 JS SDK 로그인(F-01/02/03)
- admin-web base URL 8090(admin-server) 정합
- `CLAUDE.md` §5 관리자 카카오 경로 `POST /api/v1/admin/auth/kakao`
- 04_API §4.7.2: 요청본문 `bookId/chapter/startVerse/endVerse/mainVerseRef`, 상태값 5종, 04:00 KST 노출 배치 주석

## 검증 로그

- dev: `kakao.ts` 존재, `.env.example` 8090, `CLAUDE.md` §5 2건, 04_API conflict marker 0
- 문서 master: `status=active`·`mainVerseRef`·04:00 KST 주석 grep 확인
- 잔여 브랜치(revert/chore/docs-sync)·워크트리 정리 완료, 팀원 WIP 보존

## 남은 작업 (마무리)

1. 모놀리식 원본(`qtai-server/src`) 제거 → dev (step 3, 되돌리기 부담 큼 → 단계별 확인)
2. dev → master 승격 (step 4)
3. (후속 가드) 백엔드 admin/auth/kakao·qt-passages 요청본문 PR에서 테스트 누락 금지
