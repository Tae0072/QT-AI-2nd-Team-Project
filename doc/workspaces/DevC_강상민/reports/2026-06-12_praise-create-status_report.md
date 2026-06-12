# Report - 2026-06-12 찬양 등록 상태 반영

## 요약

관리자 `찬양 큐레이션` 화면은 곡 등록 시 `ACTIVE/HIDDEN` 상태를 선택하게 되어 있었지만, 서버 `PraiseCreateRequest`가 `status`를 받지 않았고 `PraiseService.create()`가 항상 `ACTIVE`로 저장하고 있었다.

운영자가 검수 전 곡을 `HIDDEN`으로 먼저 등록할 수 있도록 등록 요청의 `status` 값을 `praise_songs.status`에 반영하게 수정했다. 사용자 앱의 큐레이션 목록은 기존처럼 `ACTIVE` 곡만 조회하므로, `HIDDEN`으로 등록한 곡은 사용자에게 노출되지 않는다.

## 변경 내용

- `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/api/dto/PraiseCreateRequest.java`
  - 등록 요청에 `String status` 필드를 추가했다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/internal/PraiseService.java`
  - 등록 시 `request.status()`를 파싱해 `ACTIVE` 또는 `HIDDEN`으로 저장한다.
  - status가 비어 있으면 기존 호환성을 위해 `ACTIVE`로 저장한다.
  - 잘못된 status는 `INVALID_INPUT`으로 거절한다.
- `qtai-server/service-bible/src/main/java/com/qtai/domain/praise/api/dto/PraiseCreateRequest.java`
  - admin-server와 동일하게 `status` 필드를 추가했다.
- `qtai-server/service-bible/src/main/java/com/qtai/domain/praise/internal/PraiseService.java`
  - 사용자 서비스 원본 로직도 같은 status 파싱 규칙으로 동기화했다.
- `qtai-server/admin-server/src/test/java/com/qtai/domain/praise/internal/PraiseServiceTest.java`
  - `HIDDEN` 등록, status 미지정 기본값 `ACTIVE`, 잘못된 status 거절 테스트를 추가했다.

## 유지한 내용

- 관리자 등록 경로의 `sourceType`은 계속 `CURATED`로 고정한다.
- 가사, 음원, 외부 URL 저장 필드는 추가하지 않았다.
- 사용자 목록 조회는 기존처럼 `status = ACTIVE`만 반환한다.
- 관리자 수정 화면의 상태 변경 기능은 이번 범위에 포함하지 않았다.

## 검증 결과

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.praise.internal.PraiseServiceTest"
```

결과: 성공

```powershell
.\gradlew.bat :admin-server:compileJava
```

결과: 성공

```powershell
.\gradlew.bat :service-bible:compileJava
```

결과: 성공

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

```powershell
git diff --check
```

결과: 성공. CRLF 변환 경고만 표시됨.

브라우저 확인:

- URL: `http://localhost:5173/praise-songs`
- `찬양 큐레이션` 화면 렌더링 정상
- `곡 등록` 버튼 노출 정상

## 확인 제한

현재 실행 중인 Docker `qtai-admin-server` 컨테이너는 수정 전 빌드로 떠 있다. 따라서 실제 브라우저에서 `HIDDEN` 등록 후 DB 저장 상태까지 확인하려면 서버 이미지를 재빌드하거나 admin-server를 재기동해야 한다.

## 결론

이제 관리자 곡 등록은 `praise_songs`에 저장하되, 등록 시 `ACTIVE/HIDDEN` 선택값을 실제로 반영한다. 운영자는 검수 전 곡을 `HIDDEN`으로 등록할 수 있고, 사용자 앱 큐레이션 목록에는 `ACTIVE` 곡만 노출되는 흐름이 된다.
