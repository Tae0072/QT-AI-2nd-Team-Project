# Workflow - 2026-06-12 praise-create-payload-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-05 / F-09 |
| 트리거 | 관리자 찬양 곡 등록 시 `요청 본문을 해석할 수 없습니다.` 오류 발생 |
| 기준 문서 | `admin-web/src/api/praiseSongs.ts`, `admin-web/src/pages/PraiseSongsPage.tsx`, `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/api/dto/PraiseCreateRequest.java` |
| 해당 경로 | `admin-web/src/api/praiseSongs.ts`, `admin-web/src/pages/PraiseSongsPage.tsx`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

관리자 찬양 곡 등록 요청 payload를 서버 등록 계약과 맞춘다. 현재 프런트엔드는 `sourceType`을 요청 본문에 포함하지만 서버 등록 DTO는 관리자 등록 경로에서 `sourceType`을 받지 않고 항상 `CURATED`로 저장한다. 이 불일치 때문에 등록 요청이 `HttpMessageNotReadable`로 실패한다.

`sourceType`은 화면에서 “큐레이션(CURATED)” 안내로만 유지하고 서버에는 보내지 않는다. `status`는 별도 작업에서 서버 계약에 추가했으므로 등록 payload에 포함한다.

## 범위

- create API payload 타입에서 `sourceType`을 제거한다.
- 등록 form 값 타입은 화면 전용으로 분리해 `sourceType` 선택 표시를 유지한다.
- submit 시 `sourceType`을 제외하고 `title`, `artist`, `licenseNote`, `status`만 전송한다.
- 타입 검사와 브라우저 등록 흐름을 확인한다.
- report를 작성한다.

## 제외 범위

- 서버 DB schema 변경
- `sourceType=DEVICE` 관리자 등록 허용
- 가사, 음원, 외부 URL 저장
- 관리자 수정 화면 상태 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/api/praiseSongs.ts` | create request 계약에서 `sourceType` 제거 |
| Modify | `admin-web/src/pages/PraiseSongsPage.tsx` | form values와 API payload 분리 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_praise-create-payload-contract_report.md` | 원인과 검증 결과 기록 |

## 구현 순서

1. `CreatePraiseSongRequest`에서 `sourceType` 필드를 제거한다.
2. `PraiseSongsPage`에 `CreatePraiseSongFormValues` 타입을 추가한다.
3. `useForm`과 `validateFields`는 form values 타입을 사용한다.
4. submit 시 `sourceType`을 제외한 payload를 만들어 `createPraiseSong`에 전달한다.
5. `admin-web` typecheck를 실행한다.
6. 서버 재기동 후 브라우저에서 등록 요청을 확인한다.
7. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | 프런트 payload 타입 정합성 변경으로 TypeScript 검사와 브라우저 수동 검증 수행 |

## 수용 기준

- [ ] 등록 요청 본문에 `sourceType`이 포함되지 않는다.
- [ ] 등록 요청 본문에 `status`는 포함된다.
- [ ] 곡 등록 시 `요청 본문을 해석할 수 없습니다.` 오류가 재발하지 않는다.
- [ ] 등록 성공 후 목록이 갱신된다.
- [ ] report가 작성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 프런트엔드 등록 payload 계약에 한정된다.
- 브라우저 재현과 코드 수정, 서버 재기동 검증이 같은 흐름에서 순차적으로 이루어져야 한다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 코드 수정, 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
npm.cmd --prefix admin-web run typecheck
```

브라우저 검증:

- `http://localhost:5173/praise-songs`
- `곡 등록` 모달 열기
- QA용 곡명/아티스트/라이선스 메모 입력
- 등록 성공 메시지 및 목록 갱신 확인

## 후속 작업으로 남길 항목

- 등록 모달에서 `sourceType`을 선택 컴포넌트 대신 읽기 전용 표시로 바꿀지 검토
