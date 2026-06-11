# 2026-06-09 bible 컷오버(Inc5a) — 결과 보고

## 요약
모놀리식 bible 호출을 bible-service HTTP로 전환하는 **컷오버를 안전하게 실행·검증·롤백**하기 위한 런북을 작성하고, 컷오버 오설정 가드의 빈 케이스(base-url 누락 fail-fast) 테스트를 보강했다. 조사 결과 HTTP 경로·안전장치는 Inc3~Inc3d에서 이미 구현·검증 완료였고, 컷오버는 env 플래그 활성화(운영)임을 확인 — 본 Inc5a는 그 실행을 문서·테스트로 확정한다. Inc5b(제거)의 선행.

## 산출물
| 파일 | 설명 |
|------|------|
| `external/bible/BibleHttpClientConfigurationTest.java` | (보강) `httpMode_missingBaseUrl_failsFast` — mode=http + base-url 누락 시 부팅 fast-fail 보장 잠금 |
| `workflows/2026-06-09_msa-bible-cutover-runbook.md` | 컷오버 런북(env 플래그·활성화 순서·검증·롤백·Inc5b readiness) |
| `reports/..._report.md` | 본 보고 |

## 조사 결과(핵심)
- 모놀리식 bible client 기본값 `inprocess` — 운영/테스트 모두 여전히 in-process `BibleService`. 그대로 제거하면 live 경로 단절.
- bible-service persistence/inbound 기본 off — 전용 DB 아직 비활성.
- **소비자 단위 테스트 6개(ai/note/qt)는 전부 UseCase 인터페이스를 Mockito mock** → in-process 빈 미의존. 인터페이스(api) 유지 시 제거해도 단위 테스트 무영향(Inc5b 위험 대폭 감소).
- HTTP 어댑터 config 테스트가 `mode=http` 시 `@Primary` 어댑터 해석·inprocess 미등록·token 누락 fail-fast 검증 → 컷오버 wiring 보장. 빠진 base-url 케이스만 보강.

## 변경 성격
- **운영 전환 문서화 + 가드 보강**: 신규 프로덕션 코드 없음(능력은 기존). 컷오버 오설정 시 조용한 실패를 막는 테스트 1건 추가.
- 동작 무변경(기본 inprocess 유지) — 테스트 안정.

## 검증
- `gradlew :test --tests com.qtai.external.bible.BibleHttpClientConfigurationTest` — **4건 0 실패**(기존 3 + 신규 1).
- 2층 셀프 점검: CI 게이트(브랜치/시맨틱 제목/gitleaks/금지) + 리뷰 9기준(테스트 보강·fail-fast 부정경로·문서 추적) PASS.

## 미해결 / 후속
- **컷오버 실행은 ops/Lead**: 런북의 env 플래그를 dev→demo→prod 순차 활성화 + soak.
- **Inc5b**(soak 후): 모놀리식 bible internal/web/dead-UseCase 제거 + HTTP 어댑터 무조건 활성 + `V31` 테이블 DROP(ops DB 분리 전제) + ArchUnit 갱신.
- (선택) 소비자→@Primary 어댑터→http end-to-end 통합 테스트(MockRestServiceServer).
