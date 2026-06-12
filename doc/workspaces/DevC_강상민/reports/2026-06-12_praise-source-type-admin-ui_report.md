# Report - 2026-06-12 찬양 등록 출처 선택 제거

## 요약

관리자 `찬양 큐레이션` 등록 모달에서 `디바이스(DEVICE)`를 선택할 수 있는 것처럼 보이던 UI를 제거했다. 서버의 관리자 등록 경로는 항상 `sourceType=CURATED`로 저장하므로, 관리자 화면에서 `DEVICE` 선택지를 노출하면 실제 동작과 맞지 않아 운영자가 오해할 수 있다.

## 변경 내용

- `admin-web/src/pages/PraiseSongsPage.tsx`
  - `SOURCE_OPTIONS`를 제거했다.
  - 등록 폼 전용 `CreatePraiseSongFormValues`를 제거하고 `CreatePraiseSongRequest`를 직접 사용하도록 되돌렸다.
  - submit 시 `sourceType`을 제거하는 분기 없이 등록 payload를 그대로 보낸다.
  - 등록 모달의 출처 셀렉트를 읽기 전용 `Input`으로 변경했다.
  - 출처는 `큐레이션(CURATED)` 고정값으로 표시한다.

## 유지한 동작

- 목록의 `출처` 컬럼은 유지한다.
- 서버 등록 payload는 `title`, `artist`, `licenseNote`, `status`만 전송한다.
- `DEVICE`는 사용자 기기 곡 저장 흐름의 의미로 남기고, 관리자 등록 흐름에서는 다루지 않는다.

## 검증 명령

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

## 브라우저 확인

- URL: `http://localhost:5173/praise-songs`
- dev 계정 `admin / admin1234`로 재로그인
- `곡 등록` 모달 오픈
- 출처 입력이 `큐레이션(CURATED)` 읽기 전용 값으로 표시됨
- `디바이스(DEVICE)` 선택지는 표시되지 않음

## 결론

관리자 찬양 등록 화면은 이제 실제 서버 동작과 동일하게 `CURATED` 전용 화면으로 보인다. `DEVICE`는 관리자 등록 대상이 아니라 사용자 기기 곡 저장 흐름으로 분리된다.
