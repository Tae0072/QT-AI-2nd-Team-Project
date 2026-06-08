# 2026-06-08 · MSA 전환 거버넌스 결정 기록 (Governance Decision Record)

작성: Claude (Lead 강태오/T 지시) · 대상: 문서 저장소 `2nd-Team-Project` + 구현 저장소 `QT-AI-2nd-Team-Project`
상태: **결정·문서 반영 완료(파일 수정)** · 커밋/PR은 담당자가 진행

## 1. 결정 (한 줄)

백엔드를 v1 단일 `qtai-server` Modular Monolith에서 **v2 MSA(서비스 분리)로 전환**하는 것을 공식 착수한다. 이에 따라 **Kafka·Kubernetes·Helm·독립 서비스 디렉터리 생성·"단일 모놀리식 고정"** 규칙은 *v1 한정*으로 축소되고, **v2 MSA 분리 트랙에서는 허용**된다.

- 결정 주체: Lead 강태오 (T) · 결정일: 2026-06-08
- 근거 설계: 이승욱 「MSA 분리 설계안(AI 제외 5개 서비스)」, Lead 로드맵 「2026-06-08_msa-migration-ai-first.md」(ai-service 별도 트랙)
- 정합성: Lead 로드맵의 Kafka(Phase 3) 계획과 일치

## 2. 무엇이 바뀌나 (금지 해제 범위)

다음은 **v1 한정 규칙**으로 바뀌고 **v2 MSA 분리 트랙에서 허용**된다.

| 항목 | v1 (단일 배포) | v2 (MSA 분리) |
|---|---|---|
| 백엔드 구조 | 단일 `qtai-server` Modular Monolith | 서비스 분리 |
| 메시징 | Spring `ApplicationEventPublisher` (+outbox) | **Kafka 허용** |
| 오케스트레이션/인프라 | Docker Compose | **Kubernetes·Helm 허용** |
| 독립 서비스 디렉터리 | 임의 생성 금지 | **MSA 트랙에 한해 허용** |

## 3. 그대로 유지되는 금지 (해제 아님)

아래는 이번 결정과 무관하게 **계속 금지**다. 문서/CI에서 건드리지 않았다.

- AI 자유 챗봇, 다중 턴 대화, SSE, `/ai/sessions/**`
- RAG, ChromaDB, vector DB, Elasticsearch, 임베딩 검색
- 개역개정·ESV·NIV, 성서유니온·두란노 본문 텍스트 저장
- 교회 인증(F-11) 화면/버튼/API/DB 필드, AI 찬양 추천, 찬양 가사·음원·직접 YouTube URL 저장
- plain secret/token/password/private key, PostgreSQL/ZooKeeper

## 4. 팀 범위 (분리 담당)

- **이승욱·강상민 = MSA 분리 작업 공동 담당** (강상민 2026-06-08 합류). 이승욱 = `note`·`sharing` 등 사용자 도메인, 강상민 = `ai` → `ai-service` + 분리 공통 인프라(게이트웨이·outbox→Kafka·`qtai-common`).
- bible / today-qt / mypage(코어)는 도메인 오너(**이지윤·김태혁·강태오**)와 협의하는 **팀 아키텍처 결정 사안**.
- **이후 전체 팀원이 분리된 MSA(서비스 단위)를 기준으로 작업한다.** 각 도메인 오너(이지윤=bible, 김태혁=qt/simulator, 김지민=Flutter·관리자웹, 강태오=member/플랫폼 코어)가 자기 도메인의 서비스 분리·연동을 담당한다. `ai-service`는 강상민 주도 + Lead 로드맵(ai-first) 병행.
- 관리자 웹은 강태오·김지민 트랙 유지(별개).

## 5. 반영한 문서·파일 (두 레포)

원칙: 살아있는 규칙·강제(enforcement)만 수정. **changelog·회고(과거 기록)는 보존.** 총 81개 지점 수정.

### 문서 저장소 `2nd-Team-Project`

