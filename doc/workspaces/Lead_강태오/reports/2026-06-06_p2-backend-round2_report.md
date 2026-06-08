# 2026-06-06 P2 백엔드 2차(member/study/ai 안전 잔결함) 결과 보고

리뷰 §3 Medium/Low의 백엔드 항목을 현재 코드로 재검증 후, **결정 불필요한 안전 버그**를 처리한 2차.
**3개 브랜치/3개 커밋**, 각 브랜치 전체 스위트 0 failures. push·PR은 T가 직접.
워크플로우: `workflows/2026-06-06_p2-backend-round2.md`

## 처리한 항목(브랜치)

| 브랜치 | 커밋 | 내용 |
|--------|------|------|
| `fix/member-nickname-trim` | f944cb0 | 닉네임 trim 정책 일원화 — updateProfile만 trim하고 changeNickname은 안 하던 불일치를 changeNicknameInternal로 통합(공백-only 거부 포함). 테스트 2건 추가 |
| `fix/study-simulator-column-validation` | d12939d | SimulatorClip.sceneScriptJson columnDefinition `TEXT`→`LONGTEXT`로 마이그레이션(V15)과 정합 — MySQL ddl-auto=validate 어긋남 방지 |
| `fix/ai-checklist-admin-and-simulator-guard` | 693df59 | 체크리스트 버전 생성 시 createdByAdminId를 null로 저장하던 버그 수정(command.adminId() 전달) + SIMULATOR 생성 작업을 큐잉→즉시 FAILED 대신 생성 단계에서 NOT_IMPLEMENTED로 거부. 테스트 갱신·추가 |

## 재검증으로 "버그 아님" 확인(변경 없음)
- 시뮬레이터 런타임 JSON 파싱 실패 → `FAILED` 반환: simulator 상태 enum(READY/MISSING/FAILED/DISABLED) 정의상 정상.

## 남은 P2 — 성격별 분류(미착수)

### A. 결정/정책이 필요한 항목 (사용자 판단 필요)
1. **ai 검증 흐름** — layer2 `NEEDS_REVIEW`면 재검증 수단이 없어 승인 영구 불가(참조 PDF 미등록 초기 상태에서 전 해설 차단), 시스템 토큰으로 `PASSED` 로그 위조 가능, `activateForTarget=false` 승인 시 publish 경로 영구 상실. → 검증 정책·권한 모델 결정 필요.
2. **audit 조회 범위 확대** — 현재 `AI_GENERATED_ASSET`만 조회 가능(체크리스트/참조작업 감사는 기록되나 조회 불가). targetType↔actionType 화이트리스트가 결합돼 있어 "어디까지 조회 노출할지" 계약 결정 필요.
3. **audit before/after_json PII 가드** — 무엇을 PII로 보고 마스킹할지 정의 필요(현재 무가공 저장).
4. **member purge 감사** — 하드 삭제를 audit_logs에 남길지/형식(SYSTEM_BATCH 주체) 결정 필요(member→audit 교차 도메인).
5. **00:00~04:00 STALE_FALLBACK 캐싱** — 캐싱 시 04:00 이후 stale 노출 위험. 허용 stale 창(짧은 TTL 등) 정책 결정 필요.
6. **JacksonConfig** — Boot ObjectMapper를 @Primary 없이 대체 → spring.jackson.* 무효. 교체 시 전 JSON 직렬화 거동 영향(스냅샷 검증 동반).
7. **ai 스케줄러 단일 스레드** — LLM 지연이 04:00 배치까지 지연. 스레드풀/분리 도입은 설계 결정.

### B. 콘텐츠·마이그레이션 조율이 필요한 항목
8. **ai_prompt_versions 초기 시드** — 첫 배포 시 일일 시딩이 PROMPT_VERSION_NOT_FOUND로 실패. 시드가 필요하나 **실제 프롬프트 내용**이 있어야 함(콘텐츠 결정).
9. **AiBatchRunLog created_at 타임존** — `LocalDateTime`(시스템 기본) vs 형제 필드 `OffsetDateTime`(KST) 불일치. KST 일원화는 JPA auditing 전역(@CreatedDate 모든 엔티티) 영향 → 타임스탬프 거동 변경 검증 필요.
10. **member purge 인덱스** — `members(status, withdrawn_at)` 인덱스 추가(마이그레이션). 다른 미머지 브랜치(P1-10 V24 등)와 **마이그레이션 번호 조율** 필요.
11. **member_auth_providers CASCADE 테스트** — FK는 ON DELETE CASCADE로 정의됨. 동작 테스트는 H2(엔티티 FK 없음)로 불가 → **Testcontainers(Docker)** 필요.

### C. Flutter (별도 toolchain — flutter analyze/test 필요)
fontSize 미적용, GowunDodum 폰트 미로딩, 로컬 타임존 날짜계산(KST 불일치), 페이지네이션 미구현(피드/노트/알림), 묵상 4섹션 라벨 의미, release 빌드 debug 서명.

### D. 문서 drift
ERD↔실제 qt_passages/테이블 정합, OpenAPI 누락 핵심 경로(auth/kakao·qt/today 등) 등재, V9/V10/V11/V23 헤더 버전 오기.

## 권장 진행 순서(제안)
A(결정)은 답을 받은 뒤 묶음 처리 → B(콘텐츠·마이그레이션) → C(Flutter, 별도 검증) → D(문서). 각 항목 처리 시 동일하게 브랜치·테스트·리포트.
