# PR 현황 갱신 + 머지순서 v2 (2026-06-06, 2차)

앞서 올린 PR들이 "거부/머지 반려"로 보였던 원인을 점검하고 전부 정리했다.
이 문서는 (1) 무엇이 문제였고 어떻게 고쳤는지, (2) 옛 PR→새 PR 매핑, (3) 갱신된 머지 순서,
(4) dev에 이미 머지된 팀 작업과의 중복 비교를 정리한다.

## 1. 진단 결과 — 실제로는 "거부" 0건

닫혀서 거부된 PR은 **원래 0건**이었다. 빨간불·머지 차단의 진짜 원인은 두 가지였다.

- **브랜치 이름 규칙 위반 16개**: CI `Branch Name Convention`(`pr-validation.yml`)은 허용 타입이 `feature/bugfix/hotfix/chore/release/docs/test`뿐인데, 내가 만든 `fix/*` 15개와 `refactor/*` 1개가 걸려 머지가 막혀 있었다. (봇 리뷰는 통과했지만 이 체크가 빨간불이라 "반려"처럼 보였다.)
- **dev 전진에 따른 qt 충돌 4개**: 내 `feature/qt-passage-verse-mapping`이 dev에 **#266으로 머지**되는 등 팀이 dev를 진행시키면서, 같은 qt 영역을 건드린 4개 브랜치가 충돌 상태가 됐다.

## 2. 조치 — 전부 정리 완료

- **이름 16개 재생성**: `fix/*`→`bugfix/*`(15), `refactor/qt-bible-sql-boundary`→`chore/qt-bible-sql-boundary`(1). GitHub 이름변경 API는 기존 PR을 닫아버려서, 올바른 이름으로 PR을 새로 만들고 옛 PR을 닫았다(동일 커밋). 그래서 PR 번호가 새로 부여됐다.
- **qt 4개 정리**: 최신 dev로 rebase. 이 과정에서 `ai-seed`(P0-2)는 dev에 #266을 통해 이미 반영된 것으로 확인되어(빈 커밋) **PR #256을 닫았다.** 나머지 `qt-publication-gate`/`qt-study-availability`/`qt-bible`은 충돌 없이 dev 위로 재정렬했다.
- **검증**: 정리 후 내 PR 30개 전부 `MERGEABLE`, `Branch Name`·`qtai-server Build & Test` 그린 확인.

## 3. 옛 PR → 새 PR 매핑

| 옛 PR | 새 PR | 브랜치(새 이름) |
|---|---|---|
| #271 | **#287** | bugfix/ai-checklist-admin-and-simulator-guard |
| #272 | **#288** | bugfix/ai-worker-stabilization |
| #274 | **#289** | bugfix/common-exception-handlers |
| #275 | **#290** | bugfix/demo-compose-auth |
| #276 | **#291** | bugfix/flutter-auth-hardening |
| #277 | **#292** | bugfix/journal-events-outbox (V25 마이그레이션) |
| #278 | **#293** | bugfix/member-nickname-trim |
| #279 | **#294** | bugfix/member-reactivate-email-null-guard |
| #280 | **#295** | bugfix/member-temp-nickname-length |
| #281 | **#296** | bugfix/mission-streak-aggregation |
| #282 | **#297** | bugfix/security-config-gaps |
| #283 | **#298** | bugfix/sharing-atomic-counters |
| #284 | **#299** | bugfix/study-hide-internal-aiassetid |
| #285 | **#300** | bugfix/study-simulator-column-validation |
| #273 | **#301** | bugfix/cache-wiring |
| #286 | **#302** | chore/qt-bible-sql-boundary |

- **닫음(중복)**: #256 ai-seed — dev #266에 이미 반영.
- **머지됨**: #266 qt-passage-verse-mapping.
- **번호 유지(이름 적합)**: #255, #257, #258(rebase), #259, #260, #261, #262, #263, #264, #265, #267(rebase), #268, #269, #270.

## 4. 갱신된 머지 순서 (새 번호 기준)

28개는 순서 무관. 아래 체인만 순서를 지킨다.

### A. 나눔·노트·알림
1. #260 bugfix/sharing-comment-withdrawn-member
2. #269 feature/sharing-source-note-deleted
3. #298 bugfix/sharing-atomic-counters
4. #265 feature/notification-wiring *(260·269·298 이후)*
5. #292 bugfix/journal-events-outbox *(269 이후, V25)*

### B. 회원 닉네임
6. #295 bugfix/member-temp-nickname-length
7. #293 bugfix/member-nickname-trim *(295 이후)*

### C. qt
8. #258 bugfix/qt-publication-gate
9. #267 feature/qt-study-availability *(258 이후)*
10. #302 chore/qt-bible-sql-boundary *(267 이후)*
- (#256 ai-seed는 닫힘, #266 절매핑은 dev에 머지 완료.)

### D. 독립 (아무 순서)
#255, #259, #268, #257, #291, #289, #297, #288, #287, #270, #300, #299, #296, #264, #290, #301, #294, #261, #262, #263

## 5. dev 머지본과의 중복 비교 — 전부 "유지(KEEP)"

dev에 이미 머지된 팀 작업과 주제가 겹쳐 보였던 5개를 내용으로 비교했다. **모두 다른 파일을 건드리는 상호보완이며, 닫을 중복이 아니다.**

| PR | 주제 | dev 머지본 | 판정 |
|---|---|---|---|
| #294 member-reactivate | 재활성 시 email/프로필 null-guard | #248 재가입·재활성화(F-01) | **유지(개선)**: #248의 `reactivate()`는 카카오가 이메일·프로필을 안 줄 때 null로 덮어써 기존 값을 잃는다. #294가 그 null-guard를 더한다. **#248 머지 후 머지 권장.** |
| #257 flutter-platform | Android/iOS 매니페스트·Today QT 파리티 | #251 Flutter 로그아웃 루프(F-01) | **유지**: 파일 겹침 없음(다른 화면·플랫폼 설정). |
| #291 flutter-auth | auth/error 인터셉터 | #251 동일 | **유지**: 파일 겹침 없음(네트워크 인터셉터). |
| #262 remove-dead-dtos | 죽은 DTO 제거(AiCallLog·Member·Qt) | #249/#254 죽은 코드 제거 | **유지**: 제거 대상이 서로 다름. |
| #263 remove-unwired-stubs | 미연결 client Mock·Kakao·Praise 제거 | #249/#254 동일 | **유지**: 제거 대상이 서로 다름. |

참고: 이 5개(및 다른 비-qt 브랜치)는 옛 dev 기반이라 #266을 포함하지 않지만, 3-way 머지로 #266을 되돌리지 않으며 GitHub PR diff에는 실제 변경만 깔끔히 표시된다.

## 6. 남은 권고

- 머지는 §4 순서대로 PR로만(`dev`/`master` 직접 push 금지).
- 자동 리뷰 봇 REQUEST_CHANGES는 해당 PR에 추가 커밋으로 대응.
- P2 후속(audit·결정항목·Flutter 잔결함·문서 drift)·MSA 물리 분리는 별도 진행(건강검진 리포트 참고).
