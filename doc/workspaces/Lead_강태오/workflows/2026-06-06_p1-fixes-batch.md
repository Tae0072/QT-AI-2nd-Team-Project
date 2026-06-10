# 2026-06-06 P1 (High) 일괄 수정 워크플로우

코드 리뷰 액션아이템 **P1 13건**을 `dev` 분기 독립 브랜치로 구현·커밋한 작업의 머지 가이드.
결과 요약: `reports/2026-06-06_p1-fixes-batch_report.md`

## 0. 역할/전제

- 구현·커밋: 본 작업에서 완료. **push·PR 생성은 사용자(T)가 직접 수행.**
- 기준 브랜치: 모든 작업 브랜치는 `origin/dev`에서 분기. PR 대상은 `dev`.
- 커밋 규칙: Conventional Commits, 기능 항목은 F-ID 명시(해당 시).

## 1. 브랜치 → 항목 매핑(커밋)

| 브랜치 | 항목 | 커밋 | 비고 |
|--------|------|------|------|
| `fix/common-exception-handlers` | P1-1, P1-8 | f56e175 | 공통 예외 레이어 — 가장 먼저 머지 권장(타 도메인 응답 일관성 기반) |
| `fix/sharing-atomic-counters` | P1-2 | 558fee0 | **P1-13보다 먼저 머지(스택 베이스)** |
| `feature/notification-wiring` | P1-13 | b42937a | **P1-2 위에 스택** — sharing 서비스 파일 공유 |
| `fix/ai-worker-stabilization` | P1-3 | f15fe5f | 독립 |
| `fix/member-temp-nickname-length` | P1-4 | 5ba9796 | 독립 |
| `chore/test-keys-dynamic` | P1-5 | 7ba8ed0 | 독립(테스트 키 정리) |
| `fix/demo-compose-auth` | P1-6 | b4ce4ff | 독립(infra/compose·프로파일) |
| `fix/security-config-gaps` | P1-7 | 9910923 | 독립(security 구성) |
| `fix/mission-streak-aggregation` | P1-9 | 402abe1 | 독립(note 달력 + mission) |
| `fix/journal-events-outbox` | P1-10 | 43cd3e0 | 독립(note journal) |
| `feature/study-simulator-glossary-publish` | P1-11 | ba5da06 | 독립(study) |
| `fix/flutter-auth-hardening` | P1-12 | bddba1e | 독립(flutter-app) |

## 2. 의존/충돌 분석

- **유일한 스택 의존**: `feature/notification-wiring`(P1-13)는 `fix/sharing-atomic-counters`(P1-2) 위에서 작업됨 — 두 브랜치가 `SharingPostService`/`CommentService`를 함께 건드린다. **반드시 P1-2를 먼저 머지**한 뒤 P1-13을 머지/리베이스.
- note 도메인 2건(P1-9, P1-10)은 **서로 다른 파일**을 수정 → 충돌 없음.
  - P1-9: `MeditationCalendarService`, `NoteRepository`, mission `MemberMissionProgress`/`MissionProgressCalculator`(+테스트).
  - P1-10: `NoteService`, `JournalEvent*`(엔티티/리포지토리/아웃박스/재처리기/딜리버리), `V24` 마이그레이션, `NoteServiceTest`/`JpaEntityDdlTest`(+신규 테스트), `MysqlMigrationValidationTest` 1줄.
- 나머지는 도메인이 갈라져 파일 교집합 없음 → 머지 순서 자유(아래 권장 순서는 리뷰 부담 최소화용).

## 3. 권장 머지 순서

1. `fix/common-exception-handlers` (P1-1·P1-8) — 공통 응답 규칙 먼저.
2. `fix/sharing-atomic-counters` (P1-2)
3. `feature/notification-wiring` (P1-13) — 2의 위.
4. `fix/security-config-gaps` (P1-7)
5. `chore/test-keys-dynamic` (P1-5)
6. `fix/demo-compose-auth` (P1-6)
7. `fix/ai-worker-stabilization` (P1-3)
8. `fix/member-temp-nickname-length` (P1-4)
9. `fix/mission-streak-aggregation` (P1-9)
10. `fix/journal-events-outbox` (P1-10)
11. `feature/study-simulator-glossary-publish` (P1-11)
12. `fix/flutter-auth-hardening` (P1-12)

각 PR 본문에는 해당 항목과 F-ID(P1-9=F-13, P1-10=F-03), 리뷰 리포트 참조(H-12/H-13 등)를 적는다.

## 4. 머지 전/후 검증

각 브랜치(또는 dev 통합 후) 서버 변경 범위에 맞춰 실행:

```bash
# Windows 로컬(JDK21)
$env:JAVA_HOME='D:\workspace\tools\jdk\jdk-21.0.11+10'
cd D:\workspace\QT-AI-2nd-Team-Project\qtai-server
Remove-Item build\test-results -Recurse -Force -ErrorAction SilentlyContinue   # Windows 파일락 회피
.\gradlew.bat --no-daemon test
```

- 본 배치 현 시점 전체 스위트: **828 tests / 0 failures / 4 skipped**(skipped는 Docker 필요 테스트).
- Docker 환경(CI)에서는 `MysqlMigrationValidationTest`가 실 MySQL 8.0에 V1~V24 Flyway 적용 + Hibernate `validate`로 엔티티↔스키마 정합을 가드(V24 신규 컬럼 검증 포함).
- 전 브랜치 머지 후 `dev`에서 전체 테스트 1회 재실행 권장.

## 5. 주의

- `.gradle`, build output, coverage HTML, generated report, 임시 파일은 stage 금지(CLAUDE.md §12).
- P1-10 PR은 16 files로 권장치 상회 — 리포트의 "품질 게이트 메모" 참고해 리뷰어에 사전 공유.
- 브랜치 전환 시 IDE/Git이 `origin/dev` 기준 파일을 보여주는 것은 정상(각 커밋은 브랜치에 보존됨).
