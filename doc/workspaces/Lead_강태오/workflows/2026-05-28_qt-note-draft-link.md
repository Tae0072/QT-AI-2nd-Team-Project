# 2026-05-28 TodayQt ↔ Note 도메인 연동 — draftNoteId enrich

## 목표
TodayQtResponse의 draftNoteId 필드를 note 도메인과 연동하여 실제 데이터로 채운다.
사용자가 해당 QT 본문에 대해 작성 중인 묵상 노트(MEDITATION DRAFT)가 있으면 그 ID를 반환한다.

## 배경
- PR #126에서 GetTodayQtUseCase를 구현했으나, draftNoteId는 항상 null로 반환
- `@Cacheable` 캐시 키에 memberId가 없어서 사용자별 데이터를 캐시에 넣으면 안 됨
- note 도메인(이승욱 담당)의 NoteService가 이미 getDraft() 메서드를 완전 구현
- ERD에 별도 `qt` 테이블 없음 — 사용자 묵상 기록은 `notes` 테이블의 `category=MEDITATION`

## 작업 순서

| # | 작업 | 상태 |
|---|------|------|
| 1 | 캐시 vs 사용자 데이터 분리 방안 설계 | 완료 |
| 2 | QtPassageLookup 컴포넌트 생성 — @Cacheable 로직 분리 | 완료 |
| 3 | QtService 리팩터링 — passageLookup 위임 + note 연동 | 완료 |
| 4 | QtPassageLookupTest 작성 — 본문 조회 로직 테스트 5건 | 완료 |
| 5 | QtServiceTest 개편 — note 연동 오케스트레이션 테스트 10건 | 완료 |
| 6 | QtServiceCacheTest 수정 — QtPassageLookup 캐시 테스트 | 완료 |
| 7 | 전체 빌드 + 테스트 검증 (BUILD SUCCESSFUL) | 완료 |
| 8 | 커밋 + push + PR #130 생성 | 완료 |
| 9 | 워크플로우 + 리포트 작성 | 완료 |

## 핵심 구현 결정

### 캐시 분리 패턴
```
QtService.getToday(memberId)
  └→ QtPassageLookup.findTodayPassage()   ← @Cacheable (날짜 키, draftNoteId=null)
  └→ GetNoteUseCase.getDraft(memberId, MEDITATION, qtPassageId)  ← 캐시 바깥
  └→ enrichWithDraftNoteId(base, draftNoteId)  ← 새 TodayQtResponse 생성
```

- **QtPassageLookup**: 날짜 기반 캐시, 공용 데이터만 반환 (draftNoteId=null)
- **QtService**: 캐시 바깥에서 memberId로 note 도메인 호출 후 enrich

### 도메인 경계 준수
- qt → note.api.GetNoteUseCase (인터페이스 import만, CLAUDE.md §3/§4)
- note.internal.* 직접 import 없음
- NoteService가 GetNoteUseCase를 이미 구현 → Mock 불필요

### 방어적 예외 처리
- memberId == null → note 도메인 호출 생략 (draftNoteId=null)
- qtPassageId == null (EMPTY/MISS) → note 도메인 호출 생략
- note 도메인 예외 발생 → log.warn + draftNoteId=null fallback
- QT 본문 응답 자체는 실패하지 않음

## 변경 파일 (5개, +508/-220)

| 파일 | 변경 |
|------|------|
| QtPassageLookup.java | 신규 — @Cacheable 캐시 로직 분리 |
| QtService.java | 수정 — 캐시 제거, passageLookup 위임 + note 연동 |
| QtPassageLookupTest.java | 신규 — 본문 조회 로직 테스트 5건 |
| QtServiceTest.java | 전면 개편 — note 연동 오케스트레이션 테스트 10건 |
| QtServiceCacheTest.java | 수정 — QtPassageLookup 캐시 테스트로 변경 |

## PR
- PR #130: `feature/qt-note-draft-link` → `dev`
- 커밋: `feat(qt): TodayQt에 note 도메인 연동 — draftNoteId enrich (F-01)`
