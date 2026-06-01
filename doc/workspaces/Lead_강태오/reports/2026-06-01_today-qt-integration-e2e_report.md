# 리포트 — Today QT 풀 컨텍스트 통합 E2E

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 브랜치: `test/today-qt-integration` → PR 대상 `dev`
- 관련: 3단계 "통합·E2E·성능" 첫 작업. CLAUDE.md §6(Today QT)/§10(필수 테스트).

## 1. 한 줄 요약

데모 1순위인 Today QT 조립 플로우를, 슬라이스·단위가 아니라 **전체 ApplicationContext를 띄워 qt + note 도메인을 실제로 관통**하는 통합 테스트로 검증했다. 본문 있음/없음 두 경로 + 시뮬레이터 상태 enum 계약을 확인. 전체 회귀 545건 통과.

## 2. 배경 — 기존 공백

- qt 도메인 테스트는 `@WebMvcTest`(usecase mock)·단위(mock)뿐이라 **실제 빈 와이어링을 관통하지 않았다.**
- "Today QT 100%"(본문 + 진입점 + 시뮬레이터 상태)는 여러 도메인이 조합되는 흐름이라, 실제 조립이 동작하는지는 풀 컨텍스트로만 검증된다.

## 3. 구현

`TodayQtFlowIntegrationTest` (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`):

- `@TestConfiguration`으로 `Clock`을 **2026-05-27 06:00 KST**(수집 배치 04:00 이후)로 고정 → "오늘" 결정을 결정적으로 만든다.
- 데이터: test 프로파일은 Flyway off(시드 없음)이므로 테스트가 직접 `QtPassage`를 시드(기존 `QtPassageFixture` 재사용, JPA 저장).
- `todayQt` 캐시는 각 테스트 전후로 비워 격리.

검증 케이스:

1. **본문 있음(HIT)**: 시드한 본문이 조립돼 반환(qtPassageId·title·passageDate·cacheStatus=HIT). 시뮬레이터 상태는 허용 enum(READY/MISSING/FAILED/DISABLED)만. note 도메인 **실호출** 결과 드래프트 없는 회원은 `draftNoteId=null`(cross-domain 무결성).
2. **본문 없음**: 04:00 이후 본문이 없으면 `simulatorStatus=DISABLED` + `cacheStatus=MISS`로 안전 응답(크래시 없음).

## 4. 변경 파일

| 구분 | 파일 |
|------|------|
| 신규 | `qtai-server/src/test/java/com/qtai/domain/qt/internal/TodayQtFlowIntegrationTest.java` |
| 신규 | 본 리포트 |

> 프로덕션 코드 변경 없음 — 테스트만 추가.

## 5. 검증

- 단독 실행: `TodayQtFlowIntegrationTest` 2건 통과(풀 컨텍스트 10.9s).
- 전체 회귀: `./gradlew test` → **545건 통과(실패 0)**.

## 6. 후속 (3단계 잔여)

- 추가 핵심 플로우 E2E: 시뮬레이터 상태→버튼 활성화 규칙, F-15 Q&A 차단(컨트롤러 구현 후), 신고→처리 도메인 흐름.
- 기본 성능/부하 점검.
- 참고: 로컬은 IDE(Cursor) Java 언어 서버가 `build/`를 잠가 gradle 출력 정리 충돌이 잦음 — `build/classes` 수동 삭제 없이 증분 실행하면 회피됨(CI 무관).
