# MSA 전환 종합 현황 리포트 (Day 1–3)

- **일자**: 2026-06-10
- **기준**: dev-msa (PR #433 머지 시점, head 532a611)
- **근거**: 회의록 2026-06-09 §1~§9, 계획 `2026-06-09_msa-restart-plan.md`, Lead 결정들
- **작성**: 강태오(Lead, AI 보조)

## 1. 전환 개요
모놀리식 `qtai-server`를 **Strangler 방식 Gradle 멀티모듈 MSA**로 전환 중. 회의록 확정 원칙:
- 사용자 서비스 4개(유저/성경/노트/AI) + 관리자 서버(모놀리식 복사) + **단일 DB**(테이블로 도메인 구분, DB-per-service 금지).
- 서비스 간 통신 **RestClient(동기)만** — 읽기 중심이라 보상 트랜잭션 불필요. **Kafka는 AI 영역만**(확장 금지).
- JWT는 service-user가 RS256 발급, 나머지는 공개키 검증.
- 관리자 기능은 admin-server(같은 DB), 콘텐츠/AI 서비스에선 `/api/v1/admin/**` 차단.
- QT 콘텐츠는 날짜별 JSON을 오브젝트 스토리지(S3/R2, 로컬 MinIO)로. 로컬은 쿠버네티스 배포로 마무리.

## 2. 모듈 구성 (현재 6모듈)
| 모듈 | 도메인 | 포트 | 비고 |
|------|--------|------|------|
| lib-common | 공통 응답·예외·BaseEntity·JWT 검증필터·RestClientConfig·Clock | — | 라이브러리(미배포) |
| service-user | member, notification, mission | 8081 | JWT 발급(RS256), Kakao |
| service-bible | bible, qt, study, music, praise | 8082 | 읽기 콘텐츠 + QT 스냅샷 스토리지 |
| service-note | note, sharing, report(제출) | 8083 | JournalEvent 아웃박스 |
| service-ai | ai | 8084 | 사전생성/검증, F-15 Q&A. @Scheduled(현재 Kafka 미사용) |
| admin-server | (모놀리식 복사) admin·audit·report관리 등 | 8090 | /api/v1/admin/** 만, 단일 DB 공유, lib-common 비의존 standalone |

root 모놀리식 `qtai-server/src`는 Strangler 동안 무손상 유지(추출 완료 후 제거 예정).

## 3. 머지된 PR (Day 1–3)
| PR | 내용 | 머지 |
|----|------|------|
| #420/#422 | 멀티모듈 골격 + lib-common + service-bible(bible·music·praise) | dev-msa 2bac476 |
| #424 | service-bible에 qt·study 이전(PR#2) | 7885bb7 |
| #426 | qt·study 쓰기/파싱 단위테스트 보강 | 9eb3579 |
| #425 / #428 / #429 | service-user(member·notification·mission) + 보강·폴리시 | 058b033 / 2d4fc24 / 396937b |
| #427 | service-note(note·sharing·report제출) | a3f59b4 |
| #430 | service-ai(ai 도메인 전체 + cross-domain Mock·LLM·보안·금지기능 테스트) | (Day2-5) |
| #431 | QT 일자별 JSON 스냅샷 오브젝트 스토리지 | 3f1e49a |
| #432 | CI Requirements Guard k8s/helm 차단 완화(로컬 배포 허용, SSoT 갱신) | db4c796 |
| #433 | admin-server 모놀리식 통째 복사 + admin 컨트롤러만 | 532a611 |

통합 검증: dev-msa에서 `gradlew projects`로 6모듈 + root 빌드스크립트 정상 평가 확인. 각 모듈 빌드/테스트는 해당 PR에서 통과.

## 4. cross-domain 결합 현황 (→ RestClient 통합 대상)
현재 도메인 간 호출은 임시 `client/{도메인}/...UseCaseMock`으로 대체돼 있다(통합 시 RestClient 어댑터로 교체·삭제, CLAUDE.md §4).
- service-ai: audit·admin·study·qt·bible (UseCase 7종)
- service-bible(qt): ai·member·note
- service-note: bible·member·notification
- service-user: 일부 admin/sharing/report 등 + mission→note

audit/admin 호출 대상은 admin-server. 단일 DB라 읽기 참조는 보상 트랜잭션 불필요.

## 5. 진행 중·남은 작업
- **로컬 배포(Docker Compose + k8s)** — 별도 세션 진행 중(worktree `QT-AI-deploy`). 가드 완화(#432)로 가능. 5서비스 컨테이너화 + 단일 MySQL·Redis.
- **RestClient 통합** — 별도 세션 진행 중(worktree `QT-AI-restclient`). Mock→실제 HTTP 어댑터 + 대상 서비스 내부 엔드포인트.
- **후속(통합 단계)**: 단일 DB schema validation 운영 정책 명문화, admin 권한 시나리오 테스트 추가 보강, QT 스냅샷 payload enrich(절 본문 — 번역본 저작권 정책 검토), retention 가드 활성화, AI Kafka 도입(필요 시 archive 태그 95ecfb4/PR#340~415 참고, AI만).
- **Strangler 마무리**: 모든 추출 완료 후 root 모놀리식 `qtai-server/src` 제거 → dev-msa를 dev로 취합.

## 6. 운영 규칙·교훈 (정착됨)
- **단일 DB**: H2(로컬/테스트)/MySQL(env), ddl-auto 기본 validate·테스트 create-drop.
- **첫푸시 APPROVE**: 브랜치 `feature/` + 푸시 전 dev-msa 병합 + Requirements Guard 금지토큰 회피(주석·테스트 리터럴 포함) + 구체 예외·인가 Mock 403·스케줄러 게이트 + 필수 테스트(MockMvc·ArchUnit·금지기능 부재·승인 게이트). 상세 체크리스트는 팀 공유.
- **거버넌스**: CI 가드 완화는 SSoT(CLAUDE.md/요구사항) 먼저 갱신 + Lead 결정 문서 + PR 근거 링크 후에만(우선순위 07→03→…→CI).
- **자동머지 타이밍**: claude-review가 빌드보다 먼저 끝나면 auto-merge 스킵 → 빌드 green 후 claude-review 재실행으로 트리거.
