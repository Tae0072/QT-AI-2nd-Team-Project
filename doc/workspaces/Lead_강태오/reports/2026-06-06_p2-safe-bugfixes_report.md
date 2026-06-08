# 2026-06-06 P2 — 안전 버그 묶음(1차) 결과 보고

리뷰 §3(Medium/Low) 중 **결정 불필요한 안전 버그**를 골라 처리한 1차. **3개 브랜치/3개 커밋**, 각 브랜치 전체 스위트 **0 failures**.
워크플로우: `workflows/2026-06-06_p2-safe-bugfixes.md`

> 작업 방식: 항목별 독립 브랜치(`origin/dev` 분기)로 구현·커밋. **push·PR은 사용자(T)가 직접.** 리뷰가 하루 전이라 P0/P1 변경 반영해 현재 코드로 재검증 후 수정.

## 브랜치별 결과

| 브랜치 | 커밋 | 내용 |
|--------|------|------|
| `fix/member-reactivate-email-null-guard` | 66b7ccd | 탈퇴 회원 재활성화 시 카카오가 email/profile을 null로 줘도 기존 보관 값을 덮어써 과거 값이 소실되던 버그. null이면 기존 값 유지하도록 가드 + 단위 테스트 신설 |
| `fix/cache-wiring` | 8c960e7 | (qt) todayQt `@Cacheable` 키 SpEL이 시스템 시계를 직접 호출해 주입 Clock과 어긋날 수 있던 것을 `@clock` 빈 기반으로 통일(KST 빈이라 운영 동작 동일·테스트 일관). (bible) 등록만 되고 미부착이던 `bibleBooks` 캐시를 `listBibleBooks`에 `@Cacheable`로 실제 사용 |
| `chore/config-cleanup` | d6d37d0 | JwtProvider javadoc "Refresh 30일"→실제 14일 정정, 미사용 `RedisConfig`(어디서도 안 쓰는 `RedisTemplate<String,Object>` 빈) 제거, 미사용 `spring-boot-starter-batch` 의존성 제거 |

## 재검증으로 "버그 아님" 확인(변경 없음)

리뷰 §3가 member 결함으로 적었으나 현재 코드 확인 결과 이미 정상:

- **SUSPENDED에 MEMBER_ALREADY_WITHDRAWN 오반환**: `AuthService`의 로그인·refresh 두 경로 모두 WITHDRAWN→`MEMBER_ALREADY_WITHDRAWN`, SUSPENDED→`MEMBER_SUSPENDED`로 올바르게 분기. (P0/P1 작업 중 정리된 것으로 보임)
- **7일 닉네임 잠금 off-by-one**: `nicknameChangedAt.plusDays(7).isBefore(now)` + 잠금해제 시각 노출이 의미상 정확. 경계 버그 아님.

## 검증

- 각 브랜치에서 전체 스위트 실행 0 failures. 예) `chore/config-cleanup` 824 tests / 0 fail / 4 skip(Docker 필요분 skip).
- `fix/member-*`는 재활성화 null 유지/정상 갱신 단위 테스트 추가.
- `fix/cache-wiring`은 `@Cacheable` 프록시 경유 캐시 통합 테스트(QtServiceCacheTest)가 주입 Clock 기반 키로 정상 통과.
- `chore/config-cleanup`은 배치 스타터·RedisConfig 제거 후 전체 컨텍스트 로드 통과(StringRedisTemplate는 Boot 자동구성).

## 의도적으로 보류(다음 처리)

| 항목 | 이유 |
|------|------|
| **study 사용자 응답 `aiAssetId`(내부 PK) 노출** | 제거가 맞지만 `AiBibleVerseAssetCustomerExposureFlowManualTest`(대형)이 노출 추적 신호로 `ExplanationItem.aiAssetId`를 읽음 → 매뉴얼 테스트 재구성을 동반해야 안전. **다음 1순위로 단독 처리** |
| qt 00:00~04:00 STALE_FALLBACK 미캐싱 | `unless=HIT만`은 04:00 이후 stale 노출 방지용 신선도 우선 설계. 캐싱하면 TTL 동안 stale 노출 위험 → 정책(허용 stale 창) 결정 필요 |
| JacksonConfig ObjectMapper(@Primary 부재, spring.jackson.* 무효) | 교체 시 전 JSON 직렬화 거동 영향 → 응답 스냅샷 검증을 동반해야 안전 |
| ai NEEDS_REVIEW 영구 차단·PASSED 로그 위조·activateForTarget=false 게시 상실 | 동작/정책 영향이 큰 건 → MSA 구조 배치 또는 별도 정밀 작업 |

## 후속 후보

- study `aiAssetId` 비노출(다음 1순위), member 기타(재활성 email 외 잠금·purge 인덱스), study FAILED 의미·scene_script 검증, ai 검증 흐름(정밀), audit 조회 화이트리스트·PII 가드, 문서 drift(ERD/OpenAPI).

## 머지 메모

- 세 브랜치 파일 교집합 없음 → 순서 자유. push·PR은 T가 직접. 전 브랜치 머지 후 `dev` 전체 테스트 1회 권장.
