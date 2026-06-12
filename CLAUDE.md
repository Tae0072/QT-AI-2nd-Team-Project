# CLAUDE.md — QT-AI 서버 구현 공통 지침

이 파일은 실제 구현 저장소의 `qtai-server` 작업에서 Claude Code가 따라야 할 팀 공통 기준이다. 기준 문서는 문서 저장소의 `07_요구사항_정의서.md` v3.1이며, 구현 중 의사결정이 흔들리면 요구사항을 먼저 확인한다.

## 1. 프로젝트 기준

- 서비스: QT-AI, 큐티 AI 앱
- 서버 형태: 단일 `qtai-server` Modular Monolith
- admin-server 복사본 동기화: ① 도메인 로직은 항상 도메인 서비스(service-*)가 원본이고 admin-server는 따라간다 ② admin 고유 기능(admin 컨트롤러·관리자 배치)만 admin-server에서 직접 수정한다 ③ 스키마(Flyway)는 admin-server가 단독 소유하며 다른 모듈에 마이그레이션 파일을 두지 않는다. 상세: `doc/admin-server-sync-rules.md` (2026-06-11 Lead 결정, 코드리뷰 TODO 4)
- Backend: Java 21, Spring Boot 3.3, Gradle, Spring Modulith, ArchUnit
- DB/cache: MySQL 8.0, 테스트 H2, Caffeine app cache
- Redis: token/rate/idempotency 등 필요 범위 검토 후 사용
- AI: DeepSeek OpenAI-compatible client
- 외부 API: 사용자 Flutter 앱(`/api/v1/**`)과 별도 관리자 웹 프런트엔드(`/api/v1/admin/**`)가 같은 `qtai-server`를 호출. 관리자 UI는 Flutter 앱이 아니라 별도 웹(2026-05-19 강사님 직강 결정, `03_아키텍처_정의서.md` v1.2 §4.9 / §13.6)
- OAuth·인증 경로: 사용자 Flutter 앱은 카카오 토큰을 `POST /api/v1/auth/kakao`로 서버에 전달한다. 관리자 웹은 카카오가 아니라 **자체 아이디/비밀번호 로그인**(`POST /api/v1/admin/auth/login`, 토큰 갱신 `POST /api/v1/admin/auth/refresh`)을 사용하며, admin-server(8090)가 `admin_users`의 `username`/`password_hash`(BCrypt)를 검증한 뒤 ADMIN 토큰을 발급한다(2026-06-11 팀 결정, 기존 2026-06-10 관리자 카카오 결정 대체). 서버사이드 `/oauth2/**` 경로는 어느 쪽도 사용하지 않는다.
- 기본 응답·주석·PR 설명 언어: 한국어 우선
- 백엔드 코드 컨벤션: `CODE_CONVENTION.md`를 우선 확인

## 2. 기준 문서 우선순위

문서가 구현 저장소에 복사되어 있거나 링크되어 있으면 작업 전 관련 문서를 확인한다.

1. 요구사항: `07_요구사항_정의서.md`
2. 아키텍처: `03_아키텍처_정의서.md`
3. API 계약: `04_API_명세서.md`
4. 품질 게이트: `18_코드_품질_게이트.md`
5. 용어: `23_도메인_용어사전.md`
6. Git/PR 규칙: `09_Git_규칙.md`
7. 구현 반영 순서: `22_구현_저장소_반영_체크리스트.md`
8. 기능 상세: `25_기능_명세서.md`
9. 코드 컨벤션: `CODE_CONVENTION.md`

충돌 시 `07` → `03` → `04` → `18` → `23` → `09` 순서로 판단한다. 요구사항 자체 변경이 필요하면 임의로 수정하지 말고 Lead 검토가 필요하다고 표시한다.

## 3. 서버 도메인 경계

도메인은 `member`, `bible`, `qt`, `study`, `note`, `sharing`, `report`, `notification`, `praise`, `mission`, `ai`, `admin`, `audit`를 기준으로 나눈다.