| 문서 | 반영 내용 |
|---|---|
| `00_개발_일정_총괄표.md` | 백엔드 구조·Kafka/K8s 대체표 v2 허용 표기 + **이승욱 MSA 분리 담당 노트** 신설 |
| `00_문서_역할_분리표.md` | Kafka/K8s "보류"→"v1 보류·v2 전환" |
| `01_프로젝트_계획서.md` | 백엔드 구조·메시징·인프라·OPS·기술선택표 v2 허용 |
| `03_아키텍처_정의서.md` | §3.1.2 v2 분리 "착수"로 격상 + Kafka 도입 시점 v2 명시 |
| `07_요구사항_정의서.md` | **§2.4 "백엔드 아키텍처 진화(v2 MSA)" 신설** |
| `09_Git_규칙.md` | 모놀리식 운영·Kafka/K8s 금지·독립 서비스 디렉터리·PR 체크리스트 v2 허용 |
| `10_환경_설정.md` | Kafka/K8s/Helm v2 도입 표기(ES/Chroma/vector는 유지 금지) |
| `12_코드_리뷰_규칙.md` | 금지 기술 목록에서 Kafka/K8s/Helm 분리(v2 허용) |
| `14_배포_가이드.md` | K8s/Helm/Kafka v2 도입 표기 |
| `18_코드_품질_게이트.md` | 금지 기술 목록·경계 방어선 주석 + **rg 차단 블록(Kafka/K8s/Helm) 비활성화** |
| `20_CICD_파이프라인.md` | **rg 차단줄에서 Kafka/Kubernetes/Helm 제외** + 금지표 v2 허용 |
| `22_구현_저장소_반영_체크리스트.md` | 모놀리식 기준·admin 귀속·금지 기술·점검 항목 v2 허용 |
| `23_도메인_용어사전.md` | qtai-server/Modular Monolith/Kafka/K8s/Helm 정의에 v2 전환 표기 |
| `.github/pull_request_template.md` | 모놀리식 체크 항목을 v1/v2 트랙 구분으로 |

### 구현 저장소 `QT-AI-2nd-Team-Project`

