# 2026-06-09 MSA Phase 1 — bible DB-per-service (Inc4)

## 목표
bible 데이터 소유를 bible-service 전용 DB로 이전한다. ① bible-service DB에 시드(books/verses) 이전 ② 모놀리식의 bible_verses 참조 **크로스 도메인 FK 4개 제거**(DB 간 FK 불가). bible_verse_id는 제약 없는 참조 값으로 유지(앱/서비스 경계 무결성).

## 배경
- DB 분리 승인(2026-06-09). 조사 결과 bible_verses 참조 FK가 **4개**(요약 §4-3의 glossary_terms 외 qt/note/ai 포함) — 4개 전부 제거로 진행(승인).
- bible-service는 Inc1~Inc3 동안 모놀리식 공유 DB를 읽었고, Inc4에서 전용 DB로 분리.

## 작업 내용
1. **모놀리식 FK 제거**(`V30__drop_bible_verse_cross_domain_fks.sql`): `fk_glossary_terms_verse`(glossary_terms)·`fk_qpv_verse`(qt_passage_verses)·`fk_nv_verse`(note_verses)·`fk_verse_explanations_verse`(verse_explanations) 4개 DROP. **컬럼·인덱스·UNIQUE KEY는 유지**(bible_verse_id는 참조 값). JPA는 bible_verse_id를 Long으로 보유(도메인 경계상 BibleVerse 엔티티 미참조)라 영향 없음.
2. **bible-service 시드 이전**: 모놀리식 `V7__seed_bible_books`·`V23__seed_bible_verses`를 **바이트 동일 복사** → bible-service `V2__seed_bible_books.sql`(4.4KB)·`V3__seed_bible_verses.sql`(9.3MB). 전용 DB에 V1(DDL)→V2(books)→V3(verses) Flyway 체인. 금지 번역본 0(KJV/KRV).

## 범위
- 브랜치: `feature/msa-bible-db-per-service` (base: `dev`)
- 변경: 모놀리식 마이그레이션 1(FK drop) + bible-service 시드 2(byte-copy). 코드·엔티티 무변경.
- 관련: bible 추출 Inc4 (Inc5 모놀리식 제거의 선행 — FK 제거 후 bible_verses 삭제 가능)

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL**(H2 flyway-disabled라 시드 무시, 기존 16건 유지).
- **마이그레이션 검증(사유 명시 — H2 단위 테스트 불가)**: V1 DDL·시드는 MySQL 전용 구문(AUTO_INCREMENT·UNIQUE KEY·9.3MB INSERT)이라 H2에서 실행 불가 → 단위 테스트 미작성. 시드는 모놀리식에서 **CI MySQL로 이미 검증된 V7/V23의 바이트 동일 복사**이고, Flyway 체인(V1→V2→V3)·V30 FK drop은 **CI/배포 MySQL**에서 검증된다(testcontainers MySQL 통합 테스트는 bible-service에 testcontainers 의존 추가 필요 → 별도). FK 이름·테이블명은 원본 마이그레이션과 대조 확인.

## 미해결 / 후속
- **Inc5**: 모놀리식 bible 도메인·bible_books/bible_verses 테이블 제거(FK 제거 완료로 안전) + ArchUnit 경계 갱신.
- 배포: bible-service 전용 DB로 분리(`QTAI_BIBLE_PERSISTENCE_*` → 별도 DB), Flyway 시드 1회 실행.
- (선택) testcontainers MySQL 시드 검증 테스트 — bible-service testcontainers 도입 시.

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
