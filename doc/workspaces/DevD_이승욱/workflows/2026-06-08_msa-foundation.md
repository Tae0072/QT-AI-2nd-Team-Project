# 2026-06-08 MSA Phase 0 — Gradle 멀티모듈 전환 + lib-common 추출

## 목표
모놀리식 `qtai-server`를 MSA로 점진 분리하기 위한 토대로, Gradle 멀티모듈 구조를 도입하고 서비스 공통 코드를 `lib-common` 모듈로 분리한다. (동작 무변경, 구조 전용)

## 배경
- 2026-06-08 팀 합의: AI 제외 **5개 사용자 서비스 + admin-service**로 분리, 이승욱이 전 파트 담당. Lead가 거버넌스 갱신(#341 CLAUDE.md·CI 금지 해제 — Kafka/K8s/Helm 허용).
- 설계 문서: `doc/workspaces/DevD_이승욱/MSA분리계획_2026-06-08.md` (Strangler 로드맵·Saga·보상 트랜잭션 포함).

## 작업 내용
1. `settings.gradle.kts`에 `:lib-common` 모듈 등록, 루트(app)가 `project(":lib-common")` 의존.
2. `com.qtai.common`(dto·entity·exception) 5개 클래스(ApiResponse·BaseEntity·ErrorCode·BusinessException·GlobalExceptionHandler)를 `lib-common`으로 이전 — **패키지명 유지로 import 102개 무수정**.
3. 최신 `dev` 머지(거버넌스 #341 포함, 충돌 없음).

## 범위
- 브랜치: `feature/msa-foundation` (base: `dev`)
- 변경: 코드 8파일(rename 5 + gradle 2 수정 + lib-common build 1) + 문서. 동작 무변경.
- 관련: MSA 로드맵 Phase 0 (공통 인프라)

## 검증
- `gradlew assemble` — **BUILD SUCCESSFUL** (app bootJar + lib-common jar). 최신 dev 머지 후에도 성공.

## 미해결 / 후속
- PR 머지 대기
- 후속 증분: JWT 검증 유틸 `lib-common` 이전 → `service-gateway` 스캐폴드 → ArchUnit 모듈 경계 규칙 → 1단계 bible 추출

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
