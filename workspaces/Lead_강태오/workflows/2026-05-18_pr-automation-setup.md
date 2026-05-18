# Workflow — 2026-05-18 pr-automation-setup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강태오 (@Tae0072) |
| 브랜치 | master (직접 push) — 이번 작업 한정 예외 |
| PR 대상 | 없음 (master 직접 push) |
| 관련 F-ID | 없음 — 인프라/CI 정합화 |
| 기준 문서 | 07_요구사항_정의서.md v3.1, 09_Git_규칙.md v3.1, 18_코드_품질_게이트.md v2.4, 23_도메인_용어사전.md v1.0 |
| 담당 경로 | .github/CODEOWNERS, .github/workflows/claude-pr-review.yml, .github/workflows/ci.yml, .github/pull_request_template.md |

## 작업 목표

작업 저장소 `.github/` 폴더의 CI · 자동 PR 리뷰 · 자동 머지 · CODEOWNERS · PR 템플릿 규칙을 v3.1로 정합화한다. 본 작업은 PR 자동화 자체의 기반을 만드는 작업이라 Lead 결정에 따라 W0 초기 셋업 한정으로 master 직접 push 예외를 적용한다. **이번 한 번뿐, 이후 모든 작업은 `{type}/{scope}-{short-task}` 브랜치 → dev PR 절차를 따른다.**

## 배경 — 정합화가 필요한 이유

기존 4개 파일이 v2.x MSA · Kafka · SSE 시대 잔재를 그대로 보유하여 v3.1과 충돌:

| 파일 | v3.1 위반 |
| --- | --- |
| `.github/CODEOWNERS` | `services/{gateway, bff-aggregator, bible-service, ai-service}/` 경로 — v3.1 폐기된 MSA 4모듈. 담당자 매핑도 잘못됨 (bible-service에 이승욱, ai-service에 김태혁 포함). |
| `.github/pull_request_template.md` | Kafka envelope/SSE `/turns` 경로 검사 (v3.1은 둘 다 금지). 13 도메인 경계 검사 누락. 핵심 7정책 누락. workflow/report 링크 누락. |
| `.github/workflows/ci.yml` | `services/*` matrix 4모듈 빌드 (v3.1은 단일 `qtai-server`). decisions-guard 금지 패턴 10개 누락. gitleaks/Spectral/docker compose config/flutter analyze 누락. |
| `.github/workflows/claude-pr-review.yml` | 리뷰 기준 "MSA 주의사항: Kafka 이벤트 페어", "AI 코칭 턴 처리" (v3.1은 Modular Monolith + AI 단발성). diff truncation 8KB (대형 PR 손실). 모델 sonnet-4-6. 테스트 커버리지 누락. |

## 작업 순서

1. ✅ 작업 저장소 `.github/` 폴더 정독 (4파일)
2. ✅ v3.1 거버닝 SSoT 기준으로 위반 사항 식별 (CRITICAL 5건 / HIGH 3건 / MINOR 3건)
3. ✅ 각 파일별 정합 미리보기 작성
4. ✅ Lead 결정 수렴:
   - CODEOWNERS 매핑 방식 (A) 13 도메인 패키지 단위
   - 바이블 서버 협업 그룹 = 김지민·이승욱·이지윤 (bible/report/notification/mission/flutter-app/apis 공동)
   - study = 김태혁(시뮬레이터) + 강상민(해설 협업) 공동
   - 강태오 = 모든 도메인에 추가 owner (전체 도움 권한)
   - 자동 머지 정책 = 옵션 (A) 유지
   - 모델 = `claude-opus-4-7`
   - diff truncation = 1MB (env 변수로 변경 가능)
   - 누락 게이트 4개 = PR-B로 분리 진행
5. ✅ 메모리 갱신:
   - 메모리 3: 로컬 경로 C:\와 E:\ 둘 다 명시
   - 메모리 25: PR 자동 게이트 8번을 "Claude APPROVE + CI 1~7 success → 자동 squash merge → dev. CODEOWNERS는 리뷰 라우팅용(자동 머지 우회)."로 갱신
6. 🚧 진행 중: `.github/CODEOWNERS` 갱신 (master 직접 push)
7. 🚧 진행 중: `.github/workflows/claude-pr-review.yml` 갱신 (master 직접 push)
8. ⏳ 다음 단계: `.github/workflows/ci.yml` v3.1 정합 미리보기 후 master push
9. ⏳ 다음 단계: `.github/pull_request_template.md` v3.1 정합 미리보기 후 master push
10. 🚧 진행 중: `workspaces/Lead_강태오/` 에 workflow + report 정리
11. ⏳ 다음 PR-B: 누락 게이트 4개 추가 (gitleaks, Spectral lint, docker compose config, flutter analyze) — 정상 PR 절차 (새 브랜치 → dev)

## 검증 계획

- master push 후 GitHub Actions `ci.yml` 자동 트리거 확인 (push: branches: [master])
- `claude-pr-review.yml`은 `pull_request` 트리거라 이번 master push에는 작동 안 함 — 의도됨
- CODEOWNERS는 다음 PR부터 자동 review 라우팅
- 다음 PR(예: `feature/note-*` 등) 시 새 자동 리뷰 정책이 실제로 작동하는지 검증

## 예상 리스크

1. **master 직접 push가 v3.1 정책과 충돌** — 메모리 23 "dev/master 직접 push 금지". 이번 한 번 예외, 이후 모든 작업은 dev로 PR 보내야 함. 팀원들에게 W0 한정 예외라는 점 명시 필요.
2. **`ANTHROPIC_MODEL=claude-opus-4-7` 비용 ↑** — sonnet 대비 단가 높음. 6주 프로젝트에서는 무시 가능하나 사용량 모니터링 필요.
3. **`DIFF_MAX_BYTES=1MB`가 Claude 컨텍스트 한도(약 200K 토큰 ≈ 600KB~1MB)에 근접** — 매우 큰 PR에서 컨텍스트 잘릴 수 있음. 이미지 시뮬레이션 도입 시(300MB 영역) 별도 워크플로우 분리 필요.
4. **자동 머지가 CODEOWNERS bypass** — 정책 인지 못한 팀원이 PR 머지 대기 중 혼란 가능. PR 템플릿(다음 작업)에 자동 머지 안내 명시 필요.
5. **`ci.yml`의 `services/*` matrix 폐지 시점에 `services/` 디렉터리에 코드가 남아 있으면 빌드 실패 가능** — 사전 검사 필요.
6. **다음 PR-B(누락 게이트 4개) 추가 시 임시로 CI 실패 가능** — 새 게이트가 실제로 통과되는 시점까지 머지 보류 정책 필요할 수 있음.
