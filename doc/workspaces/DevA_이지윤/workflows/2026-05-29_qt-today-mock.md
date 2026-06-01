# feature/qt-today-mock 작업 정리 workflow

## 1. 목적

`feature/qt-today-mock` 브랜치와 PR #97의 실제 변경 범위를 정리한다.

초기 표에는 "오늘 QT 통합 API 목업 GET /api/v1/qt/today"로 기록되어 있었으나, GitHub PR 메타데이터와 변경 파일을 확인한 결과 PR #97은 `note` 도메인 작업이 중심이었다. 따라서 이 문서는 브랜치명과 실제 PR 내용의 차이를 남기고, 리포트 브랜치에서 후속 정리를 위한 기준 자료로 사용한다.

## 2. 기준 정보

| 항목 | 내용 |
| --- | --- |
| 대상 브랜치 | `feature/qt-today-mock` |
| 연결 PR | #97 `feat(note): 오늘 목업 + 리포트추가` |
| PR 상태 | merged |
| merge 시각 | 2026-05-27 12:37 KST |
| base | `dev` |
| head | `feature/qt-today-mock` |
| 작업자 문서 경로 | `doc/workspaces/DevA_이지윤/` |

## 3. 확인 절차

1. GitHub PR #97의 title, head/base, merged_at, commit 목록을 확인한다.
2. PR #97의 changed files를 확인해 실제 도메인 변경 범위를 분류한다.
3. 표에 기록된 "GET /api/v1/qt/today" 작업과 실제 diff를 비교한다.
4. 최신 `dev` 병합 후 남은 구형 Note 계약(`NoteSaveResponse`) 여부를 확인한다.
5. 리포트 전용 브랜치 `feature/report`에 workflow/report만 추가한다.

## 4. 실제 변경 범위 분류

| 분류 | 확인 내용 |
| --- | --- |
| Note 도메인 | `NoteService`, `NoteController`, Note UseCase/DTO, `NoteRepository`, `NoteVerseRepository` 변경 |
| 테스트 | `NoteServiceTest`, `NoteControllerTest`, `NoteRepositoryIntegrationTest`, `NoteVerseRepositoryTest` 보강 |
| API 계약 | `qtai-server/apis/api-v1/openapi.yaml`에 Note API 관련 변경 포함 |
| 문서 | `DevA_이지윤` note domain completion 계열 workflow/report 포함 |
| Bible 보조 변경 | `GetBibleVerseUseCase`, `BibleService`, `BibleRepository` 일부 보강 |

## 5. 정합성 판단

- PR #97은 브랜치명은 `feature/qt-today-mock`이지만 실제 구현 범위는 `note` 도메인 완성 작업에 가깝다.
- "GET /api/v1/qt/today" 실제 구현으로 보기에는 `qt` 도메인 Controller/Service 중심 변경이 부족하다.
- 오늘 QT 조회 실제 구현은 별도 PR #126 `feat(qt): GetTodayQtUseCase 구현` 쪽이 더 직접적이다.
- 따라서 일정표나 작업 정리표에는 #97을 "오늘 QT 통합 API 목업"으로 단독 기록하기보다 "Note 도메인 completion / 오늘 목업 관련 보강"으로 재분류하는 것이 안전하다.

## 6. 후속 작업 기준

- 리포트 모음 브랜치에서는 코드 변경 없이 문서만 추가한다.
- 이미 merge된 PR의 실제 코드 변경은 되돌리지 않는다.
- PR 제목/브랜치명과 실제 diff가 다른 경우 리포트에 차이를 명시한다.
- `NoteSaveResponse`는 최신 dev 계약에서 삭제되었으므로 새 문서나 후속 작업에서 사용하지 않는다.
