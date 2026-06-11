# Lead 강태오 — 개인 워크스페이스

> 본 폴더는 강태오(Lead)의 작업 기록 공간이다. 다른 팀원과 AI 에이전트의 읽기·쓰기·삭제는 모두 금지된다 (`workspaces/README.md` §1~5).

---

## 1. 담당 역할

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강태오 (@Tae0072) |
| 주 역할 | Lead / member / qt / admin / DevOps / 전체 조율 |
| 구현 기준 | 단일 `qtai-server` Modular Monolith (`07_요구사항_정의서.md` v3.1) |
| 기준 문서 | `07_요구사항_정의서.md` v3.1, `00_개발_일정_총괄표.md` v0.1, `Lead_강태오_공식일정표.md` v3.1-align.1, `00_공통_브랜치_PR_워크플로우_규칙.md` v0.1 |

---

## 2. 담당 도메인 (단일 `qtai-server` 안 13개 도메인 중 3개)

| 도메인 | 책임 | 외부 공개 API prefix |
| --- | --- | --- |
| `domain.member` | Kakao OAuth, JWT 발급, 회원, 프로필, 튜토리얼, 탈퇴, 관리자 회원 제재 | `/oauth2/*`, `/api/v1/auth/**`, `/api/v1/me/**`, `/api/v1/members/**`, `/api/v1/admin/members/*` |
| `domain.qt` | QT 범위, Today QT 응답 조립, 관리자 QT 등록 | `/api/v1/qt/**`, `/api/v1/admin/qt-passages/**` |
| `domain.admin` | 관리자 대시보드, 공통 orchestration. 도메인별 admin API 단독 소유는 X | `/api/v1/admin/dashboard` |

> 다른 도메인의 admin API(예: `/api/v1/admin/qt-passages/**`)는 해당 도메인 소유자가 구현하되, 대시보드 통합 화면의 조립 책임만 `domain.admin`이 갖는다.

---

## 3. 대표 브랜치 scope (가이드 §1, §2)

| Scope | 예시 |
| --- | --- |
| `member` | `feature/member-kakao-oauth`, `feature/member-rate-limit` |
| `qt` | `feature/qt-today-response`, `feature/qt-cache-status` |
| `infra` | `chore/infra-quality-gates`, `chore/infra-workspaces-bootstrap` |
| `admin` | `feature/admin-dashboard`, `feature/admin-member-suspend` |

- 브랜치 명명 형식: `{type}/{scope}-{short-task}`
- 허용 type: `feature`, `bugfix`, `refactor`, `test`, `docs`, `chore`, `hotfix`
- PR 대상: 항상 `dev`
- 브랜치 수명: 최대 3일 권장. 길어지면 작은 PR로 분리

---

## 4. 폴더 구조와 파일명 규칙

```
workspaces/Lead_강태오/
├── README.md                                       (이 파일)
├── workflows/                                      (작업 시작 전 반드시 작성)
│   └── YYYY-MM-DD_{kebab-case-task}.md
└── reports/                                        (작업 완료 후 반드시 작성)
    └── YYYY-MM-DD_{kebab-case-task}_report.md
```

- 1 workflow = 1 report (동일 task-name + `_report` 접미사)
- 파일명 구분자는 `_` (가이드 §6 예시 기준)
- 모든 PR 본문에 workflow와 report 경로를 명시 (가이드 §3)

---

## 5. 흔들리지 않는 본인 기준 (`Lead_강태오_공식일정표.md` §2)

