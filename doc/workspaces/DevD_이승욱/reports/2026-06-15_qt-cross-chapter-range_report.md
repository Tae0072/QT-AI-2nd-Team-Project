# 2026-06-15 성서유니온 QT 본문 장 교차(권/장) 수집·관리 지원 — 결과 보고

## 요약
성서유니온 자동 수집이 장 교차 본문(예: `고린도전서 9:1-10:5`)을 "같은 장만 저장" 가드로 거부해
해당 날짜 Today QT가 비던 문제를 수정했다. 파서·DTO·엔티티·절 매핑·범위 응답을 장 교차 지원으로 확장하고,
`qt_passages`에 `end_chapter`/`end_book_id`를 추가(V45)했다. 관리자 QT 관리(admin REST + admin-web)도
`endChapter`를 지원해, 자동수집 교차 본문을 관리자가 수정해도 종료 장이 보존된다. 권 교차는 수집 소스에
없어 스키마만 대비하고 파서는 같은 권 기준으로 활성화했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `service-bible·admin-server …/qt/client/sum/SuTodayPassageParser.java`, `SuTodayPassage.java` | 장 교차 파싱 + 시작/종료 장 보존 + referenceText 교차 표기 |
| `…/qt/internal/QtPassage.java` (양 모듈) | `end_chapter`/`end_book_id` 컬럼 + 단일·교차 범위 생성/갱신 |
| `…/qt/internal/QtTodayPassageImportService.java` (양 모듈) | `collectRangeVerses` 장별 조회·경계 필터로 교차 절 매핑 |
| `…/qt/internal/TodayQtRangeMapper.java`, `api/dto/TodayQtRangeResponse.java` | 종료 장 포함 범위 응답·displayText |
| `admin-server …/db/migration/V45__qt_passages_multi_chapter_range.sql` | `end_book_id`/`end_chapter` 추가 + 백필 + FK (admin-server 단독) |
| `admin-server …/qt/api/admin/dto/AdminQtPassageResponse.java`, `AdminQtPassageCommand.java`, `web/AdminQtPassageController.java`, `internal/AdminQtPassageService.java` | 관리자 API에 endChapter(미지정 시 시작 장 보정·같은 장만 절 순서 강제) |
| `admin-web/src/api/qtPassages.ts`, `pages/QtPassagesPage.tsx` | 관리자 웹 타입·폼('종료 장')·표·검증 |
| `doc/workspaces/DevD_이승욱/workflows/2026-06-15_qt-cross-chapter-range.md` | 작업 워크플로우 |

## 검증
- `./gradlew :service-bible:test :admin-server:test` 전부 BUILD SUCCESSFUL.
- 파서 단위 테스트 갱신: 같은 장 파싱, 장 교차(`요한복음 3:36-4:2`) 정상 파싱, 종료 장 역전 거부.
- 교차 컬럼 추가로 깨지던 raw insert/픽스처(`QtVideoControllerTest`, `QtVideoClipPreparationEventIntegrationTest`, `dummy_today_qt_1co_verses.sql`) 및 admin 테스트(Command/Response/Request 생성자) 보강.
- admin-web `npm run typecheck` 무이슈.
- (라이브) 12:45 1회용 cron 재발사로 원인 재현 → 수정 후 단위/통합 테스트로 검증. 실DB 라이브 수집은 머지·배포 후.

## 리뷰 보강(머지 전)
- bible API 단일 장 제약을 우회하려 타 도메인 API를 바꾸지 않고 qt 도메인 안에서 장별 조회로 해결(도메인 경계 유지).
- `end_book_id`는 권 교차 대비 컬럼 — 현재 항상 `book_id`와 동일. 권 교차는 소스에 없어 파서에서 비활성.
- admin 요청 `endChapter` 선택 필드 + 컨트롤러 보정으로 기존 단일 장 클라이언트 하위호환 유지.

## 미해결 / 후속
- **DevA 협의**: admin-qt API `endChapter` 추가 → `04_API_명세서`·계약서 동기화 필요.
- **배포**: 교차 본문 라이브 수집은 머지 후 admin-server(V45 적용)+service-bible 동시 배포 필요(현재 로컬 스택은 기존 코드로 복구).
- PR 크기: 변경 파일이 가이드(10 files) 초과 → 필요 시 ①수집+스키마 ②관리자 API+웹 2개 PR로 분할 검토.

담당: DevD 이승욱
