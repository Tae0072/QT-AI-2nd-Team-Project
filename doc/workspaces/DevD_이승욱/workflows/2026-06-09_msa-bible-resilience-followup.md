# 2026-06-09 MSA Phase 1 — bible HTTP 재시도 보강 회수 + CB 의존성 결정 (Inc3d 준비)

## 목표
#403(재시도) 머지 후 stranded된 리뷰 WARN/INFO 보강을 dev로 회수하고, Circuit Breaker 도입의 **의존성 결정**을 명문화한다.

## 배경
- #403 자동 머지 직후 push한 WARN/INFO 보강(`67b27f5`)이 머지된 브랜치에 남아 dev 미반영(stranded). 자동 squash 머지 특성상 반복된 문제 → dev에서 새 브랜치로 cherry-pick해 회수.

## 작업 내용
1. **보강 회수**(cherry-pick `67b27f5`): ① 재시도 `maxAttempts` 상한 가드(`MAX_ATTEMPTS_CAP=10`) ② 4xx 무재시도 테스트(정확히 1회 호출 단언) ③ 재시도 로그 호출 식별자(`withRetry(operation, ...)`).
2. **CB 의존성 결정 명문화**(운영 진입 체크리스트): 모놀리식엔 resilience4j도 spring-cloud BOM도 없음 → CB 도입은 `spring-cloud-starter-circuitbreaker-resilience4j` 추가 또는 resilience4j 버전 핀(게이트웨이 버전 정합)의 **새 의존성/버전 결정**. 비차단(재시도+타임아웃으로 일시 장애 커버)이라 **Lead/인프라 승인 후 도입**. 실패 기록은 EXTERNAL_API_FAILURE만(4xx 제외).

## 범위
- 브랜치: `feature/msa-bible-http-client-cb` (base: `dev`)
- 변경: 보강 3종 cherry-pick(클라이언트·테스트·report) + 체크리스트(CB 결정). 코드 동작 무변경(기본 inprocess).

## 검증
- `gradlew :compileJava :test --tests "com.qtai.external.bible.*"` — **0 failures (14건)**: 클라이언트 8(+4xx 무재시도) + Configuration 3 + 어댑터 3.
- 2층 셀프 점검: CI 게이트(브랜치/커밋/gitleaks/금지) + 리뷰 9기준(#2 예외 불변·#3 4xx 무재시도 커버) PASS.

## 미해결 / 후속
- **CB 도입**: Lead/인프라 의존성 결정 후(체크리스트 추적).
- **Inc3d 컷오버**(오너 협의): `mode=http` 전환 + 양측 토큰 동기화 + 소비자(qt/note/study) 계약 테스트.
- Inc4(DB 분리) → Inc5(모놀리식 bible 제거).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
