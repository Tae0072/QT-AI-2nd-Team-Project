# TodayQt ↔ Note 도메인 연동 리포트

- 작업자: Lead 강태오
- 날짜: 2026-05-28
- PR: #130 (`feature/qt-note-draft-link` → `dev`)
- 관련 기능: F-01 오늘의 QT 조회

## 작업 요약

PR #126(GetTodayQtUseCase)에서 draftNoteId=null로 두었던 필드를
note 도메인의 GetNoteUseCase.getDraft()와 연동하여 실제 값으로 채웠다.

## 해결한 문제

### 캐시 vs 사용자별 데이터 충돌
- 문제: @Cacheable 캐시 키가 날짜 기반(memberId 미포함)
- 만약 draftNoteId를 캐시에 넣으면 한 사용자의 노트 ID가 모든 사용자에게 보임
- 해결: QtPassageLookup 컴포넌트를 분리하여 캐시에는 공용 데이터만 저장
  QtService에서 캐시 바깥에서 사용자별 draftNoteId를 enrich

### 도메인 간 의존 방향
- qt → note.api.GetNoteUseCase 인터페이스만 import (CLAUDE.md §3, §4 준수)
- note.internal.* 접근 없음
- NoteService(이승욱 구현)가 이미 GetNoteUseCase.getDraft()를 구현하고 있어 Mock 불필요

## 변경 내역

| 파일 | 변경 | 줄 수 |
|------|------|-------|
| QtPassageLookup.java | 신규 | +95 |
| QtService.java | 수정 — 캐시 제거, passageLookup 위임 + note 연동 | 변경 |
| QtPassageLookupTest.java | 신규 — 본문 조회 로직 테스트 5건 | +161 |
| QtServiceTest.java | 전면 개편 — note 연동 테스트 10건 | 변경 |
| QtServiceCacheTest.java | 수정 — QtPassageLookup 캐시 테스트 | 변경 |
| **합계** | 5 files | +508 / -220 |

## 테스트 현황

| 테스트 클래스 | 건수 | 검증 항목 |
|--------------|------|----------|
| QtPassageLookupTest | 5 | HIT/STALE_FALLBACK/MISS/EMPTY/새벽 HIT |
| QtServiceTest | 10 | DRAFT 있음/없음, null memberId, null qtPassageId, 예외 fallback, STALE_FALLBACK enrich, getPassage+DRAFT |
| QtServiceCacheTest | 2 | HIT 캐싱, MISS 미캐싱 |
| QtControllerTest | 기존 | 변경 없음 (인터페이스 MockBean) |

## 빌드 결과
```
./gradlew build → BUILD SUCCESSFUL in 22s
```

## 배운 점
- Spring @Cacheable은 프록시 기반이므로 같은 클래스 내부 호출에서는 동작하지 않음
  → 캐시 대상 메서드를 별도 빈(QtPassageLookup)으로 분리해야 함
- 캐시 키에 사용자 식별자가 없으면 사용자별 데이터를 캐시에 넣으면 안 됨
  → "캐시 바깥에서 enrich" 패턴으로 해결
- 도메인 간 호출에서 방어적 try-catch가 중요함
  → note 도메인이 다운되어도 QT 본문 응답은 정상 반환되어야 함
