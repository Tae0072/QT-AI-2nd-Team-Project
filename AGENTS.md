# AGENTS.md - QT-AI 서버 구현 공통 지침

이 파일은 실제 구현 저장소의 `qtai-server` 작업에서 Codex가 따라야 할 팀 공통 기준이다. 기준 문서는 문서 저장소의 `07_요구사항_정의서.md` v3.1이며, 구현 중 의사결정이 흔들리면 요구사항을 먼저 확인한다.

## 1. 프로젝트 기준

- 서비스: QT-AI, 큐티 AI 앱
- 서버 형태: 단일 `qtai-server` Modular Monolith
- Backend: Java 21, Spring Boot 3.3, Gradle, Spring Modulith, ArchUnit
- DB/cache: MySQL 8.0, 테스트 H2, Caffeine app cache
- Redis: token/rate/idempotency 등 필요 범위 검토 후 사용
- AI: DeepSeek OpenAI-compatible client
- 외부 API: 사용자 Flutter 앱(`/api/v1/**` 관리자 경로 제외 + `/oauth2/**`)과 별도 관리자 웹 프런트엔드(`/api/v1/admin/**`)가 같은 `qtai-server`를 호출한다. 관리자 UI는 Flutter 앱이 아니라 별도 웹이다. `03_아키텍처_정의서.md` v1.2 §4.9 / §13.6을 따른다.
- OAuth 경로: Kakao OAuth 시작/콜백은 `/oauth2/**` 예외 경로로 두며, 앱 콘텐츠 API를 여기에 두지 않는다.
- 기본 응답, 주석, PR 설명 언어: 한국어 우선
- 백엔드 코드 컨벤션: `CODE_CONVENTION.md`를 우선 확인

## 2. Codex 작업 방식

- 작업 전 관련 문서와 기존 코드를 먼저 확인하고, 저장소의 현재 구조와 관례를 우선한다.
- 요구사항 변경이 필요해 보이면 임의로 바꾸지 말고 Lead 검토가 필요하다고 표시한다.
- 관련 없는 리팩터링, 대규모 포맷팅, 파일 이동, 메타데이터 변경은 하지 않는다.
- 사용자가 만든 변경사항을 되돌리거나 의도를 바꾸는 경우에는 반드시 승인을 받는다.
- 최종 응답에는 변경 파일, 핵심 변경 내용, 실행한 검증 명령과 결과를 간결하게 보고한다.

## 3. 기준 문서 우선순위

문서가 구현 저장소에 복사되어 있거나 링크되어 있으면 작업 전 관련 문서를 확인한다. 문서는 저장소 루트 기준 `docs/프로젝트문서/` 아래에 있다.

1. 요구사항: `07_요구사항_정의서.md`
2. 아키텍처: `03_아키텍처_정의서.md`
3. API 계약: `04_API_명세서.md`
4. 품질 게이트: `18_코드_품질_게이트.md`
5. 용어: `23_도메인_용어사전.md`
6. Git/PR 규칙: `09_Git_규칙.md`
7. 구현 반영 순서: `22_구현_저장소_반영_체크리스트.md`
8. 기능 상세: `25_기능_명세서.md`
9. 코드 컨벤션: `CODE_CONVENTION.md`

충돌 시 `07` -> `03` -> `04` -> `18` -> `23` -> `09` 순서로 판단한다.

## 4. 서버 도메인 경계

도메인은 `member`, `bible`, `qt`, `study`, `note`, `sharing`, `report`, `notification`, `praise`, `mission`, `ai`, `admin`, `audit`를 기준으로 나눈다.

- `note`, `sharing`, `praise`는 `bible` 하위 모듈이 아니라 최상위 도메인이다.
- 다른 도메인으로 공개할 계약은 `api/` 패키지의 UseCase/DTO에 둔다.
- 다른 도메인의 Entity, Service, Repository, infrastructure 타입을 직접 import하지 않는다.
- 내부 도메인 호출은 Java Interface와 DTO를 사용한다.
- 내부 호출을 HTTP 경로로 우회하지 않는다.
- Controller는 Repository를 직접 호출하지 않는다.
- 도메인 모델은 자기 도메인 안에 둔다.
- 도메인 간 반환값은 DTO로 전달한다.

## 5. 패키지와 레이어 기준

기준은 `03_아키텍처_정의서.md` v1.1 §3.1이다.

