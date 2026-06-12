# Report - 2026-06-12 AI 산출물 목록 조회 성능 개선

## 요약

관리자 `AI 산출물 검증` 화면의 조회 지연을 점검하고 서버 목록 쿼리를 개선했다. 브라우저에서 조회 버튼 클릭부터 테이블 로딩 종료까지 약 1.9초가 걸렸고, 이후 재계측에서는 현재 실행 중 서버 기준으로 6.5초까지 튀는 것을 확인했다.

원인은 프론트 렌더링보다 서버 목록 API 쿼리 비용으로 판단했다. 목록 조회는 20건만 반환하지만, 전체 count 쿼리도 최신 검증 로그를 찾는 correlated join을 매번 수행하고 있었다.

## 확인한 근거

- `AdminAiAssetQueryRepository.findAll`은 목록 rows 조회와 count 조회를 각각 실행한다.
- 기존 count 쿼리는 `checklistVersionId` 필터가 없어도 `AiValidationLog latestValidation` join을 포함했다.
- 최신 검증 로그 join은 asset별로 `max(createdAt)`와 `max(id)`를 찾는 correlated subquery 구조다.
- 기존 인덱스에는 `ai_generated_assets(created_at, id)` 정렬 인덱스와 `ai_validation_logs(ai_asset_id, created_at, id)` 최신 로그 조회 인덱스가 없었다.

## 변경 내용

- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java`
  - count 쿼리용 `COUNT_FROM`, `BASE_WHERE`를 분리했다.
  - `checklistVersionId` 필터가 없으면 count 쿼리에서 `AiValidationLog` join을 제거했다.
  - enum 파싱을 rows/count 쿼리에서 중복 수행하지 않도록 지역 변수로 정리했다.
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java`
  - admin-server와 동일하게 중복 구현을 맞췄다.
- `qtai-server/admin-server/src/main/resources/db/migration/V37__add_ai_asset_list_performance_indexes.sql`
  - `ai_generated_assets(status, created_at DESC, id DESC)` 인덱스 추가
  - `ai_generated_assets(created_at DESC, id DESC)` 인덱스 추가
  - `ai_validation_logs(ai_asset_id, created_at DESC, id DESC)` 인덱스 추가

## 유지한 내용

- 목록 응답 필드와 페이지네이션 계약은 변경하지 않았다.
- `checklistVersionId` 필터가 있는 경우 최신 검증 로그 기준 필터링은 유지했다.
- 프론트 목록 UI와 페이지 크기는 변경하지 않았다.

## 검증 결과

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

```powershell
.\gradlew.bat :admin-server:compileJava
```

결과: 성공

```powershell
.\gradlew.bat :service-ai:compileJava
```

결과: 성공

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.ai.internal.AiAssetReviewServiceTest"
```

결과: 성공

## 브라우저 계측

- 변경 전 사용자 체감 경로에서 조회 버튼 클릭 후 테이블 로딩 종료까지 약 1.9초를 확인했다.
- 현재 실행 중인 로컬 서버 기준 재계측에서는 약 6.5초까지 튀었다.
- 이 재계측은 새로 컴파일한 서버 코드와 신규 DB 마이그레이션이 실행 중 프로세스에 반영되기 전 상태일 수 있다. 실제 성능 개선 효과는 서버 재시작과 Flyway 마이그레이션 적용 후 다시 확인해야 한다.

## 결론

현재 느린 조회는 프론트 테이블 렌더링보다 서버 목록 API의 DB 쿼리 비용이 원인에 가깝다. 이번 수정으로 일반 목록 count에서 불필요한 최신 검증 로그 join을 제거했고, 목록 정렬과 최신 검증 로그 조회에 필요한 인덱스를 추가했다.

## 후속 메모

서버 재시작과 마이그레이션 적용 후에도 조회가 1초 이상이면, 다음 단계는 목록 API에서 count 생략 또는 cursor pagination, 최신 검증 결과 denormalized column 저장을 검토하는 것이다.
