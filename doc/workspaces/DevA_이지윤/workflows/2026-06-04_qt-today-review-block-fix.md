# 2026-06-04 QT Today PR BLOCK 수정 workflow

## 1. 목적

`feature/flutter-bible-api-integration` PR의 Claude 자동 리뷰 BLOCK 항목과 CI 실패 가능 지점을 팀 명세 기준으로 정리하고 수정한다.

수정 대상은 오늘 QT 범위 수집, Flutter QT API 연동, 성서유니온 범위 파싱, 임시 더미 데이터 처리, 00:00/04:00 KST 노출 정책이다.

## 2. 기준 문서

| 기준 | 적용 내용 |
| --- | --- |
| `07_요구사항_정의서.md` v3.1 | 오늘 QT는 팀 정책에 맞게 사용자 노출 기준을 지킨다. |
| `04_API_명세서.md` | 실패 응답은 `BusinessException`과 `ErrorCode` 기반으로 매핑한다. |
| `09_Git_규칙.md` | `dev` 최신 반영 후 feature 브랜치에 push한다. |
| `18_코드_품질_게이트.md` | Requirements Guard 차단 패턴을 통과해야 한다. |
| `23_도메인_용어사전.md` | 성서유니온 본문 텍스트는 저장하지 않고 범위 정보만 사용한다. |

## 3. BLOCK 항목

| 항목 | 원인 | 수정 방향 |
| --- | --- | --- |
| Flyway 더미 seed | 운영 migration에 임시 더미 seed가 들어가면 나중에 삭제 시 checksum 문제가 생긴다. | 운영 migration에서 제거하고 테스트/로컬 fixture로 이동한다. |
| 04:00 노출 정책 | 00:05 import 후 캐시를 지우면 04:00 전 오늘 QT가 사용자에게 노출될 수 있다. | import 시 `todayQt` 캐시를 즉시 삭제하지 않고, 조회 로직에서 04:00 전에는 전일 본문을 반환한다. |
| `resolveRange()` 중복 | 조회와 null 처리 패턴이 `QtPassageLookup`, `QtService`에 중복되어 있었다. | `TodayQtRangeResolver`로 범위 조회를 통합하고 mapper는 응답 변환만 담당한다. |
| `Optional` null 방어 | Spring Data JPA `Optional` 반환값에 대해 null 비교를 했다. | `Optional.map(...).orElse(null)`로 단순화한다. |
| 표준 예외 매핑 | 외부 호출/파싱/권명 매칭 실패가 표준 런타임 예외였다. | `BusinessException`과 `ErrorCode`로 매핑한다. |

## 4. 구현 절차

1. `origin/dev` 최신을 가져와 `feature/flutter-bible-api-integration`에 rebase한다.
2. `application.yml` 충돌은 `dev`의 `qtai.validation` 설정과 QT 수집 설정을 모두 보존한다.
3. 더미 seed를 운영 Flyway migration에서 제거하고 fixture로 이동한다.
4. `QtTodayPassageImportService`에서 `@CacheEvict`를 제거해 00:05 import와 04:00 사용자 노출 정책을 분리한다.
5. `QtPassageLookup`은 00:00~04:00 사이 오늘 데이터가 DB에 있어도 전일 본문을 `STALE_FALLBACK`으로 반환하도록 고정한다.
6. `TodayQtRangeResolver`를 추가해 range 조회 중복을 제거한다.
7. 외부 호출, 파싱 실패, DB 권명 매칭 실패를 `BusinessException`으로 변환한다.
8. 관련 테스트와 문서를 수정한다.
9. Requirements Guard, 서버 build, Flutter analyze/test를 실행한다.
10. rebase로 히스토리가 바뀐 feature 브랜치는 `--force-with-lease`로 안전하게 push한다.

## 5. 완료 기준

| 기준 | 완료 조건 |
| --- | --- |
| dev 최신화 | `origin/dev` 기준 rebase 완료 |
| BLOCK 해소 | Flyway 더미 seed, 04:00 정책, range 중복, 예외 매핑 수정 |
| Guard | Requirements Guard 로컬 재현 통과 |
| 서버 | `qtai-server` build 통과 |
| Flutter | `flutter analyze`, 관련 테스트 통과 |
| 문서 | 2026-06-04 workflow/report 작성 |

## 6. 후속 확인

- GitHub Actions의 `ci-all`, `Requirements Guard`, `spring-build`, `flutter-test`가 새 커밋 기준으로 다시 성공하는지 확인한다.
- 실제 성경 본문 seed/import가 들어오면 테스트/로컬 fixture 사용 여부를 다시 정리한다.