- `domain.<name>.api`: 외부 도메인에서 호출 가능한 UseCase interface와 DTO (`api/dto`)
- `domain.<name>.internal`: Entity, enum, Service, Repository, QueryRepository, 도메인 전용 예외 등 내부 구현. 외부 도메인 접근 절대 금지
- `domain.<name>.client`: 다른 도메인의 `api/UseCase` 호출 어댑터(`client/{타도메인명}/...UseCaseMock.java`)와 도메인 전용 외부 시스템 호출 어댑터. 선택 패키지로, 다른 도메인 호출이 없거나 외부 시스템 호출이 없는 도메인은 두지 않는다.
- `domain.<name>.web`: `/api/v1/**` Controller와 외부 HTTP Request/Response DTO
- `common`: 공통 응답, 예외, 유틸
- `config`, `security`, `external`, `batch`: 공통 기술 영역. 여러 도메인이 공유하는 외부 시스템 호출은 `external/`에 둔다.

다른 도메인 호출은 항상 상대 도메인의 `api/UseCase` 인터페이스를 통한다. 통합 전에는 호출자 도메인의 `client/{타도메인명}/...UseCaseMock.java`로 임시 구현해 작업하고, 상대 도메인의 진짜 구현체가 등록되면 Mock을 삭제한다. 외부 시스템(Kakao, DeepSeek 등) 호출은 `client/{벤더명}Client`로 표현한다. 기존 저장소 구조가 다르면 기존 구조를 우선하되 위 경계를 깨지 않는다.

## 6. API 규칙

- 사용자, 관리자 HTTP API는 `/api/v1/**` 아래에 둔다. 단, Kakao OAuth 시작/콜백은 `/oauth2/**` 예외 경로를 사용한다.
- 관리자 API는 일반 회원 토큰의 `members.role=ADMIN`과 `admin_users.admin_role`을 모두 확인한 뒤, `OPERATOR`, `REVIEWER`, `CONTENT_CREATOR`, `SUPER_ADMIN` 중 API 명세에 맞는 세부 권한을 요구한다.
- 시스템 API와 배치/AI 내부 작업은 사용자 계정이 아니라 `SYSTEM_BATCH` 주체로 기록한다.
- 인증되지 않은 사용자는 Kakao login 시작만 가능하다.
- 앱 콘텐츠 API는 인증된 역할 기준으로 보호한다.
- 내부 Java Interface는 OpenAPI에 노출하지 않는다.
- F-ID가 있는 기능은 API, 테스트, PR 설명에 관련 F-ID를 남긴다.

## 7. 금지 기능과 금지 기술

다음은 임시 구현도 금지한다.

- 개역개정, ESV, NIV seed/test/fixture/response 데이터
- 성서유니온 또는 두란노 본문 텍스트 저장
- plain secret, token, password, private key 예시

## 8. 코드 작성 규칙

- `jakarta.*`를 사용하고 `javax.*`를 사용하지 않는다.
- write use case와 command service에는 `@Transactional`을 사용한다.
- read path에는 적절히 `@Transactional(readOnly = true)`를 사용한다.
- event handler 실패 로그에는 `eventId`, event type, handler name, error message를 남긴다.
- 로그에는 password, token, private key, 민감 개인정보를 남기지 않는다.
- 외부 API 오류는 공통 예외/응답 규칙으로 감싼다.
- 관련 없는 리팩터링, 대규모 포맷팅, 파일 이동은 하지 않는다.

## 9. 테스트 기준

변경 범위에 맞춰 단위 테스트, slice/integration test, ArchUnit/Spring Modulith 경계 테스트를 추가한다. 특히 아래 영역은 테스트 누락을 허용하지 않는다.

- Bible source metadata와 금지 번역본 데이터 차단
- 도메인 간 금지 import

## 10. 검증 명령

서버 변경 후 관련 범위에 맞게 실행한다.

```bash
./gradlew -p qtai-server build
./gradlew -p qtai-server test jacocoTestReport
./gradlew -p qtai-server jacocoTestCoverageVerification
npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml
gitleaks detect --source . --redact --exit-code 1
```

실제 저장소 구조나 변경 범위상 일부 명령을 실행할 수 없으면, 최종 응답에 실행하지 못한 이유를 명확히 적는다.

## 11. Git과 PR 규칙

- `dev`에서 작업 브랜치를 만든다.
- `dev`와 `master`에 직접 push하지 않는다.
- 브랜치명은 `{type}/{scope}-{short-description}` 형식을 사용한다.
- 커밋은 Conventional Commits를 사용한다.
- PR 대상은 `dev`다.
- PR은 가능하면 10 files 이하, 500 changed lines 이하로 유지한다.
- 기능 PR은 관련 F-ID를 명시한다.
- `.gradle`, build output, coverage HTML, generated report, temporary file은 stage하지 않는다.
