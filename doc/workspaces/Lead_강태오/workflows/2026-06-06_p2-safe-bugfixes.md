# 2026-06-06 P2 — 안전 버그 묶음(1차) 워크플로우

리뷰 §3 안전 버그 1차 처리 머지 가이드. 결과: `reports/2026-06-06_p2-safe-bugfixes_report.md`

## 0. 전제
- 구현·커밋 완료. **push·PR은 사용자(T)가 직접.** 모든 브랜치 `origin/dev` 분기, PR 대상 `dev`.

## 1. 브랜치 → 내용(커밋)

| 브랜치 | 커밋 | 도메인 | 파일 |
|--------|------|--------|------|
| `fix/member-reactivate-email-null-guard` | 66b7ccd | member | Member.java + MemberReactivateTest(신규) |
| `fix/cache-wiring` | 8c960e7 | qt + bible | QtPassageLookup(키), BibleService(@Cacheable) |
| `chore/config-cleanup` | d6d37d0 | 공통 | JwtProvider(javadoc), RedisConfig(삭제), build.gradle.kts(batch 제거) |

## 2. 충돌/의존
- 세 브랜치 파일 교집합 없음 → 머지 순서 자유.
- `fix/cache-wiring`의 bibleBooks 캐시는 `CacheConfig @EnableCaching`(기존)에 의존 — 추가 설정 불필요.
- `chore/config-cleanup`의 RedisConfig 삭제 후 `StringRedisTemplate`은 Spring Boot 자동구성으로 제공(RefreshTokenStore 사용처 영향 없음).

## 3. 권장 머지 순서
1. `fix/member-reactivate-email-null-guard`
2. `fix/cache-wiring`
3. `chore/config-cleanup`

(순서 자유) 각 PR 본문에 리뷰 리포트 §3 참조 + 워크플로우 경로 명시.

## 4. 검증
```bash
$env:JAVA_HOME='D:\workspace\tools\jdk\jdk-21.0.11+10'
cd D:\workspace\QT-AI-2nd-Team-Project\qtai-server
Remove-Item build\test-results -Recurse -Force -ErrorAction SilentlyContinue
.\gradlew.bat --no-daemon test
```
- 각 브랜치 0 failures(예: config-cleanup 824 tests / 0 fail / 4 skip).

## 5. 주의
- 보류 항목(study aiAssetId, STALE 캐싱 정책, JacksonConfig, ai 검증 흐름)은 리포트의 "보류" 표 참고 — 매뉴얼 테스트 재구성/정책 결정 동반.
- 다음 1순위: study 사용자 응답 `aiAssetId` 비노출(대형 매뉴얼 테스트 재구성 동반).
