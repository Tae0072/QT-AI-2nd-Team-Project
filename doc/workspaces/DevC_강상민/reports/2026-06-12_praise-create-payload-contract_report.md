# Report - 2026-06-12 찬양 곡 등록 요청/목록 계약 보정

## 요약

관리자 `찬양 큐레이션` 화면에서 곡 등록을 눌러도 저장되지 않는 문제를 점검했다.

원인은 프런트 등록 요청 payload에 서버 `PraiseCreateRequest`가 받지 않는 `sourceType`이 포함되어 Jackson 역직렬화 단계에서 `요청 본문을 해석할 수 없습니다.` 오류가 발생한 것이다. 관리자 등록 경로의 `sourceType`은 서버에서 항상 `CURATED`로 저장하므로 화면 표시값과 API payload를 분리했다.

등록 성공 후 목록이 `총 2곡`인데 `데이터 없음`으로 보이는 문제도 함께 확인했다. 찬양 목록 API는 Spring `Page` 응답의 현재 페이지 필드를 `number`로 내려주지만, 프런트 공통 페이지 타입은 `page`를 기대한다. `listPraiseSongs` API 레이어에서 Spring `Page`를 관리자 웹 공통 `Page<T>` 형태로 정규화했다.

## 변경 내용

- `admin-web/src/api/praiseSongs.ts`
  - `CreatePraiseSongRequest`에서 `sourceType`을 제거했다.
  - Spring `Page` 응답을 프런트 공통 `Page<T>`로 변환하는 정규화 함수를 추가했다.
  - `number` 또는 `page`를 모두 수용해 `usePagedList`의 현재 페이지 상태가 깨지지 않게 했다.
- `admin-web/src/pages/PraiseSongsPage.tsx`
  - 등록 폼 전용 타입 `CreatePraiseSongFormValues`를 추가했다.
  - 화면의 출처 선택값은 유지하되, submit 시 `sourceType`을 제외하고 서버에 전송한다.

## 확인 결과

- 등록 요청이 더 이상 `요청 본문을 해석할 수 없습니다.`로 실패하지 않는다.
- 브라우저에서 `QA 등록 확인 찬양` 등록 성공 메시지를 확인했다.
- 등록 후 `/praise-songs` 목록에서 다음 2개 곡이 테이블에 표시됨을 확인했다.
  - `QA 등록 확인 찬양`
  - `로컬 샘플 찬양`

## 검증 명령

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.praise.internal.PraiseServiceTest"
```

결과: 성공

```powershell
.\gradlew.bat :admin-server:bootJar
```

결과: 성공

```powershell
docker compose up -d --build service-admin
```

결과: 성공, `service-admin` healthcheck `healthy`

## 결론

관리자에서 곡 등록은 이제 `title`, `artist`, `licenseNote`, `status`만 서버로 전송하고, 서버가 `sourceType=CURATED`로 저장하는 흐름으로 맞춰졌다. 등록 성공 후 목록도 정상 표시된다.