| 파일 | 반영 내용 |
|---|---|
| `CLAUDE.md` | §1 서버 형태 v2 MSA 착수 표기 · §8 Kafka/K8s/Helm 금지 → **v2 허용으로 해제(취소선)** |
| `.github/workflows/qt-ai-ci.yml` | **Kafka 차단(#5)·K8s/Helm 차단(#6) → 비차단 NOTICE로 변경** (FAILED 제거) |
| `.github/workflows/claude-pr-review.yml` | 자동 리뷰 금지 인프라 목록에서 Kafka/K8s/Helm 제외 + 백엔드 설명 v2 표기 |
| `doc/프로젝트 문서/03·09·18·22·23` | 문서 저장소와 동일 사본 동기화 |

> ⚠️ 참고: 구현 저장소 `doc/프로젝트 문서/03_아키텍처_정의서.md` 사본은 일부 바이트가 손상돼 있었다(기존 상태). 손상 바이트를 보존하며 해당 라인만 안전 편집했다. 원본 재동기화를 별도 권장.

## 6. CI 게이트 변경 효과

- `qt-ai-ci.yml`: 기존에는 `qtai-server/`에 Kafka 패턴이나 `k8s/`·`helm/` 디렉터리가 있으면 빌드를 **[BLOCK]+FAILED** 처리 → 이제 **[NOTICE]만 출력, 실패 없음**. 즉 이승욱의 Kafka/outbox·게이트웨이·K8s 작업 PR이 CI에서 자동 차단되지 않는다.
- 여전히 차단: SSE·RAG·벡터DB·Elasticsearch·개역개정/ESV/NIV·교회 인증 패턴.

## 7. PR 초안

### PR A — 문서 저장소 `2nd-Team-Project`

- 브랜치(예): `docs/msa-governance-2026-06-08` (← `dev`에서 분기)
- 제목: `docs: v2 MSA 전환 거버넌스 반영 — 단일 모놀리식·Kafka/K8s/Helm 금지 v1 한정화`
- 본문:
  - 2026-06-08 Lead 결정으로 v2 MSA 분리 착수. 관련 금지(Kafka/K8s/Helm/독립 서비스 디렉터리/단일 모놀리식 고정)를 v1 한정으로 축소, v2 트랙 허용.
  - 변경 문서: 00·00역할·01·03·07·09·10·12·14·18·20·22·23 + PR 템플릿 (§5 표 참조)
  - 유지 금지: AI 자유챗봇·SSE·RAG·저작권 본문·교회인증 등(§3)
  - 팀 범위: 이승욱 = MSA 분리 담당(note·sharing 단독, 그 외 팀 결정)
  - changelog/회고 등 과거 기록은 미변경

### PR B — 구현 저장소 `QT-AI-2nd-Team-Project`

- 브랜치(예): `chore/msa-governance-ci-2026-06-08` (← `dev`에서 분기)
- 제목: `chore: v2 MSA 허용 — CLAUDE.md/CI 게이트 금지 해제(Kafka·K8s·Helm)`
- 본문:
  - CLAUDE.md §1/§8, `qt-ai-ci.yml`(Kafka·K8s 차단 해제), `claude-pr-review.yml`, `doc/프로젝트 문서` 사본 동기화
  - 이 PR 이후 이승욱 MSA 분리 PR이 CI 자동 차단되지 않음
  - 손상된 03 사본은 라인 단위 안전 편집(원본 재동기화 권장)
- 주의: `.github/workflows/*` 변경은 리뷰 필수. 머지 후 첫 빌드에서 NOTICE 로그 확인.

## 8. 후속 액션

1. 본 PR 2건 리뷰·머지(거버넌스 먼저, 그래야 이승욱 분리 PR이 CI 통과).
2. 이승욱: 1단계 `bible` PoC(스키마 분리 + 게이트웨이 + JWT 검증 라이브러리), `qtai-common` 공유 모듈, outbox→Kafka 인프라 선행 정비.
3. ai-service 트랙은 Lead 로드맵(`2026-06-08_msa-migration-ai-first.md`)과 병렬 진행.
4. ArchUnit/Spring Modulith 경계 테스트를 서비스 경계 기준으로 갱신.

## 9. 관계 문서

- 이승욱 「MSA 분리 설계안(AI 제외 5개 서비스)」 — 서비스 경계·통신·로드맵 상세
- `2026-06-08_msa-migration-ai-first.md` — ai-service 우선 분리 트랙(Lead)
- `03_아키텍처_정의서.md` §3.1.2 — v2 분리 통신 규칙

## 10. ⚠️ 기존 손상 파일 주의 (이번 변경과 무관, 복원 필요)

전수 스캔 결과 **구현 저장소 작업본에 인코딩이 손상된 파일 2개**가 있다. 이번 작업 이전부터 손상돼 있었고(메모리: Windows 한글 파일조작 이슈 추정), 본 작업은 손상 바이트를 보존한 채 해당 라인만 안전 편집했다. **깨끗한 `dev`/`master`에서 복원 후 아래 편집만 재적용**을 권장한다.

1. `.github/workflows/claude-pr-review.yml` — 파일 끝이 잘림(auto-merge 타임아웃 스크립트 블록이 `...자동 머지를 건`에서 절단). **YAML 끝부분 누락 가능 → CI 동작 영향 우려.**
   - 복원 후 재적용 편집 2건:
     - (백엔드 설명) `- 백엔드: 단일 qtai-server Spring Boot 3.3 Modular Monolith (...)` → `- 백엔드: 단일 qtai-server Modular Monolith(v1) → v2 MSA 분리(2026-06-08) (...)`
     - (자동 차단 목록) `- 인프라: Kafka, KafkaTemplate, spring-kafka, Kubernetes, Helm` → `- 인프라(주의): Kafka·Kubernetes·Helm은 v2 MSA 분리 트랙에서 허용(2026-06-08). v1 단일배포 PR에서만 점검.`
2. `doc/프로젝트 문서/03_아키텍처_정의서.md` (사본) — 중간(≈55KB) 손상. **문서 저장소 `2nd-Team-Project/03_아키텍처_정의서.md`(정상본)에서 재동기화** 후, 본 GDR §5의 03 편집 2건(§3.1.2 v2 착수 격상, Kafka 도입 시점 v2) 재적용.

> 참고: 문서 저장소(`2nd-Team-Project`)의 원본 03은 손상되지 않았다. 손상은 구현 저장소 작업본 사본에 한정.
