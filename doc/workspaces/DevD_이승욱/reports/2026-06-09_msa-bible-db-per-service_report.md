# 2026-06-09 bible DB-per-service(Inc4) — 결과 보고

## 요약
bible 데이터 소유를 bible-service 전용 DB로 이전. bible-service에 시드(books/verses) 마이그레이션 이전 + 모놀리식의 bible_verses 참조 **크로스 도메인 FK 4개 제거**. bible_verse_id는 참조 값으로 유지. 마이그레이션 SQL만 변경(코드·엔티티 무변경).

## 산출물

| 파일 | 설명 |
|------|------|
| `src/main/resources/db/migration/V30__drop_bible_verse_cross_domain_fks.sql` | (신규) 4개 FK DROP — glossary_terms·qt_passage_verses·note_verses·verse_explanations |
| `bible-service/.../db/migration/V2__seed_bible_books.sql` | (신규, byte-copy) 66권 books 시드 |
| `bible-service/.../db/migration/V3__seed_bible_verses.sql` | (신규, byte-copy 9.3MB) verses 시드 |

## 변경 성격
- **데이터 소유 이전**: bible-service 전용 DB가 V1(DDL)→V2(books)→V3(verses)로 자기 데이터를 보유.
- **크로스 FK 제거**: 4개 도메인(study/qt/note/ai)이 bible_verses에 대한 DB 레벨 FK를 잃고 bible_verse_id를 참조 값으로 보유 → DB 분리 가능. 컬럼·인덱스·UNIQUE KEY 유지(앱/서비스 경계 무결성). JPA는 Long 보유라 영향 없음.
- 금지 번역본 0(KJV/KRV). 코드·엔티티 무변경.

## 검증
- `gradlew :bible-service:test` — BUILD SUCCESSFUL(H2 flyway-disabled, 기존 16건 유지).
- **사유 명시(#3)**: 시드/DDL은 MySQL 전용 구문이라 H2 단위 테스트 불가. 시드는 모놀리식 CI MySQL 검증된 V7/V23 바이트 동일 복사. Flyway 체인·FK drop은 CI/배포 MySQL 검증. FK명·테이블명 원본 대조 확인.

## 미해결
- Inc5(모놀리식 bible 도메인·테이블 제거) → bible 추출 완료. API 명세(04) 동기화.
- 배포: bible-service 전용 DB 분리 + Flyway 시드 1회.
