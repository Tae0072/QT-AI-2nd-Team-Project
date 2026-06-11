# Workflow - 2026-06-11 ai-commentary-source-publish

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-commentary-source-publish` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-08 |
| 트리거 | P1 AI 주석 자료/출처/해설 생성 보강 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/23_도메인_용어사전.md`, `qtai-server/02_ERD_문서.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/service-ai/**`, `qtai-server/service-bible/**`, `qtai-server/admin-server/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

AI 해설 생성 시 `service-ai`의 AI 도메인이 소유한 내부 주석 자료를 verseIds 기준으로 조회해 프롬프트에 포함한다. 생성 산출물에는 주석 출처, 라이선스, 사용한 material id와 verse range를 남기고, 관리자 승인 시 해설과 용어가 함께 게시/숨김 처리되도록 보강한다.

이번 작업에서 `commentary_sources`, `commentary_materials`, `commentary_material_verses`는 사용자 study content가 아니라 AI 생성용 내부 지식/근거 데이터로 본다. 따라서 `service-bible study` 내부 API나 study RestClient adapter는 새로 열지 않는다.

## 범위

- `admin-server` migration에 commentary source/material/verse mapping 테이블을 추가한다.
  - `commentary_*` 데이터의 도메인 소유권은 `service-ai` AI 도메인으로 보되, Flyway schema 파일은 `doc/admin-server-sync-rules.md` 기준에 따라 `admin-server` 단독 소유로 둔다.
- `service-ai.domain.ai.internal`에 commentary Entity, Repository, 조회 Service를 추가한다.
- `ExplanationGenerationJobHandler`에 내부 commentary 조회를 연결하고 prompt와 `sourceMetadata`를 확장한다.
- `AiAssetReviewService`가 `payloadJson.glossaryTerms[]`를 승인 publish하고 hide 시 해설과 용어를 모두 숨기도록 service-ai/admin-server 양쪽을 맞춘다.
- service-ai가 기존 glossary UseCase를 실제 호출할 수 있도록 service-bible에 glossary publish/hide SYSTEM_BATCH endpoint를 추가한다. 이는 commentary 조회 API가 아니라 승인 용어 노출본 반영 경로다.
- 운영 seed는 승인된 `refer.jsonl` 또는 확정 라이선스/attribution 문구가 저장소에서 확인될 때만 포함한다.
- 구현 결과와 seed 차단 여부를 report에 남긴다.

## 제외 범위

- `service-bible study` commentary 조회 API 추가.
- `service-ai`에서 study commentary RestClient adapter 추가.
- 사용자 API에 주석 원문 직접 노출.
- F-15 Q&A 구현.
- admin-web 화면 변경.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/Commentary*.java` | AI 내부 주석 출처/자료/매핑 Entity, Repository, 조회 Service |
| Create | `qtai-server/admin-server/src/main/resources/db/migration/V*.sql` | commentary 테이블 생성 및 승인된 경우 seed |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | commentary 조회, prompt 포함, sourceMetadata 확장 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AiAssetReviewService.java` | glossary publish/hide 연결 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/client/study/VerseExplanationRestClientAdapter.java` | glossary publish/hide SYSTEM_BATCH 호출 추가 |
| Create | `qtai-server/service-bible/src/main/java/com/qtai/domain/study/web/GlossaryTermInternalController.java` | 승인 glossary publish/hide SYSTEM_BATCH 수신 endpoint |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiAssetReviewService.java` | admin-server 승인 런타임 동일 보강 |
| Test | `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/**` | 생성, commentary 조회, 승인 publish/hide 검증 |
| Test | `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/client/study/VerseExplanationRestClientAdapterTest.java` | glossary publish/hide HTTP 매핑 검증 |
| Test | `qtai-server/service-bible/src/test/java/com/qtai/bible/GlossaryTermInternalApiTest.java` | glossary 내부 endpoint SYSTEM_BATCH 보안 경계 검증 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java` | admin-server 승인 publish/hide 회귀 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_ai-commentary-source-publish_report.md` | 구현/검증/후속 TODO 보고 |

## 구현 순서

1. 현재 브랜치와 git 상태를 확인하고, `feature/ai-commentary-source-publish`에서 작업한다.
2. migration 번호를 확인한 뒤 admin-server Flyway migration에 commentary 3개 테이블을 추가한다.
3. 저장소에 승인된 `refer.jsonl`과 확정 license/attribution/copyright 문구가 있는지 확인한다. 없으면 운영 seed는 넣지 않는다.
4. `service-ai.domain.ai.internal`에 commentary source/material/material-verse Entity와 Repository를 추가한다.
5. verseIds 기준 활성 source/material 조회 Service를 구현한다. source/license/copyright는 source 단위로 중복 제거하고 material은 prompt용 DTO로 정리한다.
6. `ExplanationGenerationJobHandler` 생성자에 commentary 조회 Service를 주입하고, 기존 QT passage/bible verses 흐름 뒤에 commentary 조회를 추가한다.
7. user prompt에 `Commentary materials` 섹션을 추가하되, payload에 prompt 전문이나 provider raw response를 저장하지 않는다.
8. `sourceMetadata`에 `commentarySource`, `sourceName`, `licenseLabel`, `copyrightNotice`, `commentaryMaterialIds`, `commentaryVerseRange`를 추가한다.
9. service-ai `AiAssetReviewService`에 `PublishApprovedGlossaryTermsUseCase`, `HidePublishedGlossaryTermsUseCase`를 주입하고, approve/hide 시 기존 verse explanation 처리와 함께 호출한다.
10. service-ai의 study RestClient adapter와 service-bible의 SYSTEM_BATCH glossary endpoint를 연결한다.
11. admin-server `AiAssetReviewService`에도 동일 변경을 반영한다.
12. service-ai/admin-server 테스트를 보강하고 검증 명령을 실행한다.
13. report를 작성하고 변경사항을 검토한 뒤 Conventional Commits 형식으로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `service-ai/.../ExplanationGenerationJobHandlerTest.java` | commentary material 내부 조회, prompt 포함, `sourceMetadata` 확장 |
| `service-ai/.../CommentaryMaterialServiceTest.java` | verseIds 기준 조회, source/license 중복 제거, 비활성 source/material 제외 |
| `service-ai/.../AiAssetReviewServiceTest.java` | approve 시 해설과 glossary publish, hide 시 둘 다 hide |
| `service-ai/.../VerseExplanationRestClientAdapterTest.java` | glossary publish/hide HTTP 매핑과 외부 오류 변환 |
| `admin-server/.../AiAssetReviewServiceTest.java` | admin-server 복사본 approve/hide 동일 회귀 |
| `service-bible/.../GlossaryTermServiceTest.java` | 기존 glossary publish/hide 회귀 유지 |
| `service-bible/.../GlossaryTermInternalApiTest.java` | SYSTEM_BATCH 전용 glossary 내부 endpoint 보안 경계 |

## 수용 기준

- [ ] AI 생성 프롬프트에 service-ai 내부 commentary material excerpt가 포함된다.
- [ ] AI asset payload `sourceMetadata`에 주석 출처/라이선스/사용 material id/range가 저장된다.
- [ ] 관리자 승인 시 `verse_explanations`가 기존처럼 생성/교체된다.
- [ ] 관리자 승인 시 `payloadJson.glossaryTerms[]`가 `glossary_terms`로 생성/교체된다.
- [ ] 관리자 hide 시 `verse_explanations`, `glossary_terms`가 모두 숨김 처리된다.
- [ ] `service-bible study`에는 commentary 조회 API가 추가되지 않는다.
- [ ] 사용자 API에는 주석 원문이 노출되지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- migration, Entity, generation handler, review service, 테스트가 같은 payload 계약을 공유해 순차 확인이 안전하다.
- service-ai와 admin-server 복사본을 같은 기준으로 맞춰야 하므로 메인 agent가 직접 통합하는 편이 충돌과 누락을 줄인다.

### 위임 가능 작업

| Worker | 역할 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow 작성, 구현, 검증, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
.\qtai-server\gradlew.bat -p qtai-server :service-ai:test --tests "*AiAssetReviewServiceTest" --tests "*ExplanationGenerationJobHandlerTest" --tests "*VerseExplanationRestClientAdapterTest" --tests "*CommentaryMaterialServiceTest"
.\qtai-server\gradlew.bat -p qtai-server :service-bible:test --tests "*GlossaryTermServiceTest" --tests "*GlossaryTermInternalApiTest"
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AiAssetReviewServiceTest"
.\qtai-server\gradlew.bat -p qtai-server :service-ai:build
.\qtai-server\gradlew.bat -p qtai-server :admin-server:build
git diff --check
git status --short
```

## 후속 작업으로 남길 항목

- 주석 자료를 사용자 화면에 직접 노출하는 요구가 생기면, `service-ai` 내부 자료에서 `study/public content`로 승격하는 별도 TODO를 만들고 API/권한/저작권 노출 정책을 다시 검토한다.
- 승인된 운영 `refer.jsonl` 또는 라이선스 문구가 저장소에 없으면 seed 추가는 별도 자료 확정 후 후속 작업으로 남긴다.
