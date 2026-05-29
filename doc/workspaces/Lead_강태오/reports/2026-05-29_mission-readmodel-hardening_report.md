# 리포트 — mission 읽기 모델 하드닝 (#141 WARN 후속)

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `feature/mission-readmodel-hardening` → PR 대상 `dev`

## 1. 한 줄 요약

머지된 PR #141의 자동 리뷰 WARN 2건(HIDDEN 노출, 리포지토리 슬라이스 테스트 부재)을 후속 처리했다. 전체 회귀 통과.

## 2. 변경 사항

| 구분 | 파일 | 내용 |
|------|------|------|
| 수정 | `mission/internal/MissionService.java` | `getMissionProgress`에서 HIDDEN 정의의 진행률 제외(ACTIVE만 노출, 정의 누락은 방어적 유지) |
| 수정 | `test/.../mission/internal/MissionServiceTest.java` | HIDDEN 제외 검증 테스트 1건 추가(총 5건) |
| 신규 | `test/.../mission/internal/MissionRepositoryTest.java` | @DataJpaTest 슬라이스 — UNIQUE/파생쿼리/precision 검증 |

## 3. HIDDEN 정책 결정 근거

- `07_요구사항`·`25_기능명세`에 미션 HIDDEN 노출 기준 명시 없음.
- ERD 전반의 HIDDEN 의미(praise_songs/sharing 등) = 사용자 노출에서 제외.
- 코드베이스 패턴: `PraiseService.listActive`가 status=ACTIVE만 반환.
- → mission도 동일하게 **HIDDEN 정의 진행률은 대시보드에서 제외**. 단, 정의 자체가 조회되지 않는 예외 상황(삭제 등)은 진행 기록 보존을 위해 유지.

## 4. 테스트 결과

| 테스트 | 케이스 | 결과 |
|--------|--------|------|
| MissionServiceTest | HIDDEN 정의 진행률 제외 | PASS |
| MissionRepositoryTest | code UNIQUE 중복 예외 | PASS |
| | findByIdIn 부분 조회 | PASS |
| | (member,definition,period) UNIQUE 중복 예외 / 다른 기간 허용 | PASS |
| | findByMemberIdOrderByPeriodStartDateDesc 정렬·회원필터 | PASS |
| | progress_rate DECIMAL(5,2) 보존 | PASS |

- `./gradlew test --no-daemon` (전체) → BUILD SUCCESSFUL (42s).

## 5. 남은 후속

- 진행률 배치 계산(노트 활동 집계, ERD §2.24)과 `GET /me/dashboard` wiring — note/me 도메인 연결·담당자 조율 필요(별도 작업).
