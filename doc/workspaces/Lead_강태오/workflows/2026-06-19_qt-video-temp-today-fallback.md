# 워크플로우 — QT 영상 한시적 고정(오늘 미생성 시 최근 등록 영상 폴백)

작성 2026-06-19 · 브랜치 feature/qt-video-temp-today-fallback · 대상 service-bible(qtvideo) · F-12(QT 영상)/F-06(관리자 운영)

## 배경
- 오늘(2026-06-19) QT 영상이 생성되지 않아 사용자 앱 QT 영상 섹션이 MISSING으로 비표시(화면정의서 §6.12).
- 요청: "이번만" 한시적으로 6/14·6/15에 등록된 영상이 그대로 노출되게 한다. 영구 로직 변경 아님.

## 현행 동작
- 앱: `GET /qt/today` → 오늘 본문 qtPassageId → `GET /qt/{id}/video`.
- 서버 `QtVideoService`: 그 본문의 노출 클립(APPROVED/HIDDEN/FAILED)을 찾아 READY/상태/MISSING 반환. 영상은 "본문(날짜) 단위"로 묶인다.

## 결정 — [A] 백엔드 최근영상 폴백 (사용자 선택)
- "오늘(KST) 본문에 노출 클립이 전혀 없을 때만" 가장 최근 등록(APPROVED) 영상으로 한시 대체.
- 이유: 날짜 id 하드코딩 불필요(환경 안전)·한 곳만 수정·앱 재배포 불필요·원복 명확.
- 미채택: [B] 임시데이터(환경별 본문 id 의존·DB 임시행), [C] 프론트 폴백(폴백 id 하드코딩·앱 재배포).

## 구현
- `QtVideoService`: `.or(() -> recentApprovedFallbackForToday(context))` 임시 분기. 오늘(KST) 판정은 모듈 관례대로 주입 `Clock`(`LocalDate.now(clock.withZone(KST))`).
- `QtVideoClipRepository`: `findTopByStatusAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(APPROVED)` 추가(최근 1건).
- 모든 임시 코드에 `[임시 2026-06-19]` 마커. 범위: 오늘 본문일 때만(과거 본문·클립 있는 본문 영향 없음).

## 원복 방법(나중에)
1. `QtVideoService`의 `.or(...)` 한 줄 + `recentApprovedFallbackForToday(...)` 메서드 + `KST`/`clock` 필드(생성자 인자) 제거.
2. `QtVideoClipRepository`의 `findTopByStatus...` 제거.
3. `QtVideoServiceTest`의 `FIXED_CLOCK`/신규 2개 테스트 제거, setUp 생성자 2인자로 환원.
→ 또는 본 PR을 `git revert`.

## 검증
- `./gradlew :service-bible:test --tests "*QtVideoServiceTest" --tests "*QtVideoControllerTest"` → BUILD SUCCESSFUL.
- 신규 단위 2: 오늘 미생성→폴백 READY / 오늘 아님→MISSING 유지(폴백 미호출). 기존 8건 유지.