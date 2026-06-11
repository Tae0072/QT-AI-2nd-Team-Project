# Workflow - 2026-06-11 ai-commentary-aquifer-seed

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-commentary-aquifer-seed` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-08 |
| 트리거 | Aquifer Open Study Notes 영어 전체 자료를 AI 해설 생성용 내부 commentary seed로 추가 |
| 기준 문서 | `doc/admin-server-sync-rules.md`, `doc/프로젝트문서/07_요구사항_정의서.md`, `doc/프로젝트문서/03_아키텍처_정의서.md`, `doc/프로젝트문서/18_코드_품질_게이트.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/admin-server/src/main/resources/db/migration/**`, `data/aquifer-open-study-notes/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

BibleAquifer `AquiferOpenStudyNotes` GitHub release `v2026-06-09`의 `English.zip`을 기준으로 영어 전체 Aquifer Open Study Notes를 `commentary_sources`, `commentary_materials`, `commentary_material_verses` seed로 추가한다.

이 자료는 사용자 study content가 아니라 `service-ai` AI 해설 생성 프롬프트에 들어가는 내부 참고자료다. 런타임 공개 API와 `service-bible study` 계약은 변경하지 않는다.

## 범위

- `admin-server` Flyway migration에 전체 SQL seed를 추가한다.
- 원본 release zip/json을 커밋하지 않고, 재생성 가능한 seed 생성 스크립트를 추가한다.
- source/license/copyright/attribution 메타데이터를 seed에 포함한다.
- material HTML과 HTML에서 추출한 평문, release/version/reviewLevel 등 기술 메타데이터를 seed에 포함한다.
- material과 `bible_verses` 매핑은 고정 ID가 아니라 `bible_books.code`, chapter, verse 조회 기반 SQL로 생성한다.
- workflow 실행 결과와 검증 결과를 report로 남긴다.
- 변경사항을 한 커밋으로 묶는다.

## 제외 범위

- `service-bible` commentary 조회 API 추가.
- 사용자 API 또는 admin API 계약 변경.
- AI 생성/승인 런타임 로직 변경.
- 원본 zip/json 파일 커밋.
- Aquifer 영어 외 언어 seed 추가.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `data/aquifer-open-study-notes/generate_aquifer_commentary_seed.py` | Aquifer release 다운로드, SHA-256 검증, JSON 파싱, SQL 생성 |
| Create | `qtai-server/admin-server/src/main/resources/db/migration/V34__seed_aquifer_open_study_notes.sql` | Aquifer 영어 전체 commentary source/material/verse mapping seed |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_ai-commentary-aquifer-seed_report.md` | 구현, 검증, 라이선스, 후속 과제 보고 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-11_ai-commentary-aquifer-seed.md` | 작업단위 명세 |

## 구현 순서

1. `dev` 기준 브랜치 `feature/ai-commentary-aquifer-seed`에서 작업한다.
2. `v2026-06-09` `English.zip`의 release URL과 SHA-256을 seed 생성 스크립트에 고정한다.
3. 스크립트는 `eng/metadata.json`의 title/license/copyright holder/date를 검증하고 source seed를 만든다.
4. 스크립트는 `eng/json/*.content.json`의 `content_id`, `title`, `content`, `review_level`, `version`, `index_reference`, `associations.passage`를 읽어 material seed를 만든다.
5. HTML은 원문 그대로 `content_html`에 저장하고, 표준 HTML parser로 태그를 제거한 평문을 `content_text`에 저장한다.
6. `start_ref_usfm`, `end_ref_usfm`를 파싱해 book/chapter/verse range와 `refs`를 만든다. 매핑 불가 passage는 실패 처리한다.
7. `commentary_material_verses` seed는 `bible_books`와 `bible_verses`를 조인하는 `INSERT ... SELECT` 형태로 생성한다.
8. 생성 스크립트는 source/material/mapping 건수, SHA-256, skipped count, 중복 external id, 금지 번역본 키워드 스캔 결과를 출력한다.
9. 생성된 SQL을 정적 검토하고 서버 build/test를 실행한다.
10. report를 작성하고 Conventional Commits 형식으로 커밋한다.

## 수용 기준

- [ ] `commentary_sources`에 `AQUIFER_OPEN_STUDY_NOTES` source 1건이 seed된다.
- [ ] 영어 전체 Aquifer material이 `commentary_materials` seed로 생성된다.
- [ ] 각 material의 verse mapping이 `bible_books`/`bible_verses` 조회 기반으로 생성된다.
- [ ] 원본 zip/json 파일은 커밋되지 않는다.
- [ ] 공개 API, `service-bible study` 계약, AI 런타임 코드는 변경되지 않는다.
- [ ] report에 CC BY-SA 4.0 출처/저작권/ShareAlike 주의와 사용자 화면 직접 노출 시 별도 study/public content 승격 필요성을 남긴다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 핵심 변경이 seed 생성 스크립트와 그 결과 SQL에 집중되어 있어 생성 규칙과 결과를 한 흐름에서 검토하는 편이 안전하다.
- 대형 SQL 파일은 생성 스크립트와 일관성이 중요하므로 병렬 편집 시 충돌 가능성이 크다.

### 위임 가능 작업

| Worker | 역할 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow 작성, seed 생성, 검증, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
python data/aquifer-open-study-notes/generate_aquifer_commentary_seed.py --check
git diff --check
.\qtai-server\gradlew.bat -p qtai-server :admin-server:build
.\qtai-server\gradlew.bat -p qtai-server :service-ai:test --tests "*CommentaryMaterialServiceTest" --tests "*ExplanationGenerationJobHandlerTest"
.\qtai-server\gradlew.bat -p qtai-server :service-ai:build
git status --short
```

가능하면 MySQL 8.0 로컬 또는 컨테이너에서 Flyway migration dry-run을 수행한다. 환경이 없으면 report에 미실행 사유와 대체 검증을 남긴다.

## 후속 작업으로 남길 항목

- 주석 자료를 사용자 화면에 직접 노출하는 요구가 생기면 AI 내부 지식 데이터에서 `study/public content`로 승격하는 별도 작업을 만든다.
- Aquifer 영어 외 언어 seed는 별도 요구와 라이선스/출처 검토 후 진행한다.