| 구분 | 고정 기준 |
| --- | --- |
| 백엔드 구조 | 단일 `qtai-server` 안에 `domain.member`, `domain.bible`, `domain.qt`, `domain.study`, `domain.note`, `domain.sharing`, `domain.praise`, `domain.report`, `domain.notification`, `domain.mission`, `domain.ai`, `domain.admin`, `domain.audit` 13개 도메인 패키지를 둔다 |
| 배포 | v1은 Docker Compose. Kubernetes·Helm은 MVP 목표에 넣지 않는다 |
| 이벤트 | v1은 Spring `ApplicationEventPublisher`. Kafka는 v2 이후 검토 |
| AI | 자유 챗봇, 다중 턴 대화, SSE, `/ai/sessions/**` 사용자 경로 생성 금지. F-15 사실 기반 Q&A는 단발 · 검증 흐름 |
| Today QT | 공개 시각 **00:00 KST**, 수집 배치 **04:00 KST** |
| Today QT 100%의 의미 | 본문 + 해설 진입점 + 묵상 진입점 + 시뮬레이터 상태값 정상 응답. 모든 시뮬레이터 클립이 존재한다는 뜻이 아니다 |
| 시뮬레이터 상태값 | `READY`만 보기 버튼 활성. `MISSING` / `FAILED` / `DISABLED`는 비활성 |
| 성경 데이터 | Repo URL · 라이선스 · 번역본명 · 출처 표기 · 재배포 가능 여부 확인된 GitHub 공개 JSON만 적재. 개역개정 / ESV / NIV는 금지 |
| 검색 / AI 저장소 | RDB 인덱스만. RAG / ChromaDB / vector DB / Elasticsearch 금지 |
| 찬양 | 운영자 사전 큐레이션. AI 추천 / 가사·음원 저장 / 직접 YouTube URL 입력 금지 |
| 교회 인증 | MVP 기본 제외. 화면 · 버튼 · API · DB 필드 0건 |

---

## 6. 매일 작업 순서 (`Lead_강태오_공식일정표.md` §5 / 실행가이드 §2)

1. `dev` 최신 상태 확인 (`git fetch origin --prune && git checkout dev && git pull origin dev`)
2. 오늘 작업이 `00_개발_일정_총괄표.md` W1~W5 게이트 안인지 확인
3. 작업 브랜치 생성 (`git checkout -b {type}/{scope}-{short-task}`)
4. `workflows/YYYY-MM-DD_{task}.md` 작성 (작업 전)
5. 구현 또는 문서 수정
6. 로컬 검증 (실행가이드 §7) — `git diff --check` + 금지 패턴 grep + 단위/통합 테스트
7. `reports/YYYY-MM-DD_{task}_report.md` 작성 (검증 결과 포함)
8. `git add` → conventional commit → `git push -u origin {branch}`
9. `gh pr create --base dev --head {branch}` (본문에 workflow / report 경로)
10. GitHub Actions · Requirements Guard · Claude PR Auto Review 결과 확인 → 머지 또는 후속 대응

---

## 7. 주차별 본인 집중 (`Lead_강태오_공식일정표.md` §3, §4)

| 주차 | 기간 | Lead 집중 범위 | 완료 기준 |
| --- | --- | --- | --- |
| W1 | 2026-05-18 ~ 2026-05-22 | Foundation Lock-in (저장소 운영, 백엔드 골격, 도메인 경계, DB/API 계약, 품질 게이트) | Foundation 5/5 통과 |
| W2 | 2026-05-25 ~ 2026-05-29 | Today QT 집계, 인증·권한, 외부 API 응답 조립, PR 검증 | 핵심 API 데모 가능 |
| W3 | 2026-06-01 ~ 2026-06-05 | 관리자·통합 조정, Feature Freeze, 경계 위반 차단 | 주요 MVP 기능 통과 |
| W4 | 2026-06-08 ~ 2026-06-12 | E2E, 성능, 품질 게이트, Docker Compose 시연 후보 빌드 | 시연 후보 빌드 확보 |
| W5 | 2026-06-15 ~ 2026-06-17 | 리허설, 백업 자료, 발표 직전 장애 대응 | 발표 흐름 2회 이상 성공 |

---

## 8. 우선 액션 (실행가이드 §0)

1. 브랜치 / PR / 워크플로우 기준을 구현 저장소 첫 PR에 반영
2. Today QT 응답에 `cacheStatus`와 `simulatorStatus`가 빠지지 않는지 확인
3. AI 자유 챗봇 · SSE · Kafka · K8s · Helm · RAG 금지 기준을 CI/리뷰 체크로 고정
4. 팀원 PR에서 API · 화면 · ERD 충돌을 우선 확인

---

## 9. 저장소 분리 원칙

- 이 폴더(`workspaces/Lead_강태오/`)는 **구현 저장소**(`QT-AI-2nd-Team-Project`)에만 존재한다
- 본인 가이드(HTML / MD)는 **문서 저장소**(`2nd-Team-Project/개발자별_일정표/`)에 둔다
- 두 저장소 간 충돌 시 v3.1 문서 기준(`07_요구사항_정의서.md`)이 우선이다
- 본 워크스페이스 파일은 빌드·런타임·테스트·CI에 영향 0 (`workspaces/README.md` §2)
