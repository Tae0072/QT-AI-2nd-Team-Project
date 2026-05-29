# 2026-05-29 Flyway H2 호환 수정

## 목표
dev 브랜치에서 서버 기동 시 V13 Flyway 마이그레이션 H2 비호환 문법 에러를 해결한다.

## 작업 내용
1. **V13 H2 호환 수정** — `ALTER TABLE sharing_posts ADD COLUMN ..., ADD COLUMN ...` 콤마 연결 문법이 H2에서 미지원. 각각 별도 `ALTER TABLE` 문으로 분리하여 MySQL/H2 모두 호환되는 형태로 변경

## 범위
- 브랜치: `hotfix/flyway-h2-compat`
- PR: (Open, 리뷰 대기)
- 커밋: `fix(server): Flyway V13 H2 호환 수정`
- 변경: 1파일 (`V13__add_nickname_snapshot_to_sharing_posts.sql`)
- 비고: 누락 DDL(admin/mission/reports)은 #145~#150 후속 PR에서 V16~V19로 이미 반영 완료

## 미해결
- PR 머지 대기

## 담당
- Lead 강태오 (T)
