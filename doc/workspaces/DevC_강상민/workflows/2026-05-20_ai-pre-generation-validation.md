# Workflow — 2026-05-20 ai-pre-generation-validation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-pre-generation-validation` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 기준 문서 | `07_요구사항_정의서.md` §6.3, §6.14, `03_아키텍처_정의서.md` §8.1, `05_시퀀스_다이어그램.md` §10.2 |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/main/java/com/qtai/batch/**`, `qtai-server/src/main/java/com/qtai/external/llm/**` |

## 작업 목표

해설·시뮬레이터 산출물을 사용자 요청 시점이 아니라 04:00 KST 배치 또는 관리자 트리거에서 사전 생성하고, 자동 검증을 통과한 산출물만 사용자 노출 테이블과 연결할 수 있는 흐름을 만든다.

## 문제 정의

Today QT는 00:00 KST에 공개되지만 수집·AI 생성 배치는 04:00 KST에 수행된다. 00:00부터 04:00 전까지는 이전에 준비된 cache를 제공해야 하며, 사용자 요청 경로에서 해설이나 시뮬레이터를 즉시 생성하면 제품 기준과 비용·품질 기준을 모두 위반한다.

## 범위

- 시스템 배치 또는 관리자 재생성 요청에서만 AI 생성 작업을 생성한다.
- `commentary_materials`와 QT/Bible 컨텍스트를 입력으로 해설 산출물 초안을 만든다.
- 생성 결과는 `ai_generated_assets.status=VALIDATING`으로 저장한다.
- 자동 검증 결과를 `ai_validation_logs`에 남긴다.
- 검증 통과 시에만 후속 작업에서 `verse_explanations` 또는 `simulator_clips`에 연결할 수 있도록 결과를 반환한다.

## 제외 범위

- 사용자 API에서 실시간 해설 생성 버튼을 만들지 않는다.
- 성서유니온, 두란노 본문 텍스트 저장은 하지 않는다.
- 검증 참조 원문을 사용자 응답이나 일반 관리자 목록에 포함하지 않는다.
- 시뮬레이터 clip 파일 생성 자체는 김태혁 담당 범위와 조율 전까지 mock 결과로 둔다.

## 생성 흐름

```text
SYSTEM_BATCH 또는 REVIEWER
→ CreateAiGenerationJobUseCase
→ Qt/Bible/context 조회
→ external.llm 호출
→ ai_generated_assets(VALIDATING) 저장
→ 1층 형식 검증
→ 2층 정책 검증
→ ai_validation_logs 저장
→ PASSED면 승인 후보, 실패면 REJECTED 또는 NEEDS_REVIEW
```

## 검증 기준

- 본문 좌표와 target id가 일치해야 한다.
- asset type은 `EXPLANATION`, `SUMMARY`, `GLOSSARY`, `SIMULATOR` 중 명세 범위만 허용한다.
- 사용자 신앙 상태 평가, 단정적 설교, 가치 판단 문장이 있으면 반려한다.
- 출처 표기 정책을 충족하지 못하면 `NEEDS_REVIEW` 또는 `REJECTED`로 둔다.
- 외부 AI 응답 원문은 검증 전 사용자에게 반환하지 않는다.

## 구현 순서

1. `ai-generation-log-model` 작업이 머지되어 있는지 확인한다.
2. 배치 진입점 또는 system UseCase를 만든다.
3. `commentary_materials` 입력 조회는 실제 bible/study 계약 전까지 adapter mock으로 분리한다.
4. LLM 호출 전 `ai_generation_jobs`를 `RUNNING`으로 전환한다.
5. LLM 실패, 검증 실패, 부분 성공 케이스를 모두 상태로 남긴다.
6. 통과 결과만 승인 후보로 반환하고 사용자 노출 연결은 관리자 승인 작업으로 넘긴다.

## 수용 기준

- [ ] 해설·시뮬레이터 생성은 batch/admin 경로에서만 시작된다.
- [ ] 사용자 요청 경로에서 LLM client를 호출하지 않는다.
- [ ] 생성 실패와 검증 실패가 서로 다른 상태와 사유로 남는다.
- [ ] 검증 참조 원문이 사용자 응답, 로그, 일반 관리자 목록에 노출되지 않는다.
- [ ] 같은 target 재생성 시 기존 산출물과 신규 산출물을 구분할 수 있다.

## 테스트 계획

- Service 테스트: 생성 성공, 외부 AI 실패, 검증 실패, 중복 재생성
- Controller 또는 UseCase 테스트: `SYSTEM_BATCH` 주체만 system 생성 경로 접근 가능
- 경계 테스트: 사용자 API에서 `external.llm` 직접 호출 없음

## 검증 명령

- `./gradlew -p qtai-server test --tests "*Ai*"`
- `./gradlew -p qtai-server build`
- `gitleaks detect --source . --redact --exit-code 1`

## 리스크와 대응

| 리스크 | 대응 |
| --- | --- |
| 실제 bible/study 계약 미완성 | `client/qt` mock adapter로 target context를 임시 제공하고 TODO를 남긴다. |
| LLM 응답 스키마 변동 | provider raw response를 직접 노출하지 않고 내부 parser 실패를 `FAILED`로 기록한다. |
| 관리자 승인 전 사용자 노출 | `APPROVED` 연결 전에는 `verse_explanations`에 활성 연결하지 않는다. |
