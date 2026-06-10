# PR 일괄 생성 · 머지 순서 가이드 (2026-06-06)

리뷰 액션아이템(P0/P1/P2) 수정 브랜치 32개를 origin에 push 하고, 각각 `dev` 대상 PR로 생성했다.
이 문서는 (1) 생성된 PR 전수 목록과 (2) 충돌 없이 머지하기 위한 권장 순서를 정리한다.

기준: 통합 건강검진 리포트 `2026-06-06_full-integration-healthcheck_report.md`
(전 브랜치 통합 빌드·테스트 그린, 충돌 4건은 rebase 스택으로 사전 해소 완료).

## 1. 핵심 요약

- 내 수정 브랜치 **32개** 전부 push 완료, **32개 PR** 생성 완료(모두 base=`dev`).
- (PR #250 `feature/20260604`는 팀원 기존 PR로 이번 작업 대상 아님.)
- 32개 중 **28개는 서로 독립**이라 아무 순서로 머지해도 충돌이 없다.
- 나머지 **4개는 선행 PR 위에 rebase(스택)** 되어 있어, 선행 PR을 먼저 머지해야 깔끔히 들어간다.
- `dev`/`master` 직접 push 없음. 모든 변경은 PR을 통해서만 `dev`로 들어간다.

## 2. 반드시 순서를 지켜야 하는 4개 (스택)

아래 4개는 "선행 PR"이 먼저 `dev`에 머지된 뒤 머지해야 한다. 순서대로만 하면 충돌은 0이다.

| 스택 PR | 선행 PR(이 먼저 머지) |
|---|---|
| #265 feature/notification-wiring | #260 → #269 → #283 |
| #277 fix/journal-events-outbox | #269 (마이그레이션 **V25**, 원래 V24였으나 #272의 V24 시드와 충돌 회피 위해 리넘버링) |
| #278 fix/member-nickname-trim | #280 |
| #286 refactor/qt-bible-sql-boundary | #266 → #258 → #267 → #256 |

## 3. 권장 머지 순서 (체인 먼저, 독립은 자유)

### A. 나눔·노트·알림 체인 (순서 고정)
1. #260 bugfix/sharing-comment-withdrawn-member
2. #269 feature/sharing-source-note-deleted
3. #283 fix/sharing-atomic-counters
4. #265 feature/notification-wiring *(260·269·283 이후)*
5. #277 fix/journal-events-outbox *(269 이후, V25 마이그레이션)*

### B. 회원 닉네임 체인 (순서 고정)
6. #280 fix/member-temp-nickname-length
7. #278 fix/member-nickname-trim *(280 이후)*

### C. QT·성경 체인 (순서 고정)
8. #266 feature/qt-passage-verse-mapping
9. #258 bugfix/qt-publication-gate
10. #267 feature/qt-study-availability
11. #256 bugfix/ai-seed-today-passage
12. #286 refactor/qt-bible-sql-boundary *(266·258·267·256 이후)*

### D. 독립 PR 20개 (아무 순서나 가능)
관리자/감사/신고: #255, #259, #268
Flutter: #257, #276
공통/보안/AI: #274, #282, #272, #271
스터디: #270, #285, #284
미션/기타: #281, #264, #275, #273, #279
정리(chore): #261, #262, #263

## 4. PR 전수 목록 (번호 순)

| PR | 브랜치 | 분류 | 의존 |
|---|---|---|---|
| #255 | bugfix/admin-role-db-verification | P0 | 독립 |
| #256 | bugfix/ai-seed-today-passage | P0 | C체인 |
| #257 | bugfix/flutter-platform-and-today-qt-parity | P0 | 독립 |
| #258 | bugfix/qt-publication-gate | P0 | C체인 |
| #259 | bugfix/report-audit-admin-role-db-verification | P0 | 독립 |
| #260 | bugfix/sharing-comment-withdrawn-member | P0 | A체인(선행) |
| #261 | chore/config-cleanup | P2 | 독립 |
| #262 | chore/remove-dead-dtos | P2 | 독립 |
| #263 | chore/remove-unwired-client-stubs | P2 | 독립 |
| #264 | chore/test-keys-dynamic | P1 | 독립 |
| #265 | feature/notification-wiring | P1 | **A체인: 260·269·283 후** |
| #266 | feature/qt-passage-verse-mapping | P0 | C체인(선행) |
| #267 | feature/qt-study-availability | P0 | C체인 |
| #268 | feature/report-resolution-actions | P0 | 독립 |
| #269 | feature/sharing-source-note-deleted | P0 | A체인(선행) |
| #270 | feature/study-simulator-glossary-publish | P1 | 독립 |
| #271 | fix/ai-checklist-admin-and-simulator-guard | P2 | 독립 |
| #272 | fix/ai-worker-stabilization | P1 | 독립(V24 시드) |
| #273 | fix/cache-wiring | P2 | 독립 |
| #274 | fix/common-exception-handlers | P1 | 독립 |
| #275 | fix/demo-compose-auth | P1 | 독립 |
| #276 | fix/flutter-auth-hardening | P1 | 독립 |
| #277 | fix/journal-events-outbox | P1 | **269 후, V25** |
| #278 | fix/member-nickname-trim | P2 | **280 후** |
| #279 | fix/member-reactivate-email-null-guard | P2 | 독립 |
| #280 | fix/member-temp-nickname-length | P1 | B체인(선행) |
| #281 | fix/mission-streak-aggregation | P1 | 독립 |
| #282 | fix/security-config-gaps | P1 | 독립 |
| #283 | fix/sharing-atomic-counters | P1 | A체인 |
| #284 | fix/study-hide-internal-aiassetid | P2 | 독립 |
| #285 | fix/study-simulator-column-validation | P2 | 독립 |
| #286 | refactor/qt-bible-sql-boundary | P2 | **C체인: 266·258·267·256 후** |

## 5. 머지 시 주의 사항

- 4개 스택 PR(#265, #277, #278, #286)은 선행 PR이 `dev`에 머지되기 전까지는 PR diff에 선행 브랜치 변경분이 함께 보인다. 선행이 머지되면 GitHub가 자동으로 자기 변경분만 남긴다. 정상이다.
- #277과 #272는 Flyway 마이그레이션 버전이 각각 V25, V24로 분리돼 있어 둘 다 머지돼도 버전 충돌이 없다.
- 자동 리뷰 봇이 PR마다 동작한다. REQUEST_CHANGES가 오면 해당 항목만 추가 커밋으로 대응한다(브랜치별 PR이라 영향 범위가 좁다).
- 머지는 PR을 통해서만 진행하고 `dev`/`master`에 직접 push 하지 않는다.

## 6. 남은 작업(이번 범위 밖)

건강검진에서 분류한 P2 후속(별도 진행 예정): 감사(audit) 잔결함, 결정 필요 항목, Flutter 잔결함, 문서 drift. MSA 물리 분리는 saga·이벤트 브로커·공유 DB 계획·Spring Modulith 도입이 선행돼야 하므로 현 시점 보류(상세는 건강검진 리포트 §MSA).
