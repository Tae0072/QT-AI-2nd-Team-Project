# Report — 2026-05-28 admin-ai-checklist-service-startup-fix

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `bugfix/admin-ai-checklist-service-startup` |
| PR 대상 | `dev` |
| 관련 커밋 | `ed1466d fix(ai): 관리자 AI 체크리스트 서비스 기동 실패 수정` |
| 실행 경로 | dev 머지 후 서버 기동 검증 중 발견한 startup failure 대응 |
| 관련 도메인 | `ai`, `audit` |

## 증상

최신 `dev`에서 `qtai-server` 전체 빌드는 통과했지만, 실제 JAR 기동 smoke test에서 Spring ApplicationContext 초기화가 실패했다.

첫 실패 로그:

```text
Error creating bean with name 'adminAiValidationChecklistService':
Failed to instantiate [com.qtai.domain.ai.internal.AdminAiValidationChecklistService]:
No default constructor found
```

`AdminAiValidationChecklistService` 수정 후 재검증했을 때 같은 패턴으로 `AuditService`에서도 실패했다.

```text
Error creating bean with name 'auditService':
Failed to instantiate [com.qtai.domain.audit.internal.AuditService]:
No default constructor found
```

## 원인

두 서비스 모두 운영용 public 생성자와 테스트용 package-private 보조 생성자를 함께 가지고 있었다.

Spring은 생성자가 하나만 있으면 `@Autowired` 없이도 생성자 주입을 수행할 수 있지만, 생성자가 2개 이상이면 어떤 생성자를 주입 경로로 사용할지 명확히 지정해야 한다. 기존 코드는 명시가 없어 Spring이 기본 생성자 기반 생성 경로로 빠졌고, 기본 생성자가 없어서 ApplicationContext 기동이 실패했다.

## 해결

Spring이 사용할 public 생성자에 `@Autowired`를 명시했다. 테스트에서 `Clock`을 주입하기 위한 package-private 보조 생성자는 유지했다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistService.java` | public 생성자에 `@Autowired` 추가 |
| `qtai-server/src/main/java/com/qtai/domain/audit/internal/AuditService.java` | public 생성자에 `@Autowired` 추가 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AuditServiceTest" --tests "*AdminAiValidationChecklistServiceTest"` | PASS |
| `.\gradlew.bat build` | PASS |
| `npx.cmd @apidevtools/swagger-cli validate qtai-server/apis/api-v1/openapi.yaml` | PASS |
| JAR startup smoke test (`SPRING_PROFILES_ACTIVE=local`, 임시 RSA 키, `--server.port=0`) | PASS |
| `git diff --check` | PASS. CRLF 경고만 출력 |

## 비고

- Jacoco task는 현재 Gradle task 목록에 없어 실행하지 못했다.
- `.spectral.yaml`이 저장소에 없어 Spectral lint는 실행하지 못했다.
- `gitleaks` 실행 파일이 로컬에 없어 gitleaks 검증은 실행하지 못했다.
