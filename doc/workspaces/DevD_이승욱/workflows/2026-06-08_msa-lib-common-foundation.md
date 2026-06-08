# 2026-06-08 MSA Phase 0 후속 — lib-common 경계·테스트·JWT 검증 유틸

## 목표
Phase 0(#343 lib-common 추출)의 후속으로 ① 공유 모듈 `lib-common`의 경계를 ArchUnit으로 기계적으로 강제하고 ② 단위 테스트를 보강하며 ③ 서비스/게이트웨이가 재사용할 JWT 검증 유틸을 추가한다.

## 배경
- #343(Gradle 멀티모듈 + lib-common 추출)이 dev에 **squash 머지**됨. 본 작업은 그 후속(리뷰 권장사항 반영 + 인증 토대). squash로 기존 브랜치 충돌 → 최신 dev 기준 새 브랜치(`test/msa-lib-common-boundary`)로 재구성.

## 작업 내용
1. **LibCommonBoundaryArchTest** — `com.qtai.common`이 `domain/config/security/external/batch`에 의존하지 못하도록 ArchUnit 강제(공유 lib = leaf). 기존 ArchUnit 파일 미수정(신규 파일).
2. **lib-common 단위 테스트** — ApiResponse/ErrorCode/BusinessException + test 의존(`spring-boot-starter-test`, `junit-platform-launcher`, `useJUnitPlatform`).
3. **JwtTokenVerifier**(공개키 RS256 검증) + **AuthenticatedUser**(memberId, role) — 발급자 `com.qtai.security.JwtProvider` **미변경(순수 추가)**. 토큰 구조 동일(sub=memberId, role, type). jjwt 0.13.0 의존. 단위 테스트 5건(유효/만료/refresh거부/서명불일치/role누락).

## 범위
- 브랜치: `test/msa-lib-common-boundary` (base: `dev`)
- 변경: lib-common build + 신규 클래스 2(security) + 테스트 4파일. 기존 코드/인증 무영향.
- 관련: MSA 로드맵 Phase 0 후속

## 검증
- `gradlew :lib-common:test` — **통과** (단위 + JWT 5건)
- `gradlew :test --tests LibCommonBoundaryArchTest` — **통과** (jjwt 추가 후에도 leaf 유지)
- 전체 `./gradlew test`(Docker/Redis 필요)는 CI

## 미해결 / 후속
- PR 머지 대기
- 후속: `service-gateway` 스캐폴드 · bible 추출(1단계) · 각 서비스/게이트웨이에서 `JwtTokenVerifier` 채택

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
