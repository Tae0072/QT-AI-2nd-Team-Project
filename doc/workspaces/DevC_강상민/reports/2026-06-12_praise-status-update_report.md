# Report - 2026-06-12 찬양 곡 상태 변경 보강

## 요약

관리자 `찬양 큐레이션` 화면에서 곡 등록 시 `ACTIVE/HIDDEN` 상태를 선택할 수 있지만, 등록 이후에는 상태를 다시 바꿀 수 없었다. 운영자가 검수 대기 목적으로 `HIDDEN` 등록 후 공개하거나, 공개된 곡을 다시 숨기는 흐름이 필요하므로 수정 모달과 관리자 수정 API에 상태 변경을 추가했다.

## 변경 내용

- `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/api/dto/PraiseUpdateRequest.java`
  - 수정 요청에 선택적 `status` 필드를 추가했다.
  - 허용값은 `ACTIVE`, `HIDDEN`으로 제한했다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/internal/PraiseSong.java`
  - 메타데이터 수정 시 상태도 함께 갱신할 수 있게 했다.
  - `status`가 없으면 기존 상태를 유지한다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/internal/PraiseService.java`
  - 수정 요청의 `status`를 검증/파싱해 엔티티에 반영한다.
- `qtai-server/admin-server/src/test/java/com/qtai/domain/praise/internal/PraiseServiceTest.java`
  - 상태 없는 기존 수정 요청은 기존 상태를 유지하는지 확인했다.
  - `HIDDEN`으로 상태 변경되는 케이스를 추가했다.
- `admin-web/src/api/praiseSongs.ts`
  - `UpdatePraiseSongRequest`에 `status`를 추가했다.
- `admin-web/src/pages/PraiseSongsPage.tsx`
  - 수정 모달에 상태 선택 필드를 추가했다.
  - 현재 곡 상태를 수정 모달 초기값으로 표시한다.

## 확인 결과

- 수정 모달에서 기존 곡 상태가 표시된다.
- 숨김 곡을 `노출(ACTIVE)`로 변경 저장하면 목록 상태가 `노출`로 갱신된다.
- 기존 title/artist/licenseNote 수정 흐름은 유지된다.

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

## 브라우저 확인

- URL: `http://localhost:5173/praise-songs`
- 첫 번째 숨김 곡 수정 모달 오픈
- 상태를 `노출(ACTIVE)`로 변경 후 저장
- 목록에서 해당 곡 상태가 `노출`로 표시됨

## 결론

찬양 곡은 등록 후에도 관리자 수정 모달에서 `ACTIVE/HIDDEN` 상태를 전환할 수 있다. 이 상태는 사용자 추천 목록 노출 여부와 직접 연결된다.
