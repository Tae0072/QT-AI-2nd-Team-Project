# Workflow - 2026-06-12 admin-server-commentary-generation-sync

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-03 |
| 트리거 | AI 산출물 생성 시 주석자료 메타데이터가 `service-ai`에는 반영되어 있으나 `admin-server` 복사본에는 빠져 있는 차이 확인 |
| 기준 문서 | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java`, `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` |
| 해당 경로 | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`admin-server` 복사본의 AI 해설 생성 경로를 `service-ai`와 동기화한다. 어떤 모듈에서 EXPLANATION 생성 job이 실행되더라도 주석자료가 프롬프트 입력으로 사용되고, 산출물 상세의 `payloadJson.sourceMetadata`에서 주석 출처 메타데이터를 확인할 수 있어야 한다.

## 범위

- `admin-server`에 주석자료 source/material/mapping entity와 repository, read service를 추가한다.
- `admin-server`의 `ExplanationGenerationJobHandler`가 `CommentaryMaterialService`를 주입받도록 한다.
- QT_PASSAGE/BIBLE_VERSE 입력 생성 시 verseIds 기준 주석자료 컨텍스트를 조회한다.
- LLM user prompt에 주석자료 excerpt를 포함한다.
- payload `sourceMetadata`에 주석자료 출처/라이선스/material id/range 메타데이터를 포함한다.
- admin-server compile과 bootJar, 컨테이너 재기동으로 검증한다.

## 제외 범위

- 주석자료 관리 UI 추가
- 주석 원문 전체를 산출물 payload에 저장
- service-ai 로직 변경
- DB schema 수동 마이그레이션
- OpenAPI 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/Commentary*.java` | service-ai와 동일한 주석자료 조회 모델/서비스 추가 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | 주석자료 프롬프트 입력과 metadata 저장 동기화 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_admin-server-commentary-generation-sync_report.md` | 원인, 변경 내용, 검증 결과 기록 |

## 구현 순서

1. `service-ai`와 `admin-server`의 `ExplanationGenerationJobHandler` 차이를 비교한다.
2. `admin-server`에 누락된 `CommentaryMaterialContext`, `CommentaryMaterialService`, entity/repository/status enum을 추가한다.
3. `ExplanationGenerationJobHandler` 생성자에 `CommentaryMaterialService`를 추가한다.
4. 입력 생성 단계에서 `commentaryMaterialService.findPromptContextByVerseIds(verseIds)`를 호출한다.
5. prompt와 `sourceMetadata`에 service-ai와 동일한 주석자료 정보를 반영한다.
6. `:admin-server:compileJava`와 `:admin-server:bootJar`를 실행한다.
7. `qtai-admin-server` 컨테이너를 재빌드/재기동하고 health를 확인한다.
8. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | service-ai와 동일 코드 동기화 작업으로 신규 자동 테스트는 추가하지 않고 compile/bootJar/컨테이너 health로 검증 |

## 수용 기준

- [ ] `admin-server`의 해설 생성 핸들러가 주석자료 service를 사용한다.
- [ ] 주석자료가 있으면 user prompt에 excerpt가 포함된다.
- [ ] payload `sourceMetadata`에 `commentarySource`, `sourceName`, `licenseLabel`, `copyrightNotice`, `commentaryMaterialIds`, `commentaryVerseRange`가 포함된다.
- [ ] 주석자료가 없으면 해당 metadata는 null/빈 배열로 저장된다.
- [ ] `:admin-server:compileJava`가 통과한다.
- [ ] `:admin-server:bootJar`가 통과한다.
- [ ] `qtai-admin-server`가 재기동 후 healthy 상태가 된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `admin-server` AI 내부 복사본 동기화에 집중되어 있다.
- service-ai와 정확히 같은 계약을 유지해야 하므로 한 에이전트가 diff를 직접 확인하는 편이 안전하다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 비교, 구현, 컴파일, 컨테이너 재기동, report 작성을 직접 수행한다.

## 검증 계획

```powershell
.\qtai-server\gradlew.bat -p qtai-server :admin-server:compileJava
.\qtai-server\gradlew.bat -p qtai-server :admin-server:bootJar
docker compose up -d --build service-admin
```

추가 확인:

- `docker ps`에서 `qtai-admin-server` health가 `healthy`인지 확인한다.
- 필요 시 `/ai-assets`에서 새 산출물 상세 `Payload JSON.sourceMetadata`를 확인한다.

## 후속 작업으로 남길 항목

- service-ai/admin-server 복사본 중복 구조를 장기적으로 제거할지 검토
- 주석자료 metadata를 Payload JSON raw가 아니라 전용 UI 행으로 보여줄지 검토
