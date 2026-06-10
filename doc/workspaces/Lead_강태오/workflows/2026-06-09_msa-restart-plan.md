# 2026-06-09 MSA 재시작 분리 계획 (dev-msa)

> 작성: 강태오(Lead, AI 보조). 기준: 2026-06-09 회의록(MSA 서비스 분리 설계) + `CLAUDE.md` 도메인 경계.
> 본 문서는 dev-msa를 깨끗이 리셋한 직후의 **재출발 계획**이다.

## 0. 출발점 (현재 상태)

- `dev-msa` = `origin/dev` (PR #340 `1397f4e` + CI 거버넌스 `7eeb31e`)로 hard reset 완료. **순수 모놀리식, service 분리 폴더 없음.**
- 이전 MSA Phase 1(DB-per-service / Circuit Breaker / service-gateway·ai-service·bible-service 분리 / admin-service 스캐폴드 #415 등) 71개 커밋은 **폐기**. 새 회의 결정(단일 DB·Kafka 최소화)과 반대였기 때문.
- 폐기분 백업: 태그 `archive/dev-msa-before-reset-20260609` (= `95ecfb4`). 필요 시 복구 가능.
- 현재 모놀리식 도메인 14개: `admin, ai, audit, bible, member, mission, music, note, notification, praise, qt, report, sharing, study`.

## 1. 목표 구조 (회의 결정 요약)

- **사용자 서비스 4개 + 관리자 서버(모놀리식) + 단일 DB.**
- 서비스 간 통신: **RestClient(동기)만**. 읽기 중심이라 Saga·보상 트랜잭션 불필요.
- **Kafka는 AI 영역에만** 유지(쓰기·잡 처리). 그 외 도입 금지.
- 인증: **유저 서비스가 JWT 발급**, 각 서비스는 요청의 JWT를 **필터로 검증**(네트워크 호출 없이 공유 키 검증).
- 코드 배치: **별도 레포를 더 만들지 않고, 모노레포 + 서비스별 폴더(Gradle 멀티모듈).**
- QT 콘텐츠: 날짜별 JSON을 오브젝트 스토리지에 올려 앱이 직접 읽음(서버 부하·인증 축소).

## 2. 도메인 → 서비스 매핑 (14개)

| 서비스 | 담는 도메인 | 비고 |
|--------|-------------|------|
| **service-user** (인증·마이페이지) | member, notification, mission | JWT 발급 주체. 의존 허브 |
| **service-bible** (읽기전용 콘텐츠) | bible, qt, study, music, praise | 쓰기 없음 → 가장 먼저 추출(패턴 확립). 모두 "읽기 콘텐츠" 성격이라 묶음 |
| **service-note** | note, sharing, report(사용자 제출) | verseId는 화면이 쿼리파라미터로 전달 |
| **service-ai** | ai | Kafka 유지 |
| **admin-server** (모놀리식 복사본) | admin, audit, report(검수·관리) + 전 도메인 admin 컨트롤러 | 같은 DB 직접 접근. admin API만 남김 |

**모호 도메인 3개 — 회의록 원칙("성격이 같은 도메인은 묶자")에 따라 확정(2026-06-09):**

- `music`(앱 배경음악 스트리밍) → **service-content**. 사용자별 데이터가 아닌 앱 전역 읽기 콘텐츠라 bible/qt/study와 성격 동일.
- `praise`(찬양 큐레이션) → **service-content**. 읽기 전용 조회. 음원 미저장 정책이라 가벼운 읽기.
- `report`(신고) → **제출=service-note / 관리=admin-server**. 쓰기 제출은 신고 대상(노트·공유물) 근처에, 검수 CRUD는 회의록 §6대로 admin이 단일 DB 직접 처리.

> 참고: music·praise가 합류하면서 이 서비스는 "성경"보다 "콘텐츠 서비스"가 정확하나, 회의록 명칭을 존중해 통칭 "성경/콘텐츠"로 둔다.

## 3. 모노레포 레이아웃 (Gradle 멀티모듈)

```
qtai-server/                 (← 루트 빌드, settings.gradle.kts가 모듈 묶음)
  lib-common/                공통: 응답/예외, JWT 검증 필터, RestClient 설정, 공통 유틸
  service-user/              member, notification, mission        (port 8081)
  service-bible/             bible, qt, study, music, praise      (port 8082)
  service-note/              note, sharing, report(제출)          (port 8083)
  service-ai/                ai  (+ Kafka)                         (port 8084)
  admin-server/             admin, audit, report(관리) + admin    (port 8090)
```

> **브랜치 분담 확정(2026-06-09):** 백엔드 재편(service 4개 **+ admin-server**)은 전부 **dev-msa** 한 브랜치에서. `dev-admin-web`은 **관리자 웹 React 프론트(`admin-web/`)만** 담당 → 같은 `qtai-server`를 두 브랜치가 동시에 뜯어 머지 충돌나는 상황을 막는다. admin-server는 멀티모듈 속 모듈 하나(별도 pod)로, 4개 서비스와 동일 구조.

> **모듈 방식: Gradle 멀티모듈로 확정(2026-06-09).** 회의록 철학(단순·과분리 금지)에 부합하고, 각 모듈이 별도 jar·프로세스·k8s pod로 떠서 "장애 격리" 목적을 달성하면서 공통 코드(lib-common)를 중복 없이 공유한다. 독립 Gradle 프로젝트는 셋업·중복 부담만 커 채택하지 않음.

- 각 모듈은 **자체 Spring Boot main + 자체 포트(env 주입)**.
- **단일 DB**: 모든 서비스가 같은 MySQL을 바라봄(접속정보 env 공유). 도메인은 **테이블로 구분**.
- `config / security / external / batch`는 공통 기술 영역으로 `lib-common` 혹은 각 서비스에 적절히 배치(`CLAUDE.md` §4 준수).

## 4. 단계별 작업 순서 (남은 약 3일)

**Day 1 — 골격 + 성경 서비스(파일럿)**
1. Gradle 멀티모듈 골격 + `lib-common`(공통 응답/예외, JWT 검증 필터, RestClient 빈).
2. **service-bible 추출**(bible+qt+study). 읽기 전용 + 외부 의존 없음 → 분리 패턴을 여기서 확립.
3. 부팅 스모크 테스트(`*ApplicationContextTest`) + 단일 DB 연결 확인.

**Day 2 — 유저 / 노트 / AI**
4. **service-user**: 로그인·JWT 발급(`POST /api/v1/auth/kakao`), 마이페이지. 다른 서비스 JWT 검증 필터 공유.
5. **service-note**: note+sharing. verseId는 쿼리파라미터로 받음(서비스 간 조회 호출 없음).
6. **service-ai**: ai 추출, **Kafka 유지**. 사전 생성·검증과 F-15 단발 Q&A만.

**Day 3 — 관리자 + 통합 + 배포**
7. **admin-server**: 분리 직전 모놀리식 복사 → admin 컨트롤러만 남기고 사용자용 컨트롤러 정리.
8. 서비스 간 **RestClient 연동** 마무리(필요한 조회만).
9. **로컬 쿠버네티스 매니페스트** + 전체 스모크/검증.

## 5. 횡단 관심사 (반드시 지킬 것)

- **단일 DB**: 각 서비스 JPA는 **자기 도메인 테이블만 매핑**. 타 도메인 Entity 직접 import 금지(`CLAUDE.md` §3). 타 도메인 데이터가 필요하면 RestClient 또는 화면 전달(verseId 패턴).
- **크로스 도메인 FK**: 같은 DB이므로 물리 FK는 유지 가능. 단 코드에서 타 도메인 엔티티를 매핑하지 않는다(읽기 참조라 보상 트랜잭션 불필요).
- **JWT**: 유저가 발급, 각 서비스는 공유 키로 필터 검증. 토큰 검증을 위해 유저 서비스를 매 요청 호출하지 않는다.
- **QT JSON → 오브젝트 스토리지(S3/R2)**: 회의록 §2 결정대로 날짜별 JSON(`2026-06-09.json`)을 오브젝트 스토리지에 업로드, 앱이 직접 읽음. 코드는 **S3 API(표준)** 기준으로 작성 → 로컬·시연은 **MinIO(S3 호환)**, 배포는 설정만 바꿔 **S3/R2**. 00:00 생성, 04:00 노출 기준은 그대로. service-bible(QT)의 스케줄 배치가 파일 생성, 누락 시 admin에서 점검·수동 생성.
- **금지(임시 구현도 금지)**: AI 자유챗봇·다중턴·SSE, RAG/vector DB, Kafka 남용(AI 외), DB-per-service, 교회 인증, 금지 번역본 데이터.

## 6. 담당 (회의록 기반)

| 담당 | 할 일 |
|------|-------|
| AI 담당(강상민) | service-ai 분리 + RestClient 연동 (Kafka는 AI만) |
| note·sharing 담당(이승욱) | service-note 분리 + verseId 쿼리파라미터 연동 |
| 관리자 담당(1인) | admin-server: 모놀리식 복사 → admin 컨트롤러만 |
| Lead(강태오) | 멀티모듈 골격 + lib-common + service-user + 취합(dev-msa) |

## 7. 검증 게이트 (각 서비스)

- 부팅 스모크(`*ApplicationContextTest`), ArchUnit/Spring Modulith 도메인 경계 테스트.
- `./gradlew build` / `test` / 커버리지, Spectral lint(OpenAPI), gitleaks.
- CI 필수 체크 유지: `no-cross-merge`, `qtai-server Build & Test`, `Flutter Analyze & Test`.

## 8. 브랜치 운영

- `dev-msa`는 **PR을 받는 통합 브랜치**. 통합 브랜치에 직접 커밋하지 말고, 그 아래 `feature/msa-*` 작업 브랜치를 파서 PR.
- PR은 가능하면 10파일·500줄 이하, 관련 F-ID 명시(`CLAUDE.md` §12).
- `dev-msa`는 `dev`와 **자주 동기화**(막판 빅뱅 머지 회피).

## 9. 확정 사항 (2026-06-09 전부 확정)

- ✅ 모듈 방식: **Gradle 멀티모듈**.
- ✅ 모호 도메인 3개: music·praise → service-bible, report 제출=note / 관리=admin.
- ✅ QT 저장: **오브젝트 스토리지(S3 API)** — 로컬 MinIO / 배포 S3·R2.
- ✅ 서비스 명칭: **service-bible**(익숙한 명칭 유지, 안에 bible·qt·study·music·praise).
- ✅ admin-server 작업 브랜치: **dev-msa**(백엔드 일원화). dev-admin-web = React 프론트 전용.
- 비고: 담당표(§6)는 원래 분담 기준이며, 오늘 Lead 단독 진행 시 §4 순서대로 일괄 수행.
