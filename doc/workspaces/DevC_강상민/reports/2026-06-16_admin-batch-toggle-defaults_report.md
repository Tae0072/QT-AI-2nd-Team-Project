# Report - 2026-06-16 admin-batch-toggle-defaults

## 요약

`admin-server` 복사본의 SU 오늘 QT 수집과 AI 해설 시딩 scheduler가 설정 누락 시 기본 실행되지 않도록 `@Value` fallback을 `false`로 정합화했다. `application.yml`의 토글 기본값은 이미 `false`였고, 이번 변경은 코드 레벨 fallback까지 같은 정책으로 맞춘 것이다.

## 변경 내용

- `SuTodayPassageImportScheduler`
  - `qt.today-source.sum.enabled` fallback을 `true`에서 `false`로 변경.
- `AiDailyQtVerseExplanationSeedScheduler`
  - `ai.daily-qt-verse-seed.enabled` fallback을 `true`에서 `false`로 변경.
- `application.yml`
  - `${QT_TODAY_SUM_ENABLED:false}`, `${AI_DAILY_QT_VERSE_SEED_ENABLED:false}` 기본값 유지 확인.
- `service-bible`, `service-ai`
  - 배치 소유 서비스의 기본 동작은 변경하지 않음.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew :admin-server:test --tests com.qtai.domain.qt.internal.SuTodayPassageImportSchedulerToggleTest` | 통과 |
| `.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.AiDailyQtVerseExplanationSeedSchedulerToggleTest` | 통과 |
| `.\gradlew :admin-server:bootJar` | 통과 |
| `git diff --check` | 통과 |
| `rg -n "qt\.today-source\.sum\.enabled:true|ai\.daily-qt-verse-seed\.enabled:true" qtai-server/admin-server` | 결과 없음 |
| `git diff -- qtai-server/service-bible qtai-server/service-ai` | 결과 없음 |

## 수용 기준

- [x] `admin-server` yml의 두 배치 토글 기본값이 `false`다.
- [x] `admin-server` scheduler 코드에 `enabled:true` fallback이 남지 않는다.
- [x] `service-bible`, `service-ai`의 scheduler 기본 동작은 변경되지 않았다.
- [x] 지정 테스트와 빌드 검증이 통과한다.

## 비고

배치 소유권 단일화, scheduler 클래스 제거, 모듈 이동은 이번 작업 범위에서 제외했다.
