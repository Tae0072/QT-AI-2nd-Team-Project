# Report - 2026-06-12 AI 산출물 유형 필터 제거

## 요약

관리자 `AI 산출물 검증` 화면 상단의 `유형` 필터를 제거했다. 기존 필터 옵션에는 `해설(EXPLANATION)`과 `성경구절(BIBLE_VERSE)`가 있었지만, 현재 검증 흐름에서 `BIBLE_VERSE`는 산출물 타입이라기보다 산출물이 붙는 대상 타입에 가깝다.

검증자는 목록과 상세에서 산출물 유형과 대상 구절 정보를 확인해야 하지만, 상단 필터에서 `BIBLE_VERSE`를 산출물 유형처럼 선택하게 하는 것은 혼동을 줄 수 있어 상태 필터만 남겼다.

## 확인한 근거

- 현재 화면의 상단 `유형` Select는 `assetType` 파라미터로 목록 API를 호출한다.
- 서버 생성/검증 흐름은 주로 `assetType=EXPLANATION`, `targetType=BIBLE_VERSE` 구조다.
- 목록/상세의 `대상` 표시는 검증 기준점으로 필요하다.
- 상단 필터에 `성경구절(BIBLE_VERSE)`를 두면 대상 타입을 산출물 타입처럼 오해할 수 있다.

## 변경 내용

- `admin-web/src/pages/AiAssetsPage.tsx`
  - `ASSET_TYPE_OPTIONS` 상수를 제거했다.
  - `assetType` React state를 제거했다.
  - 조회/초기화 필터 파라미터에서 `assetType`을 제거했다.
  - 필터 영역에서 `유형` Select를 제거했다.
  - 목록 컬럼과 상세 Drawer의 `assetType`, `targetType`, `targetId` 표시는 유지했다.

## 제외한 내용

- `admin-web/src/api/aiAssets.ts`의 API 타입은 유지했다.
- 서버 API와 OpenAPI는 변경하지 않았다.
- `targetType` 필터는 새로 추가하지 않았다.
- 승인/반려/숨김/재생성 액션은 변경하지 않았다.

## 검증 결과

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

브라우저 확인:

- URL: `http://localhost:5173/ai-assets`
- 상단 필터 Select는 `상태` 1개만 남았다.
- `조회`, `초기화`, 새로고침 버튼은 유지됐다.
- 목록 컬럼의 `유형`, `대상` 표시는 유지됐다.

## 결론

`성경 구절` 정보는 검증 기준으로 필요하지만, 상단 `유형` 필터에는 필요하지 않다. 현재 화면에서는 상태 필터만 남기는 편이 검수 업무 흐름에 더 맞다.
