# Report - 2026-06-12 QT 본문 상태 필터 옵션 정리

## 요약

관리자 `/qt-passages` 화면의 상태 필터에 `삭제 예정`, `제거됨`이 노출되고 있었지만, 현재 구현된 서버 전이와 화면 액션에서는 해당 상태로 만들 수 없다. 운영자가 선택해도 일반적인 로컬/운영 데이터에서는 결과가 없을 가능성이 높아 혼란을 줄 수 있으므로, 필터 옵션은 실제 운영 가능한 3개 상태로 제한했다.

## 확인한 근거

- `QtPassageStatus` API 타입과 서버 enum은 5종 상태를 정의한다.
  - `pending_review`
  - `active`
  - `hidden`
  - `deletion_notified`
  - `removed`
- 현재 서버 액션은 3개 상태만 전이한다.
  - 등록: `pending_review`
  - 게시: `pending_review` 또는 `hidden` -> `active`
  - 숨김: `active` -> `hidden`
- `deletion_notified`, `removed`로 전이하는 관리자 API나 화면 버튼은 없다.
- 목록 Tag 매핑은 5종 상태를 모두 유지해야 서버가 해당 row를 반환해도 표시가 깨지지 않는다.

## 변경 내용

- `admin-web/src/pages/QtPassagesPage.tsx`
  - `STATUS_META`는 기존 5종 상태 매핑을 유지했다.
  - `FILTERABLE_STATUSES`를 추가해 상태 필터 노출 대상을 `pending_review`, `active`, `hidden`으로 제한했다.
  - `STATUS_OPTIONS`가 전체 상태 키가 아니라 `FILTERABLE_STATUSES`에서 생성되도록 변경했다.

## 제외한 내용

- 서버 enum 축소는 하지 않았다.
- API 타입 축소는 하지 않았다.
- DB migration 또는 기존 데이터는 변경하지 않았다.
- 삭제 예정/제거 상태 전이 API와 버튼은 추가하지 않았다.

## 검증 결과

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

브라우저 확인:

- URL: `http://localhost:5173/qt-passages`
- 상태 필터 드롭다운 옵션 확인 결과:
  - `검토 대기`
  - `게시됨`
  - `숨김`
- `삭제 예정`, `제거됨`은 더 이상 필터 옵션에 노출되지 않는다.

## 결론

현재 운영 플로우 기준으로 상태 필터는 3개 상태만 노출하는 것이 맞다. 5종 상태 계약은 유지했기 때문에 향후 삭제 예정/제거 플로우가 실제 구현되면 필터 노출 목록만 다시 확장하면 된다.
