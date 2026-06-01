# feature/qt-today-mock 작업 정리 리포트

## 요약

`feature/qt-today-mock` 브랜치의 PR #97을 재확인했다. 브랜치명과 초기 표의 구현 내용은 "오늘 QT 통합 API 목업 GET /api/v1/qt/today"였지만, 실제 PR diff는 `note` 도메인 API/서비스/테스트 보강이 대부분이었다.

따라서 이 리포트는 해당 브랜치를 "오늘 QT 조회 API 구현 PR"로 확정하지 않고, 실제 변경 범위를 Note 도메인 중심으로 기록한다.

## PR 정보

| 항목 | 내용 |
| --- | --- |
| PR | #97 |
| 제목 | `feat(note): 오늘 목업 + 리포트추가` |
| 브랜치 | `feature/qt-today-mock` |
| base | `dev` |
| 상태 | merged |
| 변경 규모 | 48 files, +3032 / -413 |

## 커밋 목록

| commit | 메시지 |
| --- | --- |
| `08f70bb` | `feat(note): 오늘 목업` |
| `6eea2ed` | `docs(note):  오늘 목업 + 작업 리포트 추가` |
| `cc7f2a6` | `PR 수정` |
| `6f76a72` | `fix(note): resolve review blockers` |
| `57022fb` | `fix(note): narrow duplicate catch scope` |
| `7be2571` | `test(note): cover note update and draft paths` |
| `d149046` | `test(note): cover delete and update negative paths` |

## 실제 변경 내용

### Note 도메인

- Note 생성/수정/삭제/조회 UseCase 시그니처와 DTO 정리
- Note category 조회 API 추가
- `NoteVisibility`, `NoteStatus`, `NoteVerse` 관련 모델 보강
- `NoteService`, `NoteRepository`, `NoteController` 동작 보강
- `NoteQtClient`, `GetQtUseCaseMock`을 통한 QT 연동 목업 보강

### 테스트

- `NoteServiceTest`에 생성/수정/삭제/임시저장 관련 경로 보강
- `NoteControllerTest`에 API 응답/예외 경로 보강
- `NoteRepositoryIntegrationTest`, `NoteVerseRepositoryTest` 추가 또는 보강
- `BibleServiceTest` 일부 보강

### API / 문서

- `qtai-server/apis/api-v1/openapi.yaml` 변경
- `doc/workspaces/DevA_이지윤/reports/`에 note domain completion 계열 리포트 추가
- `doc/workspaces/DevA_이지윤/workflows/2026-05-27_note-domain-completion.md` 추가

## 정합성 확인

| 점검 항목 | 결과 |
| --- | --- |
| 브랜치명과 PR head 일치 | 일치 |
| PR 제목과 실제 diff | 대체로 일치. `note` 중심 |
| 초기 표의 "GET /api/v1/qt/today"와 실제 diff | 불일치 가능성 큼 |
| 실제 오늘 QT 조회 구현 후보 | PR #126 `feat(qt): GetTodayQtUseCase 구현` |
| 최신 Note 계약과 충돌 여부 | 후속 dev 병합에서 `NoteSaveResponse` 제거로 정리됨 |

## 결론

`feature/qt-today-mock`은 이름만 보면 Today QT mock 작업처럼 보이지만, merge된 PR #97의 실질은 Note 도메인 completion 작업이다. 작업표에는 다음처럼 정리하는 것이 안전하다.

```text
feature/qt-today-mock
PR #97 feat(note): 오늘 목업 + 리포트추가
실제 범위: Note 도메인 생성/수정/삭제/조회, 카테고리, 테스트 보강
주의: GET /api/v1/qt/today 구현 PR로 단정하지 않음
```

## 검증

- GitHub PR #97 메타데이터 확인
- PR #97 changed files 확인
- `feature/qt-today-mock` 최신 커밋 로그 확인
- `NoteSaveResponse`는 최신 dev 계약상 사용하지 않는 것으로 확인

## 남은 리스크

- PR #97의 제목에 "오늘 목업" 표현이 있어 일정표와 매핑할 때 혼동 가능성이 있다.
- 실제 `GET /api/v1/qt/today` 구현은 PR #126과 구분해서 기록해야 한다.
- 이 리포트는 문서 정리용이며, 이미 merge된 코드 동작을 변경하지 않는다.
