# Workflow - 2026-06-03 ai-api-package-split

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `refactor/ai-api-package-split` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | AI 도메인 `api` 패키지의 공개 계약이 한 패키지에 집중되어 있어 기능별 하위 패키지로 분리 준비 |
| 기준 문서 | `AGENTS.md`, `CODE_CONVENTION.md`, `03_아키텍처_정의서.md` 도메인 경계 기준 |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/api/**`, `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/main/java/com/qtai/domain/ai/web/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |
| 리포트 | `doc/workspaces/DevC_강상민/reports/2026-06-03_ai-api-package-split_report.md` |

## 작업 목표

AI 도메인의 공개 UseCase와 DTO를 기능별 하위 패키지로 나누어 계약의 책임을 명확히 한다. 다른 도메인에서 `com.qtai.domain.ai.api.*UseCase`를 직접 사용하는 사용처가 없는 상태를 전제로, AI 내부 구현체와 AI web controller, 관련 테스트 import만 수정한다.

이번 작업은 동작 변경이 아니라 패키지 구조 정리다. HTTP API 경로, 요청/응답 필드, 서비스 로직, DB schema, OpenAPI 계약은 변경하지 않는다.

## 범위

- `api/generation`: 생성 작업 생성, 생성 산출물 등록 계약
- `api/qa`: Q&A 요청과 결과 조회 계약
- `api/admin/asset`: 관리자 AI 산출물 조회, 심사, 재생성 계약
- `api/admin/monitoring`: 관리자 AI 모니터링과 batch run log 조회 계약
- `api/admin/checklist`: 관리자 AI 검증 체크리스트 계약
- `api/validation`: 검증 로그와 validation reference job 계약
- 각 하위 패키지의 DTO는 같은 책임 영역의 `dto` 패키지로 이동한다.
- AI web/internal/test의 import를 새 패키지에 맞춘다.
- `AiUseCaseContractTest`는 새 패키지 구조를 허용하도록 갱신한다.

## 제외 범위

- HTTP endpoint 경로 변경
- request/response 필드명 변경
- DB migration, schema, entity 변경
- 관리자 권한 정책 변경
- 다른 도메인의 AI UseCase 신규 의존 추가
- OpenAPI 문서 변경
- 비AI 도메인 리팩터링

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/generation/**` | 생성 job과 생성 산출물 등록 UseCase/DTO 배치 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/qa/**` | Q&A 요청과 결과 조회 UseCase/DTO 배치 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/admin/asset/**` | 관리자 산출물 조회, 심사, 재생성 UseCase/DTO 배치 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/admin/monitoring/**` | 관리자 모니터링과 batch run log UseCase/DTO 배치 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/admin/checklist/**` | 관리자 검증 체크리스트 UseCase/DTO 배치 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/validation/**` | 검증 로그와 reference job UseCase/DTO 배치 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**` | 구현체의 import를 새 api 패키지로 수정 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/**` | controller의 import를 새 api 패키지로 수정 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 공개 계약 구조와 DTO 노출 제한 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/**` | web/internal 테스트 import 수정 |

## 구현 순서

1. `dev`에서 `refactor/ai-api-package-split` 브랜치를 생성한다.
2. `rg "com\.qtai\.domain\.ai\.api"`로 AI api 계약 사용처를 확인한다.
3. UseCase 파일을 기능별 하위 패키지로 이동하고 package 선언을 수정한다.
4. DTO 파일을 동일 책임 영역의 하위 `dto` 패키지로 이동하고 package 선언을 수정한다.
5. `domain.ai.web`, `domain.ai.internal`, 관련 테스트 import를 새 패키지로 수정한다.
6. `AiUseCaseContractTest`의 UseCase import를 명시하고, DTO package 검증을 `com.qtai.domain.ai.api.*.dto` 구조에 맞춘다.
7. 기존 루트 `com.qtai.domain.ai.api.<UseCase>` import와 기존 `com.qtai.domain.ai.api.dto.<Dto>` import가 남지 않았는지 검색한다.
8. 가능한 범위의 Gradle 테스트와 정적 검증을 실행한다.
9. 실행 결과와 제한 사항을 report 문서에 기록한다.

## 테스트 보강 목록

| 테스트 파일 | 검증 내용 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | UseCase가 interface인지, method가 command/query와 result/response 형태를 유지하는지, DTO가 AI api 하위 `dto` 패키지에 있는지 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/*ControllerTest.java` | controller가 새 UseCase/DTO 패키지를 사용해 기존 응답 계약을 유지하는지 컴파일로 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/*Test.java` | service/query/repository 테스트가 새 DTO 패키지를 사용해 기존 로직을 유지하는지 컴파일로 검증 |

## 수용 기준

- [ ] AI api 루트에 직접 놓인 UseCase 파일이 기능별 하위 패키지로 이동되어 있다.
- [ ] 기존 `com.qtai.domain.ai.api.dto` 패키지에 DTO가 남아 있지 않다.
- [ ] `domain.ai.web`과 `domain.ai.internal`의 import가 새 패키지를 바라본다.
- [ ] `import com.qtai.domain.ai.api.<UseCase>` 형태의 루트 UseCase import가 남아 있지 않다.
- [ ] `import com.qtai.domain.ai.api.dto.<Dto>` 형태의 기존 DTO import가 남아 있지 않다.
- [ ] HTTP endpoint, DB schema, 비AI 도메인 로직은 변경하지 않는다.
- [ ] raw provider response, prompt 원문, secret/token/password 예시를 새 DTO나 문서에 추가하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경이 동일한 AI api 계약 파일과 import 치환에 강하게 연결되어 있어 병렬 편집 시 import 충돌 가능성이 크다.
- 기능 구현이 아니라 패키지 이동 리팩터링이므로 한 에이전트가 전체 경로를 추적하며 일관성을 확인하는 편이 안전하다.
- 테스트 보강도 새 구조에 맞춘 계약 테스트 갱신이 중심이라 구현과 분리해 병렬화할 이점이 작다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 파일 이동, import 수정, 계약 테스트 갱신, 정적 검증을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests com.qtai.domain.ai.api.AiUseCaseContractTest
.\gradlew.bat test --tests "*Ai*"
.\gradlew.bat build
cd ..
git diff --check
rg "import com\.qtai\.domain\.ai\.api\.[A-Z]" qtai-server/src/main/java qtai-server/src/test/java
rg "import com\.qtai\.domain\.ai\.api\.dto\.[A-Z]" qtai-server/src/main/java qtai-server/src/test/java
gitleaks detect --source . --redact --exit-code 1
```

Gradle 검증은 Java 17 이상이 필요하다. 로컬 Java가 8뿐이면 실패 사유를 report에 기록하고, Java 21 환경에서 재실행 대상으로 남긴다.

## 후속 작업으로 남길 항목

- Java 21 환경에서 Gradle 테스트와 build 재실행
- PR 전 `gitleaks`와 Spectral 실행 가능 환경 확인
- 필요 시 ArchUnit/Modulith 경계 테스트에 AI api 하위 패키지 구조 기대값 추가
