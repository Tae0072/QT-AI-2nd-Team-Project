# 2026-06-11 Aquifer 영어 주석 자료 Seed 추가 리포트

## 요약

`feature/ai-commentary-aquifer-seed` 브랜치에서 Aquifer Open Study Notes 영어 전체 자료를 AI 해설 생성용 내부 commentary seed로 추가했다.

이번 변경은 DB seed와 재생성 스크립트, workflow/report 문서만 포함한다. 공개 API, `service-bible study` 계약, AI 생성/승인 런타임 코드는 변경하지 않았다.

## 변경 내용

| 영역 | 내용 |
| --- | --- |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-11_ai-commentary-aquifer-seed.md` 작성 |
| seed generator | `data/aquifer-open-study-notes/generate_aquifer_commentary_seed.py` 추가 |
| DB seed | `admin-server` Flyway `V34__seed_aquifer_open_study_notes.sql` 추가 |

## Seed 원본과 라이선스

- 원본: BibleAquifer `AquiferOpenStudyNotes` GitHub release `v2026-06-09` `English.zip`
- SHA-256: `14b5df7b81e8f2cdeb7202430beb9d629771d0ac520badea06cacfa9383201b3`
- Source key: `AQUIFER_OPEN_STUDY_NOTES`
- Source name: `Aquifer Open Study Notes`
- Product: `Tyndale Open Study Notes`
- License: `CC BY-SA 4.0`
- Copyright notice: `Tyndale Open Study Notes © 2019 Tyndale House Publishers`
- Attribution: Aquifer Open Study Notes는 Mission Mutual이 Tyndale Open Study Notes를 각색한 자료이며 CC BY-SA 4.0으로 제공된다는 문구를 source metadata에 저장했다.

CC BY-SA 4.0 자료이므로 사용자에게 해설/출처 메타데이터가 노출되는 흐름에서는 출처와 라이선스 표시를 유지해야 한다. 원문 주석 자료는 이번 범위에서 사용자 API로 직접 노출하지 않는다.

## 생성 결과

| 항목 | 값 |
| --- | ---: |
| source count | 1 |
| material count | 16,923 |
| material-verse mapping count | 59,119 |
| multi-passage material count | 8 |
| skipped count | 0 |
| forbidden keyword matches | 0 |
| duplicate material-verse mapping | 0 |
| max display_order | 7 |
| max content_text length | 4,049 |
| max content_html length | 4,876 |
| max refs length | 40 |
| max title length | 24 |

`commentary_material_verses`는 고정 `bible_verse_id`를 하드코딩하지 않고 `bible_books.code`, chapter, verse 기반 `INSERT ... SELECT`로 연결한다.

## 변경 규모 예외

사용자가 “영어 전체”와 “SQL migration 전체”를 선택했기 때문에 `V34__seed_aquifer_open_study_notes.sql`은 약 26.2MB, 77,465 lines다. 팀 PR 권장 기준인 10 files/500 changed lines를 초과하지만, 전체 운영 seed를 migration으로 포함하는 요구를 만족하기 위한 예외다.

원본 `English.zip`과 JSON 파일은 커밋하지 않았다. 재생성이 필요하면 generator가 release asset을 임시 디렉터리에 내려받아 SHA-256을 검증한 뒤 SQL을 만든다.

## 검증

```powershell
python data\aquifer-open-study-notes\generate_aquifer_commentary_seed.py --check
```

결과: 통과. source 1건, material 16,923건, mapping 59,119건, skipped 0건, forbidden keyword 0건.

```powershell
git diff --check
```

결과: 통과.

```powershell
.\qtai-server\gradlew.bat -p qtai-server :service-ai:test --tests "*CommentaryMaterialServiceTest" --tests "*ExplanationGenerationJobHandlerTest"
```

결과: `BUILD SUCCESSFUL`.

```powershell
.\qtai-server\gradlew.bat -p qtai-server :admin-server:build
```

결과: `BUILD SUCCESSFUL`.

```powershell
.\qtai-server\gradlew.bat -p qtai-server :service-ai:build
```

결과: `BUILD SUCCESSFUL`.

## MySQL Dry-run

MySQL 8.0 Docker 컨테이너에서 focused migration dry-run을 수행했다. 전체 애플리케이션 schema 대신 V34 검증에 필요한 최소 `bible_books`, `bible_verses` schema를 만든 뒤 `V7`, `V23`, `V33`, `V34`를 순서대로 적용했다.

결과: 통과.

| 항목 | 값 |
| --- | ---: |
| source_count | 1 |
| material_count | 16,923 |
| mapping_count | 59,119 |
| missing_material_mappings | 0 |
| max_display_order | 7 |

## 후속 TODO

- 주석 자료는 이번 P1에서 AI 내부 지식/근거 데이터로 유지한다. 사용자 화면 직접 노출 요구가 생기면 `study/public content`로 승격하는 별도 TODO가 필요하다.
- MySQL 8.0 Docker 또는 로컬 DB가 실행 가능한 환경에서 `V7`, `V23`, `V33`, `V34` focused migration dry-run을 추가로 수행하면 좋다.
