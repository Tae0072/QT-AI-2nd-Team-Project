# Report - 2026-06-12 AI 산출물 상태 필터 계약 정리

## 요약

관리자 `AI 산출물 검증` 화면에서 상단 상태 필터의 `검토필요(NEEDS_REVIEW)`를 선택하고 `조회`하면 `올바르지 않은 요청입니다.` 메시지가 표시되는 문제를 수정했다.

원인은 `NEEDS_REVIEW`가 산출물 상태(`AiGeneratedAssetStatus`)가 아니라 검증 결과(`AiValidationResult`)인데, 프론트 상태 필터가 이 값을 `/api/v1/admin/ai/assets?status=NEEDS_REVIEW`로 전송했기 때문이다.

## 확인한 근거

- 서버 산출물 상태 enum: `VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN`
- 서버 검증 결과 enum: `PASSED`, `REJECTED`, `NEEDS_REVIEW`
- OpenAPI의 관리자 AI 산출물 목록 `status` query enum도 `VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN`만 허용한다.
- 브라우저에서 수정 전 `검토필요(NEEDS_REVIEW)` 선택 후 `조회` 시 `올바르지 않은 요청입니다.` 메시지를 재현했다.

## 변경 내용

- `admin-web/src/pages/AiAssetsPage.tsx`
  - 상태 필터 옵션에서 `검토필요(NEEDS_REVIEW)`를 제거했다.
  - 산출물 상태 태그 매핑에서 `NEEDS_REVIEW`를 제거했다.
  - 승인/반려 버튼 노출 조건을 실제 전이 가능한 `VALIDATING` 상태로 제한했다.
- `admin-web/src/api/aiAssets.ts`
  - `AiAsset.status` 주석을 서버 산출물 상태 enum에 맞게 수정했다.

## 유지한 내용

- 목록 `검증결과` 컬럼의 `NEEDS_REVIEW` 값 표시는 유지했다.
- 상세 Drawer의 검증 로그 결과 표시도 유지했다.
- 승인/반려/숨김/재생성 API 동작은 변경하지 않았다.
- 서버 enum, OpenAPI, DB schema는 변경하지 않았다.

## 검증 결과

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

브라우저 확인:

- URL: `http://localhost:5173/ai-assets`
- 상태 Select 옵션: `검증중(VALIDATING)`, `승인(APPROVED)`, `반려(REJECTED)`, `숨김(HIDDEN)`
- `검토필요(NEEDS_REVIEW)`는 상태 Select에서 제거됨
- `검증중(VALIDATING)`으로 조회 시 `올바르지 않은 요청입니다.` 메시지 없이 목록이 표시됨

## 결론

이번 문제는 DB 중복이나 권한 문제가 아니라 프론트 상태 필터가 서버가 허용하지 않는 검증 결과값을 산출물 상태값처럼 전송해서 발생한 계약 불일치였다. 상태 필터는 산출물 상태만 다루도록 정리했고, 검증 결과의 `NEEDS_REVIEW` 표시는 기존처럼 목록과 상세 로그에서 확인할 수 있다.
