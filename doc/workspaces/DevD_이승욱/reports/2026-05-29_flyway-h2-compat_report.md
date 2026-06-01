# 2026-05-29 Flyway H2 호환 수정 — 결과 보고

## 요약
dev 브랜치에서 `bootRun` 실행 시 V13 마이그레이션의 H2 비호환 문법(`ALTER TABLE ... ADD COLUMN` 콤마 연결)으로 Flyway 실패하던 문제를 해결했다. 누락 DDL(admin, mission, reports)은 #145~#150 등 후속 PR에서 별도 마이그레이션(V16~V19)으로 이미 반영 완료되어 본 hotfix에서는 V13 수정만 포함한다.

## 산출물

| 파일 | 설명 |
|------|------|
| `V13__add_nickname_snapshot_to_sharing_posts.sql` | `ADD COLUMN` 콤마 연결 → 각각 분리 (H2 호환) |

## 원인 분석

| 에러 | 원인 PR | 담당 |
|------|---------|------|
| V13 `ALTER TABLE ... ADD COLUMN x, ADD COLUMN y` H2 문법 에러 | #135 feat(sharing) | rmfdnjf98 |

## 검증
- `gradlew bootRun` — 정상 기동 확인 (Flyway V1~V19 전체 적용 + Hibernate schema validation 통과)
- 금지 기술/데이터 — 위반 없음
- PR Guard — 브랜치명·커밋·변경 범위(1파일/4줄) 통과

## 미해결
- PR 머지 대기
