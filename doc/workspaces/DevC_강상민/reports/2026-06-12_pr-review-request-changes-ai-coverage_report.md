# Report - 2026-06-12 PR 리뷰 REQUEST_CHANGES 대응

## 요약

Claude 자동 코드 리뷰에서 차단 사유로 지적된 AI 주석자료 동기화와 AI 산출물 쿼리 테스트 누락을 보강했다. 핵심은 `CommentaryMaterialService`가 리스트 구조를 갖고 있으면서 첫 번째 material만 사용하던 동작을 실제 다중 material 누적으로 바꾸고, admin-server/service-ai 양쪽 복사본의 테스트를 맞춘 것이다.

## 대응 항목

### 1. CommentaryMaterialService 다중 material 누락

- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/CommentaryMaterialService.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/CommentaryMaterialService.java`

기존에는 repository가 여러 material mapping을 반환해도 첫 material id만 선택하고 나머지는 무시했다. `LinkedHashMap<Long, MaterialAccumulator>`로 material별 verse id를 누적하도록 변경했다.

결과:

- repository 정렬 순서를 유지한다.
- `commentaryMaterialIds`에 모든 material id가 들어간다.
- `materials` excerpt 목록에 각 material과 verse id 매핑이 모두 들어간다.
- admin-server와 service-ai 복사본의 동작이 동일하다.

### 2. Commentary 테스트 누락

- `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/CommentaryMaterialServiceTest.java`
- `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/CommentaryMaterialServiceTest.java`

service-ai 기존 테스트를 다중 material 기준으로 갱신했고, admin-server에도 동일한 단위 테스트를 추가했다.

검증 내용:

- 다중 material이 모두 context에 포함된다.
- interleaved row에서도 material별 verse id가 누락되지 않는다.
- mapping이 없으면 empty context를 반환한다.

### 3. AdminAiAssetQueryRepository 테스트 누락

- `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepositoryTest.java`

H2 기반 SpringBootTest로 실제 JPQL 쿼리를 검증했다.

검증 내용:

- `checklistVersionId` 필터가 없으면 count 쿼리가 validation join 없이 전체 asset 수를 반환한다.
- `checklistVersionId` 필터가 있으면 최신 validation log 기준으로 목록과 count가 함께 제한된다.
- `findActiveGenerationJob`은 `QUEUED/RUNNING` job만 반환하고 `SUCCEEDED` job은 제외한다.

## 검증 명령

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.ai.internal.CommentaryMaterialServiceTest" --tests "com.qtai.domain.ai.internal.AdminAiAssetQueryRepositoryTest"
```

결과: 성공

```powershell
.\gradlew.bat :service-ai:test --tests "com.qtai.domain.ai.internal.CommentaryMaterialServiceTest" :service-ai:compileJava :admin-server:compileJava
```

결과: 성공

## 남은 이슈

PR 규모가 큰 문제와 report/workflow가 실제 코드 변경보다 넓은 문제는 구조적 PR 분리 이슈다. 이번 커밋에서는 차단 사유였던 AI 주석자료 동작 누락과 테스트 공백을 해소했다.
