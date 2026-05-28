# JaCoCo — 코드 커버리지 측정

> **왜 배워야 하나:** QT-AI의 검증 명령에 `jacocoTestReport`와 `jacocoTestCoverageVerification`이 포함되어 있다. JaCoCo는 "테스트가 코드의 몇 %를 실행했는가"를 측정하는 도구다. 노션에서 다루지 않은 품질 관리 도구다.

---

## 1. 코드 커버리지가 뭔가?

코드 커버리지는 **테스트 코드가 전체 코드 중 얼마나 실행했는지**를 퍼센트로 보여준다.

```
전체 코드: 100줄
테스트가 실행한 코드: 80줄
→ 코드 커버리지: 80%
```

커버리지가 높을수록 테스트가 많은 코드를 검증했다는 뜻이다.

## 2. JaCoCo가 뭔가?

JaCoCo(Java Code Coverage)는 Java에서 가장 널리 쓰이는 코드 커버리지 측정 도구다. 테스트를 실행하면서 어떤 코드가 실행됐고 어떤 코드가 실행되지 않았는지 추적한다.

## 3. 커버리지 종류

| 종류 | 의미 | 예시 |
|------|------|------|
| Line Coverage | 실행된 줄 수 비율 | 100줄 중 80줄 실행 = 80% |
| Branch Coverage | 분기(if/else) 실행 비율 | if-else 2가지 중 1가지만 테스트 = 50% |
| Method Coverage | 호출된 메서드 비율 | 10개 메서드 중 8개 호출 = 80% |
| Class Coverage | 테스트된 클래스 비율 | 20개 클래스 중 15개 테스트 = 75% |

## 4. Gradle에서 JaCoCo 설정

```kotlin
// build.gradle.kts
plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

// 커버리지 리포트 생성
tasks.jacocoTestReport {
    dependsOn(tasks.test)          // 테스트 실행 후 리포트 생성
    reports {
        html.required = true        // HTML 리포트 생성
        xml.required = true         // XML 리포트 (CI 도구용)
    }
}

// 커버리지 최소 기준 검증
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.60".toBigDecimal()  // 최소 60% 커버리지
            }
        }
    }
}
```

## 5. 실행 명령

```bash
# QT-AI 검증 명령 (CLAUDE.md §11)
./gradlew -p qtai-server test jacocoTestReport
./gradlew -p qtai-server jacocoTestCoverageVerification
```

- `jacocoTestReport`: 테스트 실행 후 커버리지 리포트를 HTML로 생성
- `jacocoTestCoverageVerification`: 커버리지가 최소 기준 이상인지 확인 (미달이면 빌드 실패)

## 6. 리포트 확인

리포트는 `build/reports/jacoco/test/html/index.html`에 생성된다. 브라우저로 열면:

- 초록색: 테스트가 실행한 코드
- 빨간색: 테스트가 실행하지 않은 코드
- 노란색: 분기 중 일부만 테스트한 코드

## 7. QT-AI에서의 적용

CLAUDE.md에 명시된 필수 테스트 영역이 커버리지에 반영되어야 한다:

- Today QT 캐시 동작 → qt 도메인 커버리지
- AI Q&A 차단/검증 → ai 도메인 커버리지
- 도메인 간 금지 import → ArchUnit 테스트 커버리지
- 이벤트 핸들러 실패 로그 → notification 등 이벤트 관련 커버리지

커버리지가 최소 기준에 미달하면 PR이 머지될 수 없다.

## 8. 주의사항

- 커버리지 100%가 목표가 아니다. 핵심 비즈니스 로직과 엣지 케이스를 우선 커버한다.
- getter/setter, 단순 DTO 같은 코드는 커버리지에서 제외할 수 있다.
- 커버리지가 높아도 테스트의 **질**이 나쁘면 의미가 없다 (검증 없이 실행만 하는 테스트).

## 9. 참고 자료

- JaCoCo 공식 문서: https://www.jacoco.org/jacoco/
- Gradle JaCoCo 플러그인: https://docs.gradle.org/current/userguide/jacoco_plugin.html
