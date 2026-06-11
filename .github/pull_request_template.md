## 구현 내용
<!-- 이 PR에서 해결하거나 추가하는 변경 사항을 간략하게 설명하세요. -->

## 관련 F-ID / 이슈
<!-- 관련 F-ID: F-01 ~ F-17 중 (F-11 교회 인증은 MVP 제외) -->
<!-- 관련 이슈: Closes #000 / Fixes #000 -->

## 기준 문서 (어느 문서/섹션 기준?)
<!-- 예: 07_요구사항_정의서.md v3.1 §6.4 (F-03 묵상 노트), 02_ERD §notes, 04_API §/api/v1/notes/** -->

## 변경 유형
- [ ] 기능 추가 (feat)
- [ ] 버그 수정 (fix)
- [ ] 리팩토링 (refactor)
- [ ] 테스트 (test)
- [ ] 문서 (docs)
- [ ] 인프라 / CI (chore)

## 코드 체크리스트 (v3.1 기준)

### A. 거버닝 SSoT 충돌
- [ ] `07_요구사항_정의서.md` v3.1과 충돌 없음
- [ ] `02_ERD_문서.md` / `04_API_명세서.md` / `06_화면_기능_정의서.md`와 충돌 없음

### B. 보안·시크릿
- [ ] `application.yml` 또는 코드에 평문 Secret 없음, `.env` 커밋 없음
- [ ] 권한 검증 — 8 role (ANONYMOUS/USER/ADMIN/OPERATOR/REVIEWER/CONTENT_CREATOR/SUPER_ADMIN/SYSTEM_BATCH)
- [ ] ADMIN role 단독으로 관리자 작업 허용 X (`admin_users.admin_role` 함께 검사)

### C. 금지 기술/기능 (Requirements Guard 자동 차단 대상)
- [ ] AI 자유 챗봇 / 다중 턴 / SSE / `SseEmitter` / `text/event-stream` / `/ai/sessions/**` 없음
- [ ] 해설·시뮬레이터 사용자 요청 즉시 생성 없음 — 04:00 KST 배치 또는 관리자 트리거만
- [ ] (v1) Spring `ApplicationEventPublisher` 사용 — v2 MSA 분리 트랙은 Kafka 허용(2026-06-08)
- [ ] (v1) Docker Compose — v2 MSA 분리 트랙은 Kubernetes/Helm 허용(2026-06-08)
- [ ] MySQL 인덱스만 — RAG / ChromaDB / Vector DB / Elasticsearch / EmbeddingStore 없음
- [ ] 교회 인증 F-11 화면 / 버튼 / API / DB 필드 없음
- [ ] AI 찬양 추천 / 가사·음원 서버 저장 / 직접 YouTube URL 입력 없음

### D. 금지 데이터·번역본
- [ ] 개역개정 / ESV / NIV 저장·응답·테스트 데이터 없음
- [ ] 성서 유니온 / 두란노 본문 텍스트 저장 없음 (범위 정보만)

### E. Spring Boot 3.3.x + Jakarta + 도메인 경계
- [ ] `javax.*` import 없음, `jakarta.*` 사용
- [ ] 다른 `com.qtai.domain.*` 의 Entity/Service/Repository 직접 import 없음 (DTO/Port만)
- [ ] DB 변경 메서드에 `@Transactional` 있음, 조회는 `readOnly=true`
- [ ] 인프로세스 이벤트는 `@TransactionalEventListener(phase = AFTER_COMMIT)`

### F. 도메인 로직 + 핵심 7정책
- [ ] 시뮬레이터 4상태 (READY/MISSING/FAILED/DISABLED) — READY만 보기 버튼 활성
- [ ] 검증 통과 콘텐츠만 노출 (APPROVED 상태 외 차단)
- [ ] `validation_reference_jobs` 접근 = CONTENT_CREATOR / SYSTEM_BATCH만
- [ ] 묵상 1일 1건 멱등 정책 (`active_unique_key='ACTIVE'` + 삭제 시 NULL)
- [ ] 핵심 7정책 준수: 로그인 강제 / 자동저장 금지 / 기본 비공개 / 공유 스냅샷 / AI 단발성 / 검증 통과만 노출 / 검증 참조 원문 보호
- [ ] 금지 표현 없음: '저작권 문제 없음', '유실률 0% 보장', '내부 API 경로', 사용자 노출 '주석'(→ '해설' 사용)

## 테스트 체크리스트

- [ ] 단위 테스트 작성 완료 및 `./gradlew test` 로컬 통과
- [ ] 통합 테스트 작성 완료 또는 미작성 사유 명시: <!-- 예: 외부 OAuth Mock 한계 -->
- [ ] 예외 케이스 테스트 포함 (실패 경로, 권한 부족, 차단 상태)
- [ ] 커버리지 KPI 확인: Domain 70% / Application 70% / Presentation 50% / Infrastructure 40% / 전체 70%
- [ ] docs / chore 타입은 위 항목 면제

## 테스트 방법
<!-- 어떻게 테스트했는지 설명 (Unit / Integration / 수동 명령) -->

## Workflow / Report (필수)
- workflow: workspaces/{담당자}/workflows/YYYY-MM-DD_{task}.md
- report: workspaces/{담당자}/reports/YYYY-MM-DD_{task}_report.md

## 남은 리스크 / 후속 PR
<!-- 남은 이슈, 다음 PR에서 처리할 부분, 수동 검증 필요 사항 -->

---

## 🤖 자동 머지 안내

이 PR은 다음 조건이 충족되면 **자동으로 squash merge → dev** 됩니다:
1. Claude PR 자동 리뷰가 **APPROVE**
2. CI 체크(spring-build, flutter-test, requirements-guard, gitleaks, spectral, docker-compose) 전부 **success**

> Code Owner의 사람 리뷰는 머지 후에도 review/comment로 가능합니다.
> 자동 머지 정책은 `09_Git_규칙.md` v3.1 + `18_코드_품질_게이트.md` v2.4 기준입니다.