- `note`, `sharing`, `praise`는 `bible` 하위 모듈이 아니라 최상위 도메인이다.
- 다른 도메인으로 공개할 계약은 `api/` 패키지의 UseCase/DTO에 둔다.
- 다른 도메인의 Entity, Service, Repository, infrastructure 타입을 직접 import하지 않는다.
- 내부 도메인 호출은 Java Interface와 DTO를 사용한다.
- 내부 호출을 HTTP 경로로 우회하지 않는다.
- Controller는 Repository를 직접 호출하지 않는다.
- 도메인 모델은 자기 도메인 안에 둔다.
- 도메인 간 반환값은 DTO로 전달한다.

## 4. 패키지·레이어 기준

2026-05-19 강사님 직강에서 확정한 도메인 표준 구조를 따른다(`03_아키텍처_정의서.md` v1.1 §3.1).

- `domain.<name>.api`: 외부 도메인에서 호출 가능한 UseCase interface와 DTO (`api/dto`)
- `domain.<name>.internal`: Entity, enum, Service, Repository, QueryRepository, 도메인 전용 예외 등 내부 구현. 외부 도메인 접근 절대 금지
- `domain.<name>.client`: 다른 도메인의 `api/UseCase` 호출 어댑터(`client/{타도메인명}/...UseCaseMock.java`)와 도메인 전용 외부 시스템 호출 어댑터. 선택 패키지로, 다른 도메인 호출이 없거나 외부 시스템 호출이 없는 도메인은 두지 않는다
- `domain.<name>.web`: `/api/v1/**` Controller와 외부 HTTP Request/Response DTO
- `common`: 공통 응답, 예외, 유틸
- `config`, `security`, `external`, `batch`는 공통 기술 영역으로 둔다. 여러 도메인이 공유하는 외부 시스템 호출은 `external/`에 둔다.

다른 도메인 호출은 항상 상대 도메인의 `api/UseCase` 인터페이스를 통한다. 통합 전에는 호출자 도메인의 `client/{타도메인명}/...UseCaseMock.java`로 임시 구현해 작업하고, 상대 도메인의 진짜 구현체가 등록되면 Mock을 삭제한다. 외부 시스템(Kakao, DeepSeek 등) 호출은 `client/{벤더명}Client`로 표현한다. 기존 저장소 구조가 다르면 기존 구조를 우선하되 위 경계를 깨지 않는다.

## 5. API 규칙

- 사용자·관리자 HTTP API는 `/api/v1/**` 아래에 둔다. 사용자 앱은 카카오 인증 `POST /api/v1/auth/kakao`, 관리자 웹은 **자체 아이디/비밀번호 인증** `POST /api/v1/admin/auth/login`·`POST /api/v1/admin/auth/refresh`(SecurityConfig permitAll)를 사용한다(2026-06-11 팀 결정, 관리자 카카오 대체). `/oauth2/**` 예외 경로는 사용하지 않는다.
- 관리자 API는 일반 회원 토큰의 `members.role=ADMIN`과 `admin_users.admin_role`을 모두 확인한 뒤, `OPERATOR`, `REVIEWER`, `CONTENT_CREATOR`, `SUPER_ADMIN` 중 API 명세에 맞는 세부 권한을 요구한다.
- 시스템 API와 배치/AI 내부 작업은 사용자 계정이 아니라 `SYSTEM_BATCH` 주체로 기록한다.
- 서비스 간 시스템(배치·스케줄러) 호출 인증은 사용자 토큰과 **분리**한다. 사용자 토큰은 service-user가 발급하는 **RS256**(비대칭, 공개키 검증)이고, 서비스 간 시스템 호출은 전달할 사용자 JWT가 없으므로 **공유 시크릿 기반 HS256 단명 `SYSTEM_BATCH` 토큰**을 사용한다(`security.jwt.system-secret`, 발급=`SystemTokenProvider`, 검증=`SystemTokenValidator`, `sub=0`·`role=SYSTEM_BATCH`·단명 만료). 공통 `JwtAuthenticationFilter`는 RS256 사용자 검증 실패 시 시스템 토큰으로 폴백 검증한다. 시스템 시크릿은 env로만 주입하고 로그·커밋에 남기지 않는다. 근거: `doc/workspaces/Lead_강태오/workflows/2026-06-10_service-to-service-system-auth.md`
- 인증되지 않은 사용자는 Kakao login 시작만 가능하다.
- 앱 콘텐츠 API는 인증된 역할 기준으로 보호한다.
- 내부 Java Interface는 OpenAPI에 노출하지 않는다.
- F-ID가 있는 기능은 API, 테스트, PR 설명에 관련 F-ID를 남긴다.

