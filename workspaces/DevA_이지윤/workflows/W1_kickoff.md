# DevA 이지윤 — W1 첫 작업 가이드

## 담당 (DECISIONS.md §0 — 2026-05-14 재배치)
Bible 도메인 → Flutter → 인증 → 관리자 페이지 / 편집자 에이전트 보조 큐레이션·골든셋.
이승욱과 페어, 김지민과 Bible팀 3인 1조.

## 첫 PR 권장 순서

### 1. Bible 본문 조회 부팅
- 위치: `services/bible-service/`
- 채워 넣을 곳:
  - `bible/api/BibleController.java` — 이미 KR/EN/books 응답 시그니처 있음. 캐시 키 확인.
  - `bible/infrastructure/*Repository.java` — Spring Data JPA 자동 — 동작 OK.
  - Flyway `V1__init_schema.sql` / `V2__seed_books.sql` — 데모 시드는 GEN 1:1 (단일 절). 본인은 PSA, PRO 일부 더 채워 넣어 본문 캐시 검증. (오늘 QT는 범위 허용 — ADR-0021)
- 검증:
  ```bash
  curl http://localhost:8082/bible/kr/GEN/1/1
  curl http://localhost:8082/bible/en/GEN/1/1
  curl http://localhost:8082/bible/books
  ```

### 2. 본문 설명 + 해설 채우기
- `bible/domain/BibleExplanation.java` — 범위(`chapter_start ~ verse_end`) + `source_type` 2분류 이해.
  - REFERENCE_SOURCE: Tyndale / Matthew Henry / Bible Dictionary — AI 컨텍스트 적재용 (응답 노출 X)
  - GENERATED_EXPLANATION: AI 생성 + `editor_verified_at NOT NULL`만 응답 노출
- `bible/infrastructure/BibleExplanationRepository.java` — 범위 매칭 JPQL 사용 (`(chapter*1000+verse)` 인덱스 비교).
- TODO: 별도 `bible_passage_explanations` 테이블 분리 검토 — 쉬운 본문 설명(summary/background/terms)이 commentary와 형식이 달라서.

### 3. 편집자 에이전트 골든셋 준비
- 강상민의 AI 해설 생성 결과를 검증할 골든셋(정답 데이터) 큐레이션 시작.
- 위치 제안: `workspaces/DevA_이지윤/reports/golden_set_GEN_1_1.md`.

## 금지
- 개역개정 / ESV / NIV 적재 — 커밋 절대 금지 (DECISIONS.md §8)
- `JOURNAL_EVENTS` 수정/삭제 코드 — append-only

## 산출물
- `workspaces/DevA_이지윤/reports/W1_bible_bootup.md`에 본문 조회 + 캐시 hit/miss 검증 결과.
