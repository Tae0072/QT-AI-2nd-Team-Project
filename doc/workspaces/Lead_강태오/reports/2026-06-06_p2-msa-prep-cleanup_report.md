# 2026-06-06 P2 — MSA 준비 정리(죽은 코드 + qt→bible SQL 경계) 결과 보고

리뷰 §4(죽은 코드)·§5.2 #1(SQL 경계 침범)을 대상으로 한 MSA 준비 정리 1차. **3개 브랜치/3개 커밋**, 서버 전체 스위트 **831 tests / 0 failures**(현 브랜치 기준).
워크플로우: `workflows/2026-06-06_p2-msa-prep-cleanup.md`

> 작업 방식: 항목별 독립 브랜치로 구현·커밋까지. **push·PR 생성은 사용자(T)가 직접.** 삭제 전 모든 후보의 프로덕션 주입처 0건을 서브에이전트 + grep으로 재검증(2026-06-05 리뷰 이후 P0/P1 변경 반영).

## 브랜치별 결과

| 브랜치 | 커밋 | 내용 |
|--------|------|------|
| `chore/remove-unwired-client-stubs` | 3cca2ce | 미와이어드 스캐폴딩 10파일 삭제 — external/kakao 3종(미구현 stub, 실사용은 member/client/kakao), study/report/notification/praise의 `client/.../UseCaseMock` 4종(CLAUDE.md §4대로 실 UseCase 등록 후 삭제 대상), praise/internal `Praise`·`PraiseRepository` 빈 TODO 스텁(외부 링크 설계 주석 = §8 표면도 제거) |
| `chore/remove-dead-dtos` | 72145f2 | 죽은 DTO·스텁 5파일 삭제 — qt/api/dto `QtSimulatorResponse`·`QtStudyContentResponse`(study 동명 구현이 실엔드포인트 담당, qt 스켈레톤은 사장), member/api/dto `MemberSettingsResponse`·`MemberSettingsUpdateRequest`(실사용은 `SettingsResponse`), ai/internal `AiCallLog`(미구현 스텁 엔티티) |
| `refactor/qt-bible-sql-boundary` | e48d4c2 | **§5.2 #1 최우선 차단요소 제거.** `QtPassageRepository`의 native SQL 2건이 bible 소유 `bible_books`를 직접 조회/JOIN하던 경계 침범을 제거하고, bible 공개 포트 `ListBibleBooksUseCase` 경유 `BibleBookLookup`(신규)로 대체. 장/절은 `QtPassage`(qt 소유), 권 메타는 bible api에서 합쳐 범위 응답 생성 |

## §5.2 #1 상세 (MSA 분리 최우선)

- 제거 전: `SELECT b.id FROM bible_books ...`, `... qt_passages p JOIN bible_books b ON b.id = p.book_id`. ArchUnit이 못 잡는 SQL-레벨 경계 침범 — DB 분리 순간 깨지는 지점.
- 제거 후: qt는 bible의 `ListBibleBooksUseCase`(api 포트)만 사용. note/ai가 bible api를 직접 주입하는 기존 패턴과 동일해 ArchUnit 도메인 경계 테스트도 통과.
- 호출 지점(일일 수집·Today QT 캐시 갱신)이 사용자 요청당이 아니라 배치/캐시 경계라, 66권 전권 조회 비용은 무시할 수준(권 캐싱은 별도 개선 항목 — 아래 후속).

## 검증

- 전체 스위트 **831 tests / 0 failures / 4 skipped**(Docker 필요한 `MysqlMigrationValidationTest` 등은 로컬 자동 skip).
- 각 브랜치 커밋 시점마다 전체 빌드+테스트 그린 확인. ArchUnit `DomainBoundaryArchTest`(qt.internal→bible.api 허용, 타 도메인 internal 금지) 통과.
- SQL 경계 리팩터링은 DB 슬라이스 테스트 2종을 협력자 mock 단위 테스트로 재구성 + `BibleBookLookupTest` 신설로 회귀 방지.
- 비밀/금지 기술 추가 없음(삭제·리팩터링만). praise 스텁 제거로 §8(유튜브 링크) 설계 주석 표면도 축소.

## 의도적으로 보류한 항목(이번 배치 제외)

| 항목 | 이유 |
|------|------|
| `ai/client/qt`(QtContextClient·GetQtUseCaseMock·QtContextResult + 계약 테스트) | 프로덕션 주입처는 0이지만, ai→qt 프롬프트 컨텍스트용 **미래 포트**이고 `QtContextResult`에 §8(성경 본문 미노출) 가드 테스트가 붙어 있음. 삭제는 "ai가 qt 컨텍스트를 쓸지" 제품 결정이라 보류 — 결정 시 일괄 정리 |
| `JournalEventRepository.findByEventId`·`findByStatusOrderByOccurredAtAsc` | 현재 미사용이나 P1-10(`fix/journal-events-outbox`)과 같은 파일이라 머지 충돌 방지 위해 P1-10 PR에 합치거나 그 머지 후 정리 권장 |
| 중복 헬퍼(`requireAuthorizedReviewer` 4중, admin 인증 헬퍼 3중) | 삭제가 아니라 공통 추출 리팩터링 — 동작 영향 있어 별도 작업으로 분리 |

## 후속(다음 배치 후보)

- 안전 P2 버그 묶음: qt `@Cacheable` Clock SpEL·00~04시 캐시 0%, member 상태코드 오용·잠금 off-by-one, study `aiAssetId` 내부 PK 노출, ai NEEDS_REVIEW 재검증 영구 차단, 공통 죽은 설정(JacksonConfig/RedisConfig 등).
- MSA 구조(큰 건): member purge saga, ai 승인↔게시 보상, journal outbox 폴링 릴레이 확장, audit 수집 방식, Spring Modulith 도입.
- 문서 drift: ERD↔실제 qt_passages 구조, OpenAPI 누락 경로, 마이그레이션 헤더 버전 오기.

## 머지 메모

- 세 브랜치는 서로 다른 파일이라 충돌 없음. 머지 순서 자유.
- `bibleBookLookup`은 bible `ListBibleBooksUseCase`(이미 존재) 주입 — bible 도메인 변경 없음.
- push·PR은 T가 직접. 전 브랜치 머지 후 `dev` 전체 테스트 1회 권장.
