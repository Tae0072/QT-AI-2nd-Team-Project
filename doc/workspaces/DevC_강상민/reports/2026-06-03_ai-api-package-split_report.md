# Report - 2026-06-03 ai-api-package-split

## Summary

- 브랜치 `refactor/ai-api-package-split`를 생성했다.
- AI 도메인 공개 계약을 기능별 하위 패키지로 분리했다.
- `domain.ai.web`, `domain.ai.internal`, 관련 테스트의 import를 새 패키지로 수정했다.
- `AiUseCaseContractTest`를 새 하위 `dto` 패키지 구조에 맞게 갱신했다.

## 구현 내용

- `domain.ai.api.generation`
  - `CreateAiGenerationJobUseCase`
  - `RegisterAiGeneratedAssetUseCase`
  - `CreateAiGenerationJobCommand`, `CreateAiGenerationJobResult`
  - `RegisterAiGeneratedAssetCommand`, `RegisterAiGeneratedAssetResult`
- `domain.ai.api.qa`
  - `RequestAiQaUseCase`
  - `GetAiQaResultUseCase`
  - `RequestAiQaCommand`, `RequestAiQaResult`
  - `GetAiQaResultCommand`, `GetAiQaResultResult`
- `domain.ai.api.validation`
  - `CreateValidationReferenceJobUseCase`
  - `GetValidationReferenceJobUseCase`
  - `ExpireValidationReferenceJobUseCase`
  - `RegisterAiValidationLogUseCase`
  - validation reference job DTO와 validation log DTO
- `domain.ai.api.admin.asset`
  - `ListAdminAiAssetsUseCase`
  - `GetAdminAiAssetUseCase`
  - `ReviewAiAssetUseCase`
  - `RegenerateAiAssetUseCase`
  - 관리자 산출물 조회, 심사, 재생성 DTO
- `domain.ai.api.admin.monitoring`
  - `GetAdminAiMonitoringUseCase`
  - `ListAdminAiBatchRunLogsUseCase`
  - 관리자 모니터링과 batch run log DTO
- `domain.ai.api.admin.checklist`
  - `ListAdminAiValidationChecklistsUseCase`
  - `CreateAdminAiValidationChecklistUseCase`
  - `ActivateAdminAiValidationChecklistUseCase`
  - `RetireAdminAiValidationChecklistUseCase`
  - 관리자 체크리스트 DTO

## 변경된 소비자

- `qtai-server/src/main/java/com/qtai/domain/ai/web/**`
  - Controller import를 새 UseCase/DTO 패키지로 변경했다.
- `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`
  - 구현체와 query repository/service import를 새 DTO 패키지로 변경했다.
- `qtai-server/src/test/java/com/qtai/domain/ai/**`
  - AI web/internal/api 테스트 import를 새 패키지로 변경했다.

## 검증 결과

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
java -version
```

- 결과: PASS
- 확인 버전: Temurin OpenJDK `21.0.11`
- 설치 위치: `C:\Users\HSystem\.jdks\temurin-21`
- 환경변수: User scope `JAVA_HOME`과 User `Path`에 `C:\Users\HSystem\.jdks\temurin-21\bin` 등록

```powershell
git diff --check
```

- 결과: PASS

```powershell
rg "import com\.qtai\.domain\.ai\.api\.[A-Z]" qtai-server/src/main/java qtai-server/src/test/java
```

- 결과: 매치 없음
- 의미: 기존 루트 `com.qtai.domain.ai.api.<UseCase>` import가 남아 있지 않다.

```powershell
rg "import com\.qtai\.domain\.ai\.api\.dto\.[A-Z]" qtai-server/src/main/java qtai-server/src/test/java
```

- 결과: 매치 없음
- 의미: 기존 `com.qtai.domain.ai.api.dto.<Dto>` import가 남아 있지 않다.

```powershell
cd qtai-server
.\gradlew.bat test --tests com.qtai.domain.ai.api.AiUseCaseContractTest
```

- 결과: PASS
- 비고: `C:\Users\HSystem\.jdks\temurin-21` portable JDK를 `JAVA_HOME`으로 지정해 실행했다.

```powershell
cd qtai-server
.\gradlew.bat --no-watch-fs test --tests "*Ai*"
```

- 결과: PASS
- 비고: Windows file watcher 경고 회피를 위해 `--no-watch-fs`를 사용했다.

```powershell
cd qtai-server
.\gradlew.bat --no-watch-fs build
```

- 결과: PASS

```powershell
cd qtai-server
.\gradlew.bat --no-watch-fs test jacocoTestReport
```

- 결과: FAIL
- 사유: `jacocoTestReport` 태스크가 root project `qtai-server`에 등록되어 있지 않다.

```powershell
cd qtai-server
.\gradlew.bat --no-watch-fs tasks --all | Select-String -Pattern 'jacoco|coverage|Coverage'
```

- 결과: PASS
- 출력: 매치 없음
- 의미: 현재 Gradle 태스크 목록에 Jacoco/coverage 관련 태스크가 없다.

## 실행하지 못한 검증

- `.\gradlew.bat jacocoTestCoverageVerification`: Jacoco/coverage 태스크가 Gradle에 등록되어 있지 않아 미실행
- `gitleaks detect --source . --redact --exit-code 1`: `gitleaks` 명령이 PATH에 없음
- Spectral lint: `npx` 명령이 PATH에 없음

## 리스크와 후속 조치

- 패키지 이동 폭이 커서 Git status에는 delete/add가 많이 표시된다. PR 전 rename 인식 여부는 `git diff --summary` 또는 IDE diff로 확인한다.
- Gradle 검증에는 `C:\Users\HSystem\.jdks\temurin-21` portable JDK를 사용했다. User scope 환경변수에 등록했으므로 새 터미널부터 기본 Java로 사용할 수 있다.
- Jacoco/coverage 태스크 미등록은 본 리팩터링과 별개 이슈이므로 후속 PR에서 Gradle 설정 기준을 정리한다.

## 리뷰 반영

- `REQUEST_CHANGES` 사유였던 Java 한글 리터럴 mojibake를 `dev` 기준 원문으로 복구했다.
- `AiService.java`의 BusinessException 메시지는 최종 `dev` 대비 diff에서 import 변경만 남도록 정리했다.
- 한글 리터럴이 포함된 테스트 파일도 `dev` 기준 원문으로 복구한 뒤 import 변경만 다시 적용했다.
- `AiUseCaseContractTest`에 하위 패키지별 UseCase 분류 기대값을 추가해 `qa`, `generation`, `admin.asset`, `admin.monitoring`, `admin.checklist`, `validation` 배치 회귀를 막는다.

## 참고

- HTTP API 경로, request/response 필드, DB schema, 서비스 동작은 변경하지 않았다.
- raw provider response, prompt 원문, secret/token/password 예시는 추가하지 않았다.
