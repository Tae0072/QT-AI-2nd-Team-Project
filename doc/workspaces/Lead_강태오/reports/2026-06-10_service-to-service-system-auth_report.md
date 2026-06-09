# 작업 리포트 — 서비스 간 시스템 인증(공유 HS256 단명 SYSTEM_BATCH 토큰)

- **일자**: 2026-06-10
- **작업 폴더(worktree)**: `D:\workspace\QT-AI-sysauth` (브랜치 feature/msa-system-auth-token, dev-msa 기준)
- **작업자**: 강태오(Lead, AI 보조)
- **근거**: 결정 문서 `2026-06-10_service-to-service-system-auth.md`, 회의록 2026-06-09 §3·§4

## 1. 배경
MSA 전환 후 배치·스케줄러(SYSTEM_BATCH) 호출(예: ai 해설 워커→bible 본문 조회)은 전달할 사용자 JWT가 없다. RestClient 통합(ai→bible 등)의 **선행 조건**으로 서비스 간 시스템 인증 메커니즘이 필요하다.

## 2. 수행 내용 (lib-common)
- **SSoT 먼저 갱신**: CLAUDE.md §5에 "서비스 간 시스템 인증 = 공유 HS256 단명 SYSTEM_BATCH 토큰(사용자 RS256과 분리)" 명문화 + 결정 문서 작성. (가드/리뷰가 SSoT 미갱신을 막으므로 코드보다 먼저)
- **`SystemTokenProvider`**(HS256 발급): `security.jwt.system-secret` + `system-token-expiry-ms`(기본 60초). sub=0, role=SYSTEM_BATCH, type=system. `@ConditionalOnProperty(system-secret)`.
- **`SystemTokenValidator`**(HS256 검증): 서명·type=system·role=SYSTEM_BATCH 확인 후 시스템 memberId(0) 반환. 실패 시 `JwtException`.
- **`SystemTokenClaims`**: 발급·검증 공통 claim 상수.
- **`JwtAuthenticationFilter` 폴백**: RS256 사용자 검증 실패 시 `SystemTokenValidator`(있으면)로 폴백 → 성공 시 memberId=0 + ROLE_SYSTEM_BATCH. **둘 다 실패해야 401.** SystemTokenValidator는 `ObjectProvider`로 선택 주입(미설정 환경 null → 폴백 비활성, RS256 동작 그대로).
- **시크릿은 env로만**(relaxed binding: `SECURITY_JWT_SYSTEM_SECRET`), 토큰·시크릿 로그/커밋 금지. HS256은 32바이트 이상.

## 3. 검증 결과
- `:lib-common:build` 통과 — 신규 테스트:
  - `SystemTokenTest`: 발급→검증 라운드트립(memberId 0) / 다른 시크릿 위조 거부 / type≠system 거부 / role≠SYSTEM_BATCH 거부 / 만료 거부.
  - `JwtAuthenticationFilterTest`(확장): 토큰없음 통과 / 사용자토큰 정상 / 시스템검증기 없이 실패 401(기존 동작) / 사용자정상이면 폴백 안함 / **사용자 실패→시스템 폴백 성공(SYSTEM_BATCH)** / 사용자·시스템 모두 실패 401.
- **전 모듈 회귀 빌드 통과(1m36s)**: lib-common + service-user/bible/note/ai/admin-server. 기존 JWT/보안 테스트 무손상(필터 변경이 하위호환 — system-secret 미설정 시 동작 동일).

## 4. 설계 판단
- **RS256(사용자) vs HS256(시스템) 분리**: 사용자 토큰은 발급처 단일(service-user)이라 RS256, 시스템 토큰은 여러 서비스가 서로 발급·검증해야 해 공유 시크릿 HS256이 단순. 단명(60초)으로 탈취 위험↓. 시스템 시크릿이 새도 사용자 토큰 위조로 이어지지 않게 분리.
- **폴백 순서**: 사용자 먼저 → 시스템. 사용자 토큰이 정상이면 시스템 검증 안 함(불필요 연산·오탐 방지).
- **선택 주입(ObjectProvider)**: system-secret 미설정 환경에서도 부팅·기존 동작 유지. 광범위 catch 없이 `JwtException|IllegalArgumentException`만 좁게 처리.
- **서비스 config 미변경**: bible/ai/note 등은 jwt를 env(relaxed binding)로 주입하므로 application.yml을 건드리지 않음 → 동시 진행 중인 RestClient 세션과 충돌 최소화.

## 5. 다음 단계 (후속 PR)
- 첫 소비자: ai 배치→bible RestClient 어댑터에서 `SystemTokenProvider.issueSystemToken()`으로 토큰을 발급해 `Authorization` 헤더에 실어 호출. 서비스별 `SECURITY_JWT_SYSTEM_SECRET` env 주입(배포 설정).
- (선택) 시스템 토큰 발급 로깅·감사 연계, 시크릿 회전 정책.
