# Report - 2026-06-12 신고 처리 POST 숨김 액션 연결

## 요약

관리자 `신고 처리` 화면을 점검한 결과, 백엔드는 POST 신고를 처리할 때 `action=HIDE_TARGET`이 함께 오면 대상 나눔글을 숨김 처리하도록 구현되어 있었지만, admin-web은 `action` 필드를 보내지 않고 있었다.

이 상태에서는 운영자가 POST 신고를 `처리(인정)`해도 신고 상태만 `RESOLVED`로 바뀌고, 대상 게시글 숨김 조치가 실행되지 않을 수 있다. 프런트엔드에서 POST 신고 처리 시 `HIDE_TARGET` 액션을 전송하도록 보강했다.

## 확인한 근거

- `AdminReportService`는 `newStatus == RESOLVED`, `command.action() == HIDE_TARGET`, `targetType == POST` 조건에서 `hideSharingPostForModerationUseCase.hideForModeration()`을 호출한다.
- `admin-web/src/api/reports.ts`의 `ProcessReportPayload`는 `reason`, `notifyReporter`만 표현하고 있었다.
- `admin-web/src/pages/ReportsPage.tsx`의 처리 제출 payload도 `action`을 포함하지 않았다.
- 브라우저 `/reports` 화면에는 신고 4건이 표시되며 모두 종료 상태라 실제 처리 버튼 제출은 수행하지 않았다.

## 변경 내용

- `admin-web/src/api/reports.ts`
  - `ProcessReportPayload`에 `action?: 'HIDE_TARGET'`를 추가했다.
- `admin-web/src/pages/ReportsPage.tsx`
  - `submitAction` payload 타입을 `ProcessReportPayload`로 명시했다.
  - `resolve` 모드이고 대상이 `POST`이면 `payload.action = 'HIDE_TARGET'`를 설정하도록 했다.
  - POST 처리 모달에 `처리 시 대상 나눔글이 숨김 처리됩니다.` 안내를 추가했다.

## 제외한 내용

- 댓글, AI Q&A, AI 산출물 대상 숨김/후속 조치 연동은 추가하지 않았다.
- 서버 API와 DB schema는 변경하지 않았다.
- `REVIEWING` 상태 전환 UI는 추가하지 않았다.
- 대상 상세 링크는 추가하지 않았다.

## 검증 결과

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

브라우저 확인:

- URL: `http://localhost:5173/reports`
- `신고 처리` 화면 렌더링 정상
- 신고 목록 4건 표시 정상
- 현재 데이터 4건 모두 `처리완료` 또는 `반려` 상태라 실제 처리 제출은 수행하지 않음

## 후속 검토

- 실제 `RECEIVED` 상태의 POST 신고 데이터가 있을 때 처리 모달에서 숨김 안내가 보이고, 처리 후 대상 게시글이 숨김 처리되는지 통합 확인이 필요하다.
- 댓글/AI 대상 신고 처리 시 어떤 후속 조치를 해야 하는지는 정책 확정 후 별도 작업으로 진행하는 것이 맞다.
- 운영자가 신고 대상 원문을 바로 확인할 수 있도록 대상 링크 또는 상세 drawer 추가를 검토할 만하다.
