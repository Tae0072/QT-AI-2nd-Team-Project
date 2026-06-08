# 2026-06-05 P0 Critical 일괄 수정 워크플로우 (리뷰 후속 1차)

전체 코드 리뷰(2026-06-05_full-codebase-review)에서 식별된 P0 9건을 Lead 단독으로 일괄 수정.
조치 계획서: `reports/2026-06-05_full-codebase-review_action-items.pdf`

## 작업 환경 (이번에 구축)

- Windows 네이티브 도구 체인 신규 구축: `D:\workspace\tools\`
  - MinGit 2.54.0 (`mingit\cmd\git.exe`) — git for Windows 미설치 + 샌드박스 마운트가 git 메타파일(rename-replace)을 안정적으로 못 읽는 문제 때문에 모든 git 조작을 네이티브로 수행
  - Temurin JDK 21 (`jdk\jdk-21.0.11+10`) — `gradlew.bat`로 실저장소 빌드/테스트
- `git config --global safe.directory` 2건 등록(소유자 SID 불일치 해소)
- 빌드 잔재(build/.gradle — 이전 세션 소유권 문제) takeown 후 정리

## 진행 절차 (이슈 공통)

1. origin/dev 기준 브랜치 생성(연관 이슈는 체인으로 스택)
2. 코드 수정 + 회귀 테스트 추가 → `gradlew test`(전체 또는 영향 도메인) 그린 확인
3. Conventional Commit (한국어 본문, 결정·근거 명시)
4. push/PR은 Lead가 GitHub에서 진행(자동 머지 파이프라인)

## 브랜치 체인과 머지 순서

```
[admin 체인]   8acb277 bugfix/admin-role-db-verification            (P0-1a: ai web)
            └─ 47bc1a1 bugfix/report-audit-admin-role-db-verification (P0-1b: report/audit web)
               └─ 57a280e feature/report-resolution-actions           (P0-7: 신고 후속 3종)

[qt 체인]      8ebec2d bugfix/ai-seed-today-passage                 (P0-2: 시딩 날짜)
            └─ eda6a2b feature/qt-passage-verse-mapping              (P0-3: 절 매핑+백필)
               └─ 85a7bd6 bugfix/qt-publication-gate                  (P0-8: 공개 게이트)
                  └─ b9df400 feature/qt-study-availability            (P0-4: Today QT 연동)

[sharing 체인] 1c8fd01 bugfix/sharing-comment-withdrawn-member      (P0-5: 댓글 404+N+1)
            └─ b4e0f21 feature/sharing-source-note-deleted           (P0-6: 노트삭제↔나눔)

[flutter]      48d465b bugfix/flutter-platform-and-today-qt-parity  (P0-9)
```

- 체인 내부는 순서대로 PR→머지(스택 PR — 선행 머지 후 후행은 차분만 남음).
- 체인 간 파일 충돌 없음(확인 완료). 단 **모든 체인 머지 후 dev에서 전체 테스트 1회 필수**(조합 검증).
- 브랜치 타입은 CI pr-validation 허용 목록(bugfix/feature)에 맞춤.

## 검증 기록

- 서버: 체인별 영향 도메인 테스트 + 대표 시점 2회 전체 스위트 **836 tests / 0 fail**
  (admin 체인 시점 1회, qt 체인 완성 시점 1회). 로그: `D:\workspace\tools\test*.log`
- 신규 회귀 테스트: 실JWT 종단 인가 6, verifyAnyRole 6, 시딩 날짜 1+α, 절 매핑 5,
  공개 게이트 4, 가용성 3+2, 댓글 폴백 2, 원본삭제 3+2, 신고 후속 3, Flutter 파싱 2
- Flutter: SDK 부재 환경이라 dart 분석/테스트 미실행 — 기존 테스트 픽스처와 수동 정합
  확인 완료, **CI(claude-pr-review의 dart 포함 리뷰)와 팀원 로컬에서 검증 필요**

## Lead 결정 기록 (코드에 반영됨)

1. 관리자 2차 권한: **DB 검증 방식**(VerifyAdminRoleUseCase) 통일 — DISABLED 즉시 반영
2. P0-6 정책: **유지+안내 표시**(명세안) — HIDDEN 자동 전환안(엔티티 주석) 폐기
3. SU 수집 00:05→**00:02** 이동 — 00:05 시딩(§6 고정)과 같은 분 경합 해소
4. 본문 부재 시 simulatorStatus **DISABLED→MISSING** 정정(§6 4값 의미 정합)
5. 신고 후속: 숨김은 RESOLVED+HIDE_TARGET+POST 한정(같은 tx), 알림 실패는 비차단(멱등 eventKey)
