# 워크플로우 — admin-web 재취합 + 04_API SSoT 동기화 (2026-06-10)

- 작성자: Lead 강태오
- 관련 PR: 구현 `QT-AI-2nd-Team-Project` #462 / 문서 `2nd-Team-Project` #19
- 관련 F-ID: F-06, F-01/02/03
- 기준 문서: `CLAUDE.md` §2/§5, `04_API_명세서` §4.7.2, `09_Git_규칙`

## 1. 배경

dev-admin-web 취합(#459)이 dev에 머지됐다가 #460으로 revert됐다. revert 사유는 claude-review의 **REQUEST_CHANGES** 판정이며, 차단 항목은 다음 2건이었다.

1. SSoT 변경(`CLAUDE.md` §5 관리자 카카오 경로)에 대한 **Lead 명시 승인 코멘트** 누락
2. **04_API 캐논(문서저장소) 동기화** 후속 약속 누락

이전 머지는 자동리뷰 본문 끝 판정을 끝까지 확인하지 않고 GitHub 리뷰 상태값(COMMENTED)만 보고 진행한 실수였다. 본 작업은 그 두 차단 항목을 정식으로 충족한 뒤 재취합하는 것을 목표로 한다.

## 2. 핵심 기술 판단 — revert-the-revert

`dev-admin-web`은 #459 머지 후 #460 revert로 인해 이미 `dev` 이력의 조상이 되어 있었다(`git rev-list --count origin/dev..origin/dev-admin-web == 0`). 따라서 일반 재머지는 no-op이다.

→ revert 커밋(`b7c290b`)을 다시 되돌리는 **revert-the-revert**로 #459 변경분(76 files)을 복원했다. 이 방식은 #459의 04_API 충돌 해소본까지 그대로 복원하므로 충돌이 재발하지 않는다(머지 후 conflict marker 0 검증).

## 3. 절차

1. `origin/dev`(revert 반영 상태 `acffa31`)에서 작업 브랜치 생성
2. `git revert b7c290b` → admin-web 복원(카카오 로그인·8090 정합·CLAUDE.md §5·04_API §4.7.2)
3. 브랜치명 규칙(`revert/` 미허용) 위반으로 `chore/restore-pr459-admin-web`로 재생성
4. PR #462 생성, 본문에 차단 2건 해소 명시
   - Lead 승인 코멘트 첨부(차단 ①)
   - 04_API 캐논 동기화 후속 PR 약속(차단 ②)
5. CI 그린 + claude-review 본문 끝 판정 **APPROVE** 직접 확인 후 머지(`86c48af`)
6. 후속: 문서저장소 #19로 04_API §4.7.2 캐논 동기화(04:00 KST 노출 주석 포함) → master `1bbbb2a`
7. 잔여 브랜치·워크트리 정리

## 4. 검증

- dev에 admin-web 복원: `kakao.ts` OK, `.env.example` 8090 OK, `CLAUDE.md` §5 2건, 04_API marker 0
- #462 claude-review: 정식 `APPROVED` + 본문 끝 `APPROVE`, 현재 차단 항목 없음(INFO 내 "향후 백엔드 PR 테스트 누락 시 BLOCK" 경고만)
- 문서저장소 master: `status=active`, `mainVerseRef`, 04:00 KST 주석 반영 확인

## 5. 교훈 / 후속 가드

- 자동리뷰는 **GitHub 상태값이 아니라 본문 끝 최종 판정(APPROVE/REQUEST_CHANGES)** 까지 확인한 뒤 머지한다.
- 머지 revert 후 재취합은 **revert-the-revert** 또는 새 통합 브랜치를 사용한다(일반 재머지 no-op 주의).
- 후속 백엔드 PR(service-user `admin/auth/kakao`, admin-server qt-passages 요청본문)에서 검증 테스트 누락 금지.
- 남은 마무리: 모놀리식 원본 제거(step 3) → dev → master 승격(step 4).
