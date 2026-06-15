# 2026-06-15 성서유니온 QT 본문 장 교차(권/장) 수집·관리 지원 (feature/qt-multi-chapter-range)

## 목표·배경
오늘(2026-06-15) 성서유니온 자동 수집이 실패했다. 1회용 cron으로 재발사해 원인을 재현한 결과,
`SuTodayPassageImportScheduler`(00:02 KST)는 정상 발사되나 `SuTodayPassageParser`의
"같은 장만 저장" 가드가 장 교차 본문을 `INVALID_INPUT`으로 거부하는 것이 원인이었다.

```
WARN SuTodayPassageImportScheduler : 성서유니온 오늘 QT 본문 반영 실패.
qtDate=2026-06-15, errorMessage=QT 본문 범위는 같은 장 안에서만 저장할 수 있습니다.
```

성서유니온 매일성경은 같은 권 안에서 장을 넘기는 범위(예: `고린도전서 9:1-10:5`)를 자주 내보내므로,
그런 날은 본문이 통째로 비고 Today QT·AI 시딩·관리자 QT 관리가 모두 영향을 받는다.
Lead 승인 후 "권+장 교차까지" 지원으로 구현(권 교차는 스키마만 대비, 파서는 같은 권 내 장 교차 활성화).

## 작업 내용
### ① 수집 파이프라인 (service-bible, admin-server 복사본 동기화)
- `SuTodayPassageParser`: "같은 장만" 거부 제거. 시작/종료 장을 함께 파싱하고
  referenceText를 같은 장 `9:1-23` / 장 교차 `9:1-10:5` 형태로 생성. 종료 장 < 시작 장만 거부.
- `SuTodayPassage` DTO: `endChapter` 추가(시작 장 `chapter` 유지).
- `QtPassage` 엔티티: `end_chapter`/`end_book_id` 추가(시작값 승계). 단일·교차 범위 생성/갱신 오버로드.
- `QtTodayPassageImportService.collectRangeVerses`: bible API가 단일 장 전용이라
  장 교차는 장별로 조회(`getVerses(..., null, null)` = 장 전체)하고 경계 절을 필터링해 이어붙여
  `qt_passage_verses`를 채운다. (AI 해설은 verseIds 소비 → 매핑만 맞으면 영향 없음)
- `TodayQtRangeMapper`/`TodayQtRangeResponse`: 종료 장 포함 displayText로 교차 범위 표기.

### ② 스키마 (admin-server 단독, V45)
- `V45__qt_passages_multi_chapter_range.sql`: `end_book_id`/`end_chapter` 추가 + 기존 행 백필 + FK.
  MySQL/H2 호환 위해 컬럼은 한 문장씩 NULL 추가 후 백필(V40 패턴).

### ③ 관리자 QT 관리 (admin REST + admin-web)
- `AdminQtPassageResponse`/`Command`/`Request`/`Service`/검증에 `endChapter` 추가.
  요청에서 `endChapter` 미지정 시 시작 장으로 보정(단일 장 하위호환). 같은 장일 때만 절 순서 강제.
- admin-web `qtPassages.ts` 타입·요청에 `endChapter`, `QtPassagesPage` 폼에 '종료 장' 입력·표 표시·검증.
  → 자동수집 교차 본문을 관리자가 수정해도 `end_chapter` 손실 없음.

## 범위/주의
- **권 교차**: 성서유니온 표기에는 권 교차가 없어 스키마(`end_book_id`)만 대비하고 파서는 같은 권 기준.
- **admin-server 동기화**: 도메인 로직은 service-bible 원본, admin-server 복사본이 따라감. Flyway는 admin-server 단독.
- **DevA 계약**: admin-qt API에 `endChapter` 추가 → `04_API_명세서`/계약서 동기화 필요(PR 명시).
- 실행 중 로컬 스택은 V45 미적용 DB와 호환 위해 기존 코드(00:02)로 service-bible 복구 배포함.
  교차 본문 라이브 수집은 머지 후 admin-server(V45)+service-bible 동시 배포 필요.

## 검증
- `./gradlew :service-bible:test :admin-server:test` 전부 BUILD SUCCESSFUL.
- 파서 단위 테스트: 같은 장 / 장 교차 정상 파싱, 종료 장 역전 거부.
- admin-web `npm run typecheck` 무이슈.
- push 전 .github 게이트 자체 점검: 브랜치명 `feature/…`, Requirements Guard 금지 패턴 없음, 시크릿 없음(CLAUDE.md §13).

담당: DevD 이승욱
