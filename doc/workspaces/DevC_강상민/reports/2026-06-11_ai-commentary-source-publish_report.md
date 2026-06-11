# 2026-06-11 AI 내부 주석 자료 기반 해설 생성 보강 리포트

## 요약

`feature/ai-commentary-source-publish` 브랜치에서 P1 AI 주석 자료/출처/해설 생성 보강을 진행했다. 이번 작업에서 `commentary_sources`, `commentary_materials`, `commentary_material_verses`는 `service-ai` AI 도메인 소유의 내부 생성 자료로 두고, 사용자 study content API에는 주석 원문을 노출하지 않았다.

Flyway schema 변경 파일은 `doc/admin-server-sync-rules.md` 기준에 따라 `admin-server`에만 둔다. `service-ai`에는 AI 내부 Entity/Repository/Service와 런타임 조회 흐름만 둔다.

## 변경 내용

| 영역 | 내용 |
| --- | --- |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-11_ai-commentary-source-publish.md` 작성 |
| DB | `admin-server`에 AI commentary source/material/mapping migration 추가 |
| service-ai | AI 내부 commentary Entity/Repository/Service 추가, verseIds 기준 prompt context 조회 |
| 생성 흐름 | `ExplanationGenerationJobHandler`가 성경 본문과 commentary excerpt를 함께 prompt에 넣고 `sourceMetadata`에 출처/라이선스/material id/range 저장 |
| 승인 흐름 | service-ai/admin-server `AiAssetReviewService`가 approve 시 해설과 glossary를 함께 publish하고 hide 시 둘 다 숨김 |
| service-bible | 기존 glossary publish/hide UseCase를 service-ai가 호출할 수 있도록 SYSTEM_BATCH 내부 endpoint 추가 |
| 테스트 | 생성 handler, commentary 조회 service, service-ai/admin-server 승인 publish/hide 회귀 테스트 보강 |

## 운영 seed 판단

저장소에서 승인된 `refer.jsonl` 파일을 찾지 못했다. 문서에는 Tyndale Open Study Notes가 예시로 언급되어 있지만, 실제 원문 파일과 확정 license/attribution/copyright 문구가 저장소에 없어 운영 seed는 추가하지 않았다.

## 후속 TODO

- 승인된 운영 `refer.jsonl` 또는 확정 출처/라이선스 문구가 제공되면 별도 작업으로 seed를 추가한다.
- 주석 자료를 사용자 화면에 직접 노출하는 요구가 생기면, `service-ai` 내부 생성 자료에서 `study/public content`로 승격하는 별도 TODO를 만들고 API/권한/저작권 노출 정책을 다시 검토한다.
- `commentary_*` 테이블은 service-ai AI 도메인 내부 데이터지만, schema/Flyway 파일은 `admin-server` 단독 소유 규칙을 따른다. service-ai 쪽 migration 파일은 두지 않는다.
- 승인 publish는 원격 study publish를 해설, glossary 순으로 호출한다. 각 publish는 `aiAssetId`/verse 기준 교체형·재실행 가능 계약으로 보고, 후속 호출 실패 시 로컬 트랜잭션 롤백 후 재시도로 두 publish를 다시 실행한다.
- 이번 P1의 glossary publish/hide는 `BIBLE_VERSE` 대상 AI 해설 asset에 한정한다. `QT_PASSAGE` asset의 glossaryTerms 직접 publish는 별도 요구가 생기면 대상 매핑 정책을 확정한 뒤 추가한다.

## 검증

```powershell
.\qtai-server\gradlew.bat -p qtai-server :service-ai:test --tests "*AiAssetReviewServiceTest" --tests "*ExplanationGenerationJobHandlerTest" --tests "*CommentaryMaterialServiceTest" --tests "*VerseExplanationRestClientAdapterTest"
.\qtai-server\gradlew.bat -p qtai-server :service-bible:test --tests "*GlossaryTermServiceTest" --tests "*GlossaryTermInternalApiTest"
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AiAssetReviewServiceTest"
.\qtai-server\gradlew.bat -p qtai-server :service-ai:build
.\qtai-server\gradlew.bat -p qtai-server :admin-server:build
git diff --check
```

모두 통과했다. `git diff --check`에서는 CRLF 변환 경고만 출력되었고 whitespace 오류는 없었다.
