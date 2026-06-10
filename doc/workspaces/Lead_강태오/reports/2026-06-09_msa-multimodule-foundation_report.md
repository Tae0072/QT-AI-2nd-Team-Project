# 작업 리포트 — MSA 멀티모듈 기반 구축 (① 골격 + ② lib-common)

- **일자**: 2026-06-09
- **브랜치**: `feat/msa-multimodule-skeleton` (dev-msa 기반)
- **작업자**: 강태오(Lead, AI 보조)
- **관련 문서**: `2026-06-09_msa-restart-plan.md`, `2026-06-09_msa-multimodule-foundation.md`(워크플로우)

## 1. 배경

2026-06-09 회의에서 MSA 방향을 단일 DB·RestClient·Kafka 최소화로 재확정했다. 이전 MSA Phase 1(DB-per-service·Circuit Breaker 등)은 방향이 반대라 `dev-msa`를 `origin/dev`(#340)로 리셋해 폐기하고(백업 태그 `archive/dev-msa-before-reset-20260609` 보존), 깨끗한 모놀리식에서 멀티모듈화를 재시작했다.

## 2. 수행 내용

### ① 멀티모듈 골격 (`62e791b`)
- `settings.gradle.kts`에 `pluginManagement`를 도입해 Spring Boot/dependency-management 플러그인 버전을 한 곳에서 관리하고, root와 모든 모듈이 버전 없이 공유하도록 변경.
- root `build.gradle.kts`(모놀리식)는 유지(Strangler). `lib-common`(java-library)·`service-bible`(boot app, web only) 스켈레톤 모듈을 추가.
- 검증: `:lib-common:build`·`:service-bible:build`·root `compileJava` 모두 통과.

### ② lib-common (`00fb9f5`)
- 공통 응답·예외(`ApiResponse`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`)를 모놀리식과 **동일 패키지 `com.qtai.common.*`**로 lib-common에 배치 → ③에서 도메인 코드를 옮길 때 import 무수정.
- MSA용 **검증 전용** JWT 구성: `JwtValidator`(공개키만), `JwtAuthenticationFilter`, `SecurityErrorResponseWriter`. 토큰 발급(개인키)은 service-user 전용으로 분리. `security.jwt.public-key` 설정 시에만 활성(`@ConditionalOnProperty`)해 스켈레톤/테스트는 키 없이 부팅.
- 서비스 간 동기 호출용 `RestClientConfig` 추가.
- 검증: `:lib-common:build`(ApiResponse 단위 테스트 포함) + `:service-bible:build` 통과.

## 3. 검증 결과

| 명령 | 결과 |
|------|------|
| `:lib-common:build :service-bible:build` (①) | BUILD SUCCESSFUL (14s) |
| `gradlew projects` | root + lib-common + service-bible 정상 인식 |
| root `compileJava` | BUILD SUCCESSFUL (6s) — 모놀리식 무손상 |
| `:lib-common:build :service-bible:build` (②) | BUILD SUCCESSFUL (14s) |

3회 교차 검증(모듈 빌드 / 프로젝트 인식 / 모놀리식 컴파일)으로 회귀 없음 확인.

## 4. 이슈 및 대응

- **빌드 폴더 잠금**: Windows에서 Cursor IDE Java 언어서버가 `build/classes` 디렉터리를 잠가 `Unable to delete directory` / `Failed to clean up stale outputs` 오류가 2회 발생. → `gradlew --stop` 후 해당 모듈 `build/` 폴더를 삭제하고 재빌드해 해결.
- **`org.springframework.dao` 누락**: JPA 의존을 ②에서 제외하자 `GlobalExceptionHandler`의 `DataIntegrityViolationException`이 컴파일 실패. → `spring-tx`를 lib-common에 추가해 해결.
- **`.git/worktrees/*` 삭제 권한 경고**: 이전 작업의 잔여 worktree 메타데이터로, 커밋에는 영향 없음. 후속 정리 대상.

## 5. 다음 단계

③ service-bible 도메인 추출. `bible`(cross-domain 의존 없음)부터 파일럿으로 옮겨 BaseEntity/JPA/단일 DB 패턴을 확립한 뒤 `music` → `qt`·`study`·`praise` 순. 후자 3개는 `client` 패키지의 타 도메인 호출을 RestClient 어댑터/Mock으로 대체해야 한다.

## 6. 미해결/주의

- 작업 브랜치는 **로컬에만** 존재(아직 push 안 함). PR 전 dev-msa와 동기화 권장.
- 모놀리식 전체 테스트(Testcontainers MySQL)는 미실행 — 모듈 단위 검증으로 대체. PR 단계에서 CI 필수 체크(`qtai-server Build & Test`)로 보강 필요.