## 6. 고정 제품 결정

- QT 범위 공개 시각은 00:00 KST, 사용자 노출/cache refresh 기준 시각은 04:00 KST다.
- 00:00부터 04:00 전까지는 이전에 준비된 QT cache를 제공한다.
- AI 해설 generation job 시딩은 오늘 QT passage가 존재한다는 전제에서 00:05 KST 내부 배치로 수행할 수 있다. 이 시딩은 `EXPLANATION + BIBLE_VERSE` job 생성만 의미하며 승인본 사용자 노출을 보장하지 않는다.
- 해설 승인본, 시뮬레이터 상태, Today QT cache의 사용자 노출 갱신은 04:00 KST 기준으로 처리한다.
- "Today QT 100%"는 본문, 해설 진입점, 노트 진입점, 시뮬레이터 상태가 반환된다는 뜻이다.
- 모든 본문에 실제 시뮬레이터 clip이 있다는 뜻이 아니다.
- 시뮬레이터 상태는 `READY`, `MISSING`, `FAILED`, `DISABLED`만 사용한다.
- 시뮬레이터 버튼은 `READY`일 때만 활성화한다.
- 해설과 시뮬레이터 콘텐츠는 batch 또는 admin trigger로 사전 생성·검증한다. 단, SIMULATOR는 00:05 내부 해설 job 시딩 범위에 포함하지 않는다.
- 사용자 요청 경로에서 해설·시뮬레이터를 즉시 생성하지 않는다.

## 7. AI 구현 규칙

