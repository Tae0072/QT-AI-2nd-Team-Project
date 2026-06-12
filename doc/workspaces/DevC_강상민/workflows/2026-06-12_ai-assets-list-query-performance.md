# Workflow - 2026-06-12 ai-assets-list-query-performance

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-03 |
| 트리거 | 관리자 `AI 산출물 검증` 화면에서 조회가 약 1.9초 걸려 체감상 느림 |
| 기준 문서 | `qtai-server/apis/api-v1/openapi.yaml`, `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java`, `qtai-server/admin-server/src/main/resources/db/migration/V6__create_auth_ai_explanation_tables.sql` |
| 해당 경로 | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java`, `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java`, `qtai-server/admin-server/src/main/resources/db/migration/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

AI 산출물 목록 조회의 서버 쿼리 비용을 줄인다. 현재 목록 API는 데이터 20건 조회와 전체 count 조회를 수행하는데, count 쿼리에서도 최신 검증 로그를 찾는 correlated join을 매번 수행한다. `checklistVersionId` 필터가 없으면 count에는 최신 검증 로그가 필요 없으므로 불필요한 join을 제거한다.

또한 목록 정렬과 최신 검증 로그 조회에 필요한 인덱스를 추가해 운영 DB에서 스캔 비용을 줄인다.

## 범위

- `AdminAiAssetQueryRepository.findAll`의 count 쿼리를 `checklistVersionId` 필터 유무에 따라 분리한다.
- `checklistVersionId`가 없으면 count 쿼리에서 `AiValidationLog` join을 제거한다.
- `ai_generated_assets` 목록 정렬/상태 필터용 인덱스를 추가한다.
- `ai_validation_logs` 최신 로그 조회용 인덱스를 추가한다.
- admin-server와 service-ai에 중복된 QueryRepository 구현은 같은 내용으로 맞춘다.

## 제외 범위

- 목록 응답 필드 변경
- 페이지네이션 계약 변경
- 검증 결과 필터 신규 추가
- 프론트 무한스크롤 또는 캐싱 도입
- DB explain 자동 테스트 추가

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java` | count 쿼리 분리 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java` | 중복 구현 동기화 |
| Create | `qtai-server/admin-server/src/main/resources/db/migration/V37__add_ai_asset_list_performance_indexes.sql` | 목록 조회 성능 인덱스 추가 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-list-query-performance_report.md` | 원인, 수정 내용, 검증 결과 기록 |

## 구현 순서

1. 브라우저에서 조회 클릭부터 로딩 종료까지 시간을 측정한다.
2. 목록 API 서버 쿼리 구조와 기존 인덱스를 확인한다.
3. count 쿼리에서 최신 검증 로그 join을 조건부로만 사용하도록 분리한다.
4. 목록 정렬/상태 필터와 최신 검증 로그 조회용 인덱스 마이그레이션을 추가한다.
5. admin-server와 service-ai 중복 QueryRepository 구현을 맞춘다.
6. 서버 테스트 또는 최소 컴파일 검증을 실행한다.
7. 브라우저에서 조회 시간을 다시 측정한다.
8. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | 쿼리 성능 개선 작업으로 별도 자동 테스트는 추가하지 않고 서버 테스트/컴파일과 브라우저 수동 계측으로 검증 |

## 수용 기준

- [ ] `checklistVersionId`가 없는 목록 count 쿼리는 `AiValidationLog` join을 수행하지 않는다.
- [ ] `checklistVersionId`가 있는 경우 기존 필터 의미는 유지된다.
- [ ] 목록 응답 필드와 페이지네이션 계약은 바뀌지 않는다.
- [ ] 성능 인덱스 마이그레이션이 추가된다.
- [ ] 서버 검증 명령이 성공하거나, 실패 시 기존 환경 문제를 명확히 기록한다.
- [ ] 브라우저에서 조회 지연이 줄었는지 계측 결과를 기록한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 하나의 목록 조회 경로에 집중되어 있다.
- admin-server와 service-ai 중복 구현을 같은 맥락에서 동기화해야 한다.
- 브라우저 계측과 서버 쿼리 변경 검증이 순차적으로 이어져야 한다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 서버 쿼리 수정, 인덱스 추가, 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
./gradlew -p qtai-server :admin-server:test --tests "*Ai*"
./gradlew -p qtai-server :admin-server:compileJava
npm.cmd --prefix admin-web run typecheck
git diff --check
rg -n "[T]BD|[T]ODO|[p]laceholder|\\?\\?|[나]중|[적]절" doc/workspaces/DevC_강상민/workflows/2026-06-12_ai-assets-list-query-performance.md doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-list-query-performance_report.md
```

브라우저 검증:

- `http://localhost:5173/ai-assets`에서 조회 버튼 클릭
- 클릭부터 테이블 로딩 종료까지 시간 측정

## 후속 작업으로 남길 항목

- 더 줄여야 하면 최신 검증 로그를 별도 denormalized column으로 저장하거나, 목록 API에 count 생략/커서 페이지네이션을 도입하는 별도 설계가 필요하다.
