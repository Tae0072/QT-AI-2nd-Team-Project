# 스터디 노트 — Gradle 멀티모듈 & MSA 기초 (2026-06-09)

> 모놀리식 `qtai-server`를 멀티모듈로 쪼개며 배운 개념 정리. 입문자 기준.

## 1. 모놀리식 vs 멀티모듈 vs 마이크로서비스

- **모놀리식**: 코드가 한 덩어리(한 빌드, 한 실행). 지금까지의 qtai-server.
- **멀티모듈**: 한 저장소·한 빌드 안에서 코드를 여러 모듈(폴더)로 나눈 것. 각 모듈이 별도 jar로 빌드될 수 있음.
- **마이크로서비스(MSA)**: 각 모듈을 **따로 실행·배포**하는 것. 한 서비스가 죽어도 다른 서비스는 살아있음(= 장애 격리).

→ 우리는 "멀티모듈로 나눠 각 모듈을 별도 서비스로 띄운다". 멀티모듈은 수단, MSA(장애 격리)는 목적.

## 2. Strangler(스트랭글러) 패턴

큰 모놀리식을 한 번에 갈아엎으면 위험하다. 그래서 **기존 모놀리식은 그대로 두고, 도메인을 하나씩 새 모듈로 떼어내고(추출), 다 옮기면 모놀리식 쪽을 제거**한다. 전환 중에는 코드가 양쪽에 잠시 중복돼도 괜찮다. "넝쿨(strangler fig)이 나무를 서서히 감싸 대체한다"는 비유에서 온 이름.

## 3. Gradle 멀티모듈 핵심

- **`settings.gradle.kts`의 `include(...)`**: 어떤 폴더가 하위 모듈인지 등록.
- **`pluginManagement`**: 플러그인(예: Spring Boot) 버전을 한 곳에서 정의 → 모든 모듈이 `id("...")`만 쓰고 버전은 생략(중복 제거). 그래서 root 빌드에서도 버전 표기를 지웠다.
- **`api` vs `implementation`** (의존성 노출 차이):
  - `implementation` = 이 모듈 안에서만 쓰는 의존성(밖으로 안 보임).
  - `api` = 이 모듈을 가져다 쓰는 다른 모듈에도 그대로 노출됨. lib-common은 web/security 등을 `api`로 둬서, lib-common을 의존하는 service-bible이 그것들을 자동으로 물려받게 했다.

## 4. JWT를 "발급"과 "검증"으로 나누기

RS256(비대칭 키)을 쓰면 **개인키로 발급**, **공개키로 검증**한다. MSA에선:
- 토큰 **발급**은 service-user(개인키 보유)만.
- 나머지 서비스는 **공개키로 검증만** 하면 됨(네트워크 호출 불필요).

그래서 lib-common에는 검증 전용 `JwtValidator`(공개키만)를 두고, 발급 로직(개인키)은 넣지 않았다. 보안상으로도 개인키를 한 서비스에만 두는 게 안전.

## 5. `@ConditionalOnProperty` — 설정 있을 때만 빈 등록

JWT 검증 빈은 `security.jwt.public-key`가 설정돼 있을 때만 활성화하도록 `@ConditionalOnProperty`를 붙였다. 덕분에 키가 없는 **스켈레톤/테스트 환경에서도 부팅이 깨지지 않는다**. "설정이 준비되면 켜진다"는 패턴.

## 6. 왜 JPA를 넣자 스켈레톤이 깨질 뻔했나

Spring Boot는 **클래스패스에 JPA가 있으면 자동으로 DataSource(DB 연결)를 구성하려 한다**. 그런데 스켈레톤엔 DB 설정이 없어서 "DataSource를 구성할 수 없다"며 부팅 실패. → 그래서 JPA/BaseEntity는 **DB 설정이 함께 들어가는 ③단계로 미뤘다**. "필요한 의존성은 그게 실제로 쓰일 때 함께 넣는다"는 교훈.

(반대로 `spring-tx`는 `GlobalExceptionHandler`가 `DataIntegrityViolationException`을 참조해서 컴파일에 꼭 필요 → 바로 추가.)

## 7. Windows 빌드 폴더 잠금 이슈

Cursor IDE의 Java 언어서버가 `build/classes` 폴더를 잠가서, Gradle이 그 폴더를 못 지우고 `Unable to delete directory` / `Failed to clean up stale outputs`로 실패하는 일이 반복됐다.
- **대응**: `gradlew --stop`(데몬 종료) 후 해당 모듈 `build/` 폴더를 삭제하고 재빌드.
- 교훈: 빌드 실패 메시지가 "컴파일 에러"인지 "파일 잠금/삭제 실패"인지 구분할 것. 후자는 코드 문제가 아니라 환경(잠금) 문제다.

## 8. 한 줄 요약

> 멀티모듈은 "한 저장소에서 모듈로 나누기", MSA는 "그 모듈을 따로 띄워 장애 격리". Strangler로 안전하게 옮기고, 공통 코드는 lib-common에 동일 패키지로 모아 import를 안 바꾸게 했다.
