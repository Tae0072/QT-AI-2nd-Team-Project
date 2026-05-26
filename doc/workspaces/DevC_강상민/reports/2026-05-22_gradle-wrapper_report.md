# Report - 2026-05-22 Gradle Wrapper 추가

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `chore/build-gradle-wrapper` |
| PR 대상 | `dev` |
| 관련 범위 | 빌드/검증 환경 |

## 작업 목적

로컬 환경에 전역 Gradle이 없어도 `qtai-server` 빌드와 테스트를 실행할 수 있도록 Gradle Wrapper를 추가했다.

## 변경 내용

1. `qtai-server`에 Gradle Wrapper를 추가했다.
2. Wrapper Gradle 버전은 Java 21과 Spring Boot 3.3.4 기준에 맞춰 `8.10.2`로 고정했다.
3. 배포 타입은 CI와 로컬 검증에 필요한 런타임만 포함하는 `bin`을 사용했다.
4. 이후 서버 검증 명령은 전역 `gradle` 대신 `./qtai-server/gradlew.bat` 기준으로 실행할 수 있다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/gradlew` | Linux/macOS용 Gradle Wrapper 실행 스크립트 |
| `qtai-server/gradlew.bat` | Windows용 Gradle Wrapper 실행 스크립트 |
| `qtai-server/gradle/wrapper/gradle-wrapper.jar` | Wrapper 실행 런처 |
| `qtai-server/gradle/wrapper/gradle-wrapper.properties` | Gradle 8.10.2 distribution 설정 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `./qtai-server/gradlew.bat -p qtai-server --version` | Gradle 8.10.2 확인 |
| `./qtai-server/gradlew.bat -p qtai-server test` | `BUILD SUCCESSFUL`, `3 actionable tasks: 3 up-to-date` |

## 후속 작업

- W2 서버 기능 구현 검증 시 전역 `gradle` 명령 대신 Gradle Wrapper 명령을 사용한다.
- 전체 품질 게이트를 실행할 때도 `./qtai-server/gradlew.bat -p qtai-server ...` 형식으로 명령을 정리한다.
