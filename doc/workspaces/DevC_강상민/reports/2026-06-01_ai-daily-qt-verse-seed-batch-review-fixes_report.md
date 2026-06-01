# Report - 2026-06-01 ai-daily-qt-verse-seed-batch-review-fixes

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/ai-daily-verse-seed-batch` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-daily-qt-verse-seed-batch-review-fixes.md` |
| 관련 F-ID | F-02, F-14 |

## 실행 요약

`ai-daily-qt-verse-seed-batch` 코드 리뷰에서 나온 테스트/가독성/방어 로직 지적사항을 반영했다. 신규 API, OpenAPI, DB schema 변경은 없고, 기존 00:05 KST scheduler 정책은 유지했다.

## 변경 내용

- `AiDailyQtVerseExplanationSeedService`
  - `seedToday()`에 `@Transactional(propagation = Propagation.NOT_SUPPORTED)`를 명시했다.
  - 오늘 QT 응답과 passage content context에 `Objects.requireNonNull` 가드를 추가했다.
  - skip 조회 repository 호출을 의도 기반 메서드명으로 변경했다.
- repository query
  - `findReadyExplanationBibleVerseTargetIds(...)`로 ready asset skip 조회 의도를 고정했다.
  - `findActiveExplanationBibleVerseTargetIds(...)`로 active job skip 조회 의도를 고정했다.
- 테스트
  - 빈 verse ids, invalid qtPassageId/verseId, null use case 응답, prompt 미존재 warn 로그 검증을 추가했다.
  - repository 테스트 메서드명과 호출부를 새 query 이름에 맞췄다.
  - `AiGeneratedAssetRepositoryTest.nextGenerationJobId` 필드를 클래스 상단으로 이동했다.
- 문서
  - 00:05 KST는 내부 사전 생성 시딩이고, 기존 04:00 KST는 사용자 노출/cache refresh 기준임을 기존 workflow/report에 명시했다.
  - Windows `.\gradlew.bat`와 Unix/CI `./gradlew` 검증 표기를 함께 남겼다.

## TDD 기록

1. service/repository 테스트를 먼저 보강했다.
2. `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeed*"`에서 새 repository 메서드 미구현으로 `compileTestJava` 실패를 확인했다.
3. repository query와 service 구현을 최소 변경으로 맞췄다.
4. seed/repository/job/build 검증을 다시 실행했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeed*"` | 성공 |
| `.\gradlew.bat test --tests "*AiGenerationJobRepositoryTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiGeneratedAssetRepositoryTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiGenerationJob*"` | 성공 |
| `.\gradlew.bat build` | 성공 |
| `git diff --check` | 성공. CRLF 변환 warning만 출력 |
| `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | 매칭 없음. 추가 금지 import 없음 |

Unix/CI 환경에서는 `qtai-server` 기준 `./gradlew`로 같은 Gradle task를 실행한다.

## 수용 기준 확인

- 코드 리뷰의 실제 반영 항목 처리: 충족.
- 00:05/04:00 정책 관계 문서화: 충족.
- 신규 API/OpenAPI/DB schema 변경 없음: 충족.
- 관련 테스트와 build 통과: 충족.
- 금지 import 검색: 추가 금지 import 없음.

## 후속 작업

- schedlock 또는 DB unique constraint 기반 중복 실행 race 보강은 별도 PR로 남긴다.
- batch 실패 알림/모니터링 연동은 운영 정책 확정 후 별도 PR로 남긴다.
