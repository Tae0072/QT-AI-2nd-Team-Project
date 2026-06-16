# Report - 2026-06-12 AI 산출물 상세 재생성 버튼 노출 조건 정리

## 요약

관리자 `AI 산출물 검증` 상세 Drawer에서 승인(`APPROVED`) 산출물에도 `재생성` 버튼이 노출되는 문제를 수정했다. 서버는 재생성을 `REJECTED` 또는 `HIDDEN` 상태에서만 허용하므로, 승인 산출물에서 버튼을 보여주는 것은 잘못된 UI 계약이었다.

## 확인한 근거

- `AiService.requireRegeneratableStatus`는 `REJECTED` 또는 `HIDDEN` 상태만 통과시킨다.
- 그 외 상태는 `INVALID_STATUS_TRANSITION`으로 거절하며, 메시지는 `REJECTED 또는 HIDDEN 상태의 AI 산출물만 재생성을 요청할 수 있습니다.` 계열이다.
- 수정 전 브라우저에서 승인 산출물 `#31101` 상세 Drawer에도 `재생성` 버튼이 활성화되어 있었다.

## 변경 내용

- `admin-web/src/pages/AiAssetsPage.tsx`
  - `isRegeneratable(status)` helper를 추가했다.
  - 상세 Drawer의 `재생성` 버튼을 `REJECTED` 또는 `HIDDEN` 상태에서만 렌더링하도록 변경했다.

## 유지한 내용

- 재생성 모달 입력값과 API 호출 로직은 유지했다.
- 서버 재생성 정책은 변경하지 않았다.
- 승인/반려/숨김 액션 로직은 변경하지 않았다.

## 검증 결과

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

브라우저 확인:

- URL: `http://localhost:5173/ai-assets`
- 승인 산출물 `#31101` 상세 Drawer: `재생성` 버튼 없음
- 반려 산출물 `#31102` 상세 Drawer: `재생성` 버튼 있음

## 결론

이 문제는 서버 오류가 아니라 프론트가 서버에서 거절할 액션을 승인 상태에서도 노출한 UI 계약 불일치였다. 버튼 노출 조건을 서버의 재생성 가능 상태와 맞춰, 사용자가 승인 산출물에서 실행 불가능한 재생성 요청을 누를 수 없게 했다.
