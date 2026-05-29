# 2026-05-21 서버 공통 기반 및 Entity/DDL 구축

## 목표
QT-AI 서버의 모든 도메인이 공유하는 공통 인프라(응답 패턴, 예외 처리, Security, Cache, Jackson 설정)와 핵심 Entity/DDL(Bible, QT, Note, Sharing)을 한 번에 구축한다. 이후 Phase에서 각 도메인 API를 올릴 때 공통 기반 없이 빌드가 깨지는 상황을 방지한다.

## 작업 내용
1. **공통/인프라 10파일** — BaseEntity(@MappedSuperclass), GlobalExceptionHandler(traceId 반환), SecurityConfig(CSRF off/stateless), RedisConfig, CacheConfig(Caffeine L1), JacksonConfig(Asia/Seoul), JpaAuditingConfig, WebConfig(CORS localhost 명시 허용)
2. **프로파일별 application.yml 4파일** — 공통, local(H2), dev(MySQL), prod(환경변수 주입)
3. **도메인 Entity** — BibleBook/BibleVerse(KRV+KJV 병합), QtPassage/QtPassageVerse, Note(4섹션 body + activeUniqueKey soft delete), SharingPost(인라인 스냅샷)/Comment/PostLike
4. **Flyway V1~V5** — members, bible_tables, qt_tables, note_tables, sharing_tables
5. **기존 Entity 정리** — Share/ShareSnapshot `@Deprecated`, QtRepository `@Deprecated`

## 범위
- 브랜치: `feature/common-foundation`
- 변경 규모: 43 files (수정 13 + 신규 30)
- PR Guard 리뷰: Round 2 완료 (BLOCK 8건 + WARN 17건 모두 해결)
- 테스트: JpaEntityDdlTest 7건, GlobalExceptionHandlerTest 3건, EntityCompilationTest

## 미해결
- 각 도메인 API Controller/Service는 후속 Phase에서 구현

## 담당
- DevD 이승욱
