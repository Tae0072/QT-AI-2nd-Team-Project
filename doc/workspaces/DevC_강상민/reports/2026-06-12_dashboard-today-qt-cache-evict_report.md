# Report - 2026-06-12 대시보드 오늘 QT 캐시 무효화

## 요약

오늘 QT 관리에서 QT를 `게시`하면 대시보드의 오늘 QT 상태가 `READY`로 보이지만, 이후 `숨김` 처리해도 대시보드가 이전 `READY` 상태를 계속 표시하는 문제가 있었다.

원인은 `QtPassageLookup.findTodayPassage()`가 `cacheStatus=HIT`인 오늘 QT 응답을 `todayQt` 캐시에 저장하는데, 관리자 QT 상태 변경 경로에서 해당 캐시를 비우지 않았기 때문이다. `게시`는 이전 상태가 `MISS`일 때 캐시되지 않아 잘 반영되는 것처럼 보였고, `숨김`은 이미 저장된 `HIT` 캐시가 남아 대시보드가 오래된 값을 표시했다.

## 변경 내용

- `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/AdminQtPassageService.java`
  - `update(...)`, `publish(...)`, `hide(...)` 성공 시 `todayQt` 캐시를 전체 무효화하도록 `@CacheEvict(cacheNames = "todayQt", allEntries = true)`를 추가했다.

## 확인 결과

- 숨김 상태의 오늘 QT에서 대시보드 새로고침 시 `MISSING` 표시를 확인했다.
- 오늘 QT를 다시 게시하면 대시보드가 `READY / HIT`로 표시됨을 확인했다.
- 다시 숨김 처리하면 대시보드가 `MISSING`으로 변경됨을 확인했다.

## 검증 명령

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.qt.internal.AdminQtPassageServiceTest"
```

결과: 성공

```powershell
.\gradlew.bat :admin-server:compileJava
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

- `/dashboard`: 오늘 QT `READY`, 캐시 `HIT` 상태 확인
- `/qt-passages`: 2026-06-12 QT를 숨김 처리
- `/dashboard`: 오늘 QT `MISSING`, 제목 `-`, 캐시 `-` 확인
- `/qt-passages`: 같은 QT를 다시 게시
- `/dashboard`: 오늘 QT `READY`, 캐시 `HIT` 확인
- `/qt-passages`: 같은 QT를 다시 숨김
- `/dashboard`: 오늘 QT `MISSING` 확인

## 결론

관리자에서 오늘 QT를 게시/숨김/수정하면 대시보드 오늘 QT 캐시가 무효화된다. 숨김 후에도 이전 `READY` 상태가 남아 보이는 문제는 해결됐다.
