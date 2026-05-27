# 2026-05-27 서버 시작 실패 수정

## 목표
dev 브랜치에서 서버(bootRun)가 시작되지 않는 문제 4건을 수정한다.

## 원인 분석
여러 팀원의 PR이 dev에 merge되면서 통합 시 발생한 문제들이다.

1. **빈 이름 충돌** — ai, note 도메인이 동일 클래스명 `GetQtUseCaseMock`을 `@Component`로 등록
2. **JWT 로컬 키 누락** — `application-local.yml`에 RS256 키 설정이 없어 JwtProvider 초기화 실패
3. **bible_books seed 미존재** — V8 sample qt_passages가 FK로 참조하는 bible_books 데이터 부재
4. **TINYINT↔int 타입 불일치** — V6 DDL `TINYINT` vs Entity `int` → Hibernate validate 실패

## 범위
- `GetQtUseCaseMock.java` 2건 — `@Component` 고유 빈 이름 부여
- `application-local.yml` — JWT 테스트 키 추가, ddl-auto: update
- `V7__seed_bible_books.sql` — 성경 66권 seed 데이터
- `V6__create_auth_ai_explanation_tables.sql` — layer 컬럼 TINYINT→INT

## 미해결 (타 팀원 파트)
- `comments.deleted_at` DDL 누락 — sharing 도메인(이승욱)
- ai UseCase 구현체 미등록 — ai 도메인(강상민)

## 담당
- Lead 강태오
