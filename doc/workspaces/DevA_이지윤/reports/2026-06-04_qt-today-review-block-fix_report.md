# 2026-06-04 QT Today PR BLOCK 수정 리포트

## 요약

`feature/flutter-bible-api-integration` PR의 자동 리뷰 BLOCK 항목을 팀 명세 기준으로 수정했다.

핵심은 운영 Flyway migration에 들어간 임시 더미 seed를 제거하고, 00:05 KST 수집과 04:00 KST 사용자 노출 정책을 분리한 것이다. 또한 `resolveRange()` 중복과 `Optional` null 방어를 정리하고, 외부 호출/파싱 실패를 `BusinessException` 기반으로 맞췄다.

## 작업 브랜치

| 항목 | 내용 |
| --- | --- |
| 기능 브랜치 | `feature/flutter-bible-api-integration` |
| 리포트 브랜치 | `feature/report` |
| 기준일 | 2026-06-04 |
| 최신 기능 커밋 | `76cc5ba fix(qt): align today passage import with guards` |

## BLOCK 수정 내용

| BLOCK | 수정 결과 |
| --- | --- |
| 운영 Flyway migration에 임시 더미 seed 커밋 | 더미 seed를 `src/test/resources/db/fixtures/`로 이동해 운영 migration 대상에서 제외 |
| 00:05 import 후 캐시 삭제로 04:00 전 오늘 QT 노출 가능 | `@CacheEvict` 제거, 00:00~04:00 조회 시 전일 본문 `STALE_FALLBACK` 반환 |
| `resolveRange()` 중복과 죽은 null 방어 | `TodayQtRangeResolver` 추가, `Optional.map(...).orElse(null)`로 단순화 |
| 외부 호출/파싱/권명 매칭 실패 표준 예외 미사용 | `BusinessException` + `ErrorCode`로 매핑 |
| 더미 seed 운영 배포 가드 불명확 | 운영 migration에서 제거하고 fixture 목적을 문서와 테스트에 명시 |

## 변경 파일 적용내용

| 파일 | 적용 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/qt/internal/QtPassageLookup.java` | 04:00 전 전일 본문 반환 정책 고정 |
| `qtai-server/src/main/java/com/qtai/domain/qt/internal/TodayQtRangeResolver.java` | QT range 조회 공통화 |
| `qtai-server/src/main/java/com/qtai/domain/qt/internal/QtService.java` | range 조회를 resolver로 위임 |
| `qtai-server/src/main/java/com/qtai/domain/qt/internal/QtTodayPassageImportService.java` | 즉시 cache evict 제거, 생성/갱신 흐름 분리, 권명 실패 예외 매핑 |
| `qtai-server/src/main/java/com/qtai/domain/qt/client/sum/SuTodayBibleClient.java` | 외부 API 실패를 `BusinessException`으로 매핑 |
| `qtai-server/src/main/java/com/qtai/domain/qt/client/sum/SuTodayPassageParser.java` | 파싱 실패를 `BusinessException`으로 매핑 |
| `qtai-server/src/main/resources/db/migration/V23__seed_dummy_today_qt_1co_verses.sql` | 운영 Flyway migration에서 제거 |
| `qtai-server/src/test/resources/db/fixtures/dummy_today_qt_1co_verses.sql` | 테스트/로컬 확인 전용 fixture로 이동 |
| `qtai-server/src/test/java/com/qtai/**` | BLOCK 회귀 방지 테스트 수정 |
| `doc/workspaces/DevA_이지윤/**` | workflow/report 정책 설명 갱신 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| Requirements Guard 로컬 재현 | 통과 |
| `./gradlew.bat test --tests ...` | QT 관련 테스트 통과 |
| `./gradlew.bat build` | 통과 |
| `flutter analyze` | 통과 |
| `flutter test test/app_route_resolver_test.dart test/features/bible` | 통과 |

## 남은 확인

- GitHub Actions에서 새 커밋 기준 `ci-all`과 `Requirements Guard` 결과를 확인해야 한다.
- 실제 본문 seed/import가 들어온 뒤 테스트/로컬 fixture가 더 이상 필요 없는지 확인한다.
- 관리자 수동 trigger API를 추가할 경우 권한 검사와 에러 응답 계약을 별도 PR에서 확정한다.
