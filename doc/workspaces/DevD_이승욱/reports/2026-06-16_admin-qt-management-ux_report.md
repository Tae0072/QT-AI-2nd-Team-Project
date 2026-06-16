# 2026-06-16 관리자 QT 관리 — 게시/수집 시각·권 한글·날짜 입력 개선 — 결과 보고

## 요약
관리자 QT 관리의 세 가지 사용성/일관성 이슈를 개선했다. ① 자동수집 본문이 ACTIVE인데 `publishedAt`이 비어
"게시 시각"이 안 뜨던 문제 — service-bible 수집은 게시 시각=QT 날짜 04:00 KST로 채우고, 실제 수집 시각은
별도 컬럼 `collected_at`(V51)에 기록(now). ② 권 선택을 ID 숫자 → 한글 권 이름 Select로 교체(목록도 한글). ③ QT
날짜 입력에 `-` 자동 삽입.

## 산출물
| 파일 | 설명 |
|------|------|
| `admin-server/.../db/migration/V51__add_qt_passage_collected_at.sql` | `qt_passages.collected_at` 컬럼 추가(admin-server 단독) |
| `service-bible·admin-server …/qt/internal/QtPassage.java` | `collectedAt` 필드 + `recordCollected(수집시각, 게시시각|null)` |
| `service-bible·admin-server …/qt/internal/QtPassageWriter.java` | Clock 주입, upsert에서 수집/게시 시각 기록(service-bible 04:00, admin null) |
| `admin-server …/qt/api/admin/dto/AdminQtPassageResponse.java`, `internal/AdminQtPassageService.java` | 응답·감사 스냅샷에 `collectedAt` |
| `admin-web/src/constants/bibleBooks.ts` | book_id↔한글 66권 상수 + `bibleBookName` |
| `admin-web/src/api/qtPassages.ts`, `pages/QtPassagesPage.tsx` | 권 한글 Select·목록 한글·날짜 마스킹·수집 시각 컬럼 |
| `doc/프로젝트 문서/04_API_명세서.md` | 변경 이력 v1.14(collectedAt·게시 시각 04:00·권 한글·날짜 입력) |

## 검증
- `./gradlew :service-bible:test :admin-server:test` 전부 BUILD SUCCESSFUL.
- `QtPassageWriterTest` 양 모듈: 고정 클럭으로 수집 시각=now, service-bible 게시 시각=QT날짜 04:00, admin 게시 시각=null 검증. `AdminQtPassageControllerTest` 응답 생성자 동기화.
- admin-web `npm run typecheck` 무이슈.

## 리뷰 보강(머지 전)
- 게시 시각은 비어 있을 때만 설정(`recordCollected`)해 관리자가 게시/숨김으로 바꾼 상태·시각을 재수집이 덮어쓰지 않는다.
- `isVisibleToUsers`는 status·날짜만 보므로 본 변경은 사용자 노출 동작에 영향 없음(관리자 표시 일관성만 개선).
- 봇 diff가 `.sql`을 제외하므로 V51 전문을 workflow 문서에 노출.

## 미해결 / 후속
- 권 목록을 상수로 하드코딩(66권 불변) — 추후 bible 권 메타 API가 admin-web에 도입되면 그 소스로 일원화 가능.
- 관리자 수동 등록의 게시 시각은 기존대로 '게시' 버튼 시 기록(검토 게이트 유지).

담당: DevD 이승욱
