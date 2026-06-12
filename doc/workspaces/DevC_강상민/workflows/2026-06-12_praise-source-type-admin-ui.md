# Workflow - 2026-06-12 praise-source-type-admin-ui

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-05 / F-09 |
| 트리거 | 관리자 찬양 등록 모달에서 `디바이스(DEVICE)` 선택지가 보여 운영자가 실제 저장 가능하다고 오해할 수 있음 |
| 기준 문서 | `AGENTS.md`, `admin-web/src/pages/PraiseSongsPage.tsx`, `admin-web/src/api/praiseSongs.ts` |
| 해당 경로 | `admin-web/src/pages/PraiseSongsPage.tsx`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

관리자 `찬양 큐레이션` 화면의 등록 모달에서 `DEVICE` 선택지를 제거한다. 관리자 등록 API는 서버에서 항상 `sourceType=CURATED`로 저장하므로, 폼에서 `DEVICE`를 선택할 수 있게 보이는 것은 실제 동작과 맞지 않고 QA/운영 혼선을 만든다.

## 범위

- 등록 모달에서 출처 선택 셀렉트를 제거한다.
- 관리자 등록은 `큐레이션(CURATED)` 고정임을 읽기 전용 입력으로 표시한다.
- `sourceType` 폼 전용 타입과 submit 시 제거 로직을 삭제한다.
- TypeScript 타입체크와 브라우저 확인을 수행한다.
- 작업 리포트를 작성한다.

## 제외 범위

- 서버 API 변경
- 사용자 `DEVICE` 저장 흐름 변경
- 음원 업로드/재생 소스 연결
- DB schema 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/pages/PraiseSongsPage.tsx` | 등록 모달 출처 UI를 `CURATED` 고정 표시로 변경 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_praise-source-type-admin-ui_report.md` | 변경 내용과 검증 결과 기록 |

## 구현 순서

1. `SOURCE_OPTIONS`와 `CreatePraiseSongFormValues`를 제거한다.
2. 등록 form 타입을 `CreatePraiseSongRequest`로 되돌린다.
3. submit 시 `sourceType` 제거 로직 없이 `createPraiseSong(values)`를 호출한다.
4. 등록 모달의 출처 셀렉트를 읽기 전용 `Input`으로 바꾼다.
5. `admin-web` typecheck를 실행한다.
6. 브라우저에서 등록 모달에 `디바이스(DEVICE)` 선택지가 사라졌는지 확인한다.
7. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | UI 혼선 제거 작업이므로 TypeScript와 브라우저 수동 확인으로 검증 |

## 수용 기준

- [ ] 등록 모달에서 `디바이스(DEVICE)`를 선택할 수 없다.
- [ ] 등록 모달은 `큐레이션(CURATED)` 고정임을 표시한다.
- [ ] 등록 요청 payload에는 기존처럼 `title`, `artist`, `licenseNote`, `status`만 포함된다.
- [ ] `npm.cmd --prefix admin-web run typecheck`가 성공한다.
- [ ] report가 작성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 화면의 UI/타입 정리에 집중되어 있다.
- 구현, 브라우저 확인, 리포트 작성이 같은 맥락에서 순차적으로 이루어져야 한다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 전체 작업을 직접 수행하는 편이 충돌과 재작업을 줄인다.

## 검증 계획

```powershell
npm.cmd --prefix admin-web run typecheck
```

브라우저 확인:

- `http://localhost:5173/praise-songs`
- `곡 등록` 모달 열기
- 출처가 `큐레이션(CURATED)` 고정 표시인지 확인
- `디바이스(DEVICE)` 선택지가 없는지 확인

## 후속 작업으로 넘길 항목

- 실제 음원/외부 재생 소스 연결은 별도 요구사항으로 분리한다.
