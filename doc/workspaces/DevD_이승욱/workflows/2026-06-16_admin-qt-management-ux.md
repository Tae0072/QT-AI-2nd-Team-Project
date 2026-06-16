# 2026-06-16 관리자 QT 관리 — 게시/수집 시각·권 한글·날짜 입력 개선 (feature/admin-qt-management-ux)

## 목표·배경
관리자 웹 QT 관리에서 ① 자동수집(00:02) 본문의 "게시 시각"이 비어 보임, ② 권 선택이 ID 숫자라 보기 힘듦,
③ QT 날짜 입력 시 `-`를 일일이 쳐야 함 — 세 가지 사용성/일관성 이슈를 개선한다.

원인(①): service-bible 자동수집은 본문을 `status=ACTIVE`로 만들지만 `publishedAt`을 설정하지 않아,
사용자 노출(`isVisibleToUsers`=status·날짜 기준)은 되지만 관리자 목록의 "게시 시각" 컬럼이 비어 보였다.

## 작업 내용
### ① 게시 시각 04:00 + 수집 시각(collected_at) 분리
- `qt_passages.collected_at` 컬럼 신규(V51, admin-server 단독). 시스템 배치가 실제로 가져온 시각.
- `QtPassage.recordCollected(collectedAt, publishedAtIfAbsent)` 추가(양 모듈) — 수집 시각은 매번 갱신,
  게시 시각은 비어 있을 때만 설정(관리자 게시/숨김 상태·시각 보존).
- `QtPassageWriter`에 `Clock` 주입. service-bible 수집은 게시 시각=QT 날짜 **04:00 KST**(노출/cache refresh 기준, §6),
  수집 시각=now. admin-server 수집은 검토 대기(PENDING)라 수집 시각만 기록하고 게시 시각은 비운다.
- `AdminQtPassageResponse`·`AdminQtPassageService`(toResponse·snapshot)에 `collectedAt` 추가.

### ② 권 한글 표시 (admin-web)
- `constants/bibleBooks.ts` 신규(book_id 1~66 ↔ 한글, V7 seed 동일). 등록 폼의 권 입력을 ID 숫자
  `InputNumber` → 한글 권 이름 `Select`(검색 가능)로 교체. 목록 '본문'도 `bibleBookName(bookId)`로 한글 표시.

### ③ 날짜 입력 자동 하이픈 (admin-web)
- QT 날짜 입력에 `getValueFromEvent`로 마스킹(`maskQtDate`) — 숫자만 쳐도 `YYYY-MM-DD`로 `-`를 5·8번째에 자동 삽입.
- 목록에 '수집 시각'(collectedAt) 컬럼 추가.

## 스키마 (admin-server 단독, V51) — 봇 diff는 `.sql` 제외라 전문 노출
```sql
-- V51__add_qt_passage_collected_at.sql
ALTER TABLE qt_passages ADD COLUMN collected_at TIMESTAMP NULL;
```
- Flyway 순서: 기존 최신 V50 다음 V51. 기존 행은 수집 시각 미상 → NULL 허용(백필 없음).

## 범위/주의
- service-bible 수집 본문은 ACTIVE라 게시 시각을 채우지만, admin 수집/수동 등록(PENDING)은 게시 시각을 비워 둔다(검토 게이트 보존).
- `isVisibleToUsers`는 여전히 `status==ACTIVE && 날짜도래`만 본다 — publishedAt은 노출 판정에 쓰지 않으므로 이 변경은 노출 동작에 영향 없음(관리자 표시 일관성만 개선).
- admin-server 복사본 동기화(엔티티 recordCollected), Flyway는 admin-server 단독.

## 검증
- `./gradlew :service-bible:test :admin-server:test` 전부 BUILD SUCCESSFUL.
- `QtPassageWriterTest`(양 모듈): 고정 클럭으로 수집 시각=now, service-bible 게시 시각=QT날짜 04:00, admin 게시 시각=null 검증.
- admin-web `npm run typecheck` 무이슈.
- push 전 .github 게이트 점검: 브랜치명 `feature/…`, 금지 패턴·시크릿 없음, V51 전문 문서 노출(CLAUDE.md §13).

담당: DevD 이승욱
