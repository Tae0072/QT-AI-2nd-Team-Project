# 2026-05-27 서버 시작 실패 수정 — 결과 보고

## 요약
dev 브랜치에서 `bootRun` 시 발생하는 서버 시작 실패 건을 수정했다.
빈 이름 충돌, JWT 로컬 설정 누락, bible_books FK 데이터 부재, DDL 타입 불일치, V5 sharing DDL 보완을 해결했다.
Claude 자동 리뷰 BLOCK(plain key 커밋) / WARN(ddl-auto:update, refresh 30일) 반영 완료.

## 산출물

| 파일 | 설명 |
|------|------|
| `qtai-server/.../ai/client/qt/GetQtUseCaseMock.java` | `@Component("aiGetQtUseCaseMock")` 고유 이름 |
| `qtai-server/.../note/client/qt/GetQtUseCaseMock.java` | `@Component("noteGetQtUseCaseMock")` 고유 이름 |
| `qtai-server/.../resources/application-local.yml` | JWT 환경변수 참조 + ddl-auto: validate |
| `qtai-server/.../db/migration/V7__seed_bible_books.sql` | 성경 66권 seed 데이터 |
| `qtai-server/.../db/migration/V6__create_auth_ai_explanation_tables.sql` | layer TINYINT→INT |
| `qtai-server/.../db/migration/V5__create_sharing.sql` | sharing_posts/comments deleted_at 추가 |
| `qtai-server/src/test/resources/application.yml` | refresh-expiry-ms 30일→14일 정책 일치 |

## 검증
- `gradlew build -x test` — BUILD SUCCESSFUL
- `gradlew test` — BUILD SUCCESSFUL (전체 테스트 통과)

## 미해결 (타 팀원 파트)
- ai UseCase 구현체 미등록(`RegenerateAiAssetUseCase` 등) — ai 도메인(강상민)
- sharing/mission Mock 활성화 시 빈 이름 충돌 잠재 — 각 도메인 담당자 공유 필요