- 허용 AI 흐름은 사전 생성/검증과 F-15 단발성 사실 기반 Q&A뿐이다.
- F-15 Q&A는 단어, 시대상, 역사적 배경 질문으로 제한한다.
- Q&A는 single-turn으로 처리하고 이전 질문 맥락을 유지하지 않는다.
- 가치 판단, 신앙 평가, 상담, 설교식 단정 요청은 차단한다.
- 외부 AI 응답 원문은 검증 전 사용자에게 반환하지 않는다.
- 승인된 사용자 노출 해설은 `verse_explanations` 기준으로 제공한다.
- 검증용 한국어 주석 원문과 참조 자료는 사용자 응답, 로그, 관리자 일반 목록에 노출하지 않는다.
- AI Provider API key는 `external.llm` 같은 외부 연동 영역에서만 사용하고 로그에 남기지 않는다.
- AI 작업은 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`에 prompt/model/hash/status/error, 생성 지시 자산 버전, 검증 체크리스트 버전, 수행 주체를 기록한다.

## 8. 금지 기능·금지 기술

다음은 임시 구현도 금지한다.

- AI 자유 챗봇, 다중 턴 대화, SSE, `/ai/sessions/**`
- RAG, ChromaDB, vector DB, Elasticsearch
- Kafka, Kubernetes, Helm v1 도입
  - 예외(2026-06-10 Lead 승인): MSA 전환 마무리를 위해 **로컬 배포 한정 Kubernetes/Helm 매니페스트**를 허용한다(회의록 2026-06-09 §9 "로컬은 쿠버네티스 배포 형태로 마무리"). 운영(프로덕션) K8s/Helm 도입은 별도 결정이 필요하며, Kafka 확장 금지는 그대로 유지(Kafka는 AI 영역만). 이에 맞춰 CI Requirements Guard의 K8s/Helm 차단을 비차단으로 완화. 근거: `doc/workspaces/Lead_강태오/workflows/2026-06-10_local-k8s-deploy.md`
- 교회 인증 화면, 버튼, API, DB 필드
- AI 찬양 추천
- 찬양 가사, 음원 파일, 직접 YouTube URL 입력·저장
  - 예외(2026-06-07 Lead 승인): 앱 전역 배경음악(브금·찬송가)은 신규 `domain.music`에서 로열티프리/직접 제작 음원에 한해 DB 저장·스트리밍을 허용한다. F-09 찬양 큐레이션의 음원 미저장 정책과 위 금지(찬양 가사·사용자 디바이스 음원 업로드 등)는 그대로 유지. 근거: `doc/workspaces/Lead_강태오/workflows/2026-06-07_app-background-music.md`
- 개역개정, ESV, NIV seed/test/fixture/response 데이터
- 성서유니온 또는 두란노 본문 텍스트 저장
- plain secret, token, password, private key 예시

저작권 표현은 "저작권 문제 없음" 대신 "저작권 리스크를 낮춘다"를 사용한다. 이벤트 처리는 "유실률 0%" 대신 "핸들러 실패 로그와 재처리 가능 상태를 남긴다"로 표현한다.

## 9. 코드 작성 규칙

- `jakarta.*`를 사용하고 `javax.*`를 사용하지 않는다.
- write use case와 command service에는 `@Transactional`을 사용한다.
- read path에는 적절히 `@Transactional(readOnly = true)`를 사용한다.
- event handler 실패 로그에는 `eventId`, event type, handler name, error message를 남긴다.
- 로그에는 password, token, private key, 민감 개인정보를 남기지 않는다.
- 외부 API 오류는 공통 예외/응답 규칙으로 감싼다.
- 관련 없는 리팩터링, 대규모 포맷팅, 파일 이동은 하지 않는다.

## 10. 테스트 기준

변경 범위에 맞춰 단위 테스트, slice/integration test, ArchUnit/Spring Modulith 경계 테스트를 추가한다. 특히 아래 영역은 테스트 누락을 허용하지 않는다.

- 00:00 공개, 00:05 내부 AI 해설 job 시딩, 04:00 Today QT cache/user exposure 동작
- simulator status와 버튼 활성화 조건
- AI 자유 챗봇/SSE 부재
- F-15 Q&A 차단·검증·실패 처리
- 승인되지 않은 AI 산출물 미노출
- A/B/승인 해설 데이터와 검증 참조 자료 미노출
- admin authorization과 `SYSTEM_BATCH` 주체 검증
- Bible source metadata와 금지 번역본 데이터 차단
- event handler 실패 로그와 재처리 가능 상태
- 도메인 간 금지 import

## 11. 검증 명령

서버 변경 후 관련 범위에 맞게 실행한다.

```bash
./gradlew -p qtai-server build
./gradlew -p qtai-server test jacocoTestReport
./gradlew -p qtai-server jacocoTestCoverageVerification
npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml
gitleaks detect --source . --redact --exit-code 1
```

## 12. Git·PR 규칙

- `dev`에서 작업 브랜치를 만든다.
- `dev`와 `master`에 직접 push하지 않는다.
- 브랜치명은 `{type}/{scope}-{short-description}` 형식을 사용한다.
- 커밋은 Conventional Commits를 사용한다.
- PR 대상은 `dev`다.
- PR은 가능하면 10 files 이하, 500 changed lines 이하로 유지한다.
- 기능 PR은 관련 F-ID를 명시한다.
- `.gradle`, build output, coverage HTML, generated report, temporary file은 stage하지 않는다.
