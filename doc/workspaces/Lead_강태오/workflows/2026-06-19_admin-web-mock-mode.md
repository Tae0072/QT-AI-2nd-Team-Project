# 워크플로우 — 관리자 웹 목업(데모) 모드

- 날짜: 2026-06-19
- 담당: Lead 강태오
- 대상: `admin-web` (React + Vite + TS + antd)
- 목적: 시연(데모)에서 **백엔드(qtai-server/admin-server) 없이도** 관리자 웹의 모든 메뉴(AD-01~20)가 목업 데이터로 동작하게 한다.

## 배경

관리자 웹은 `apiClient`(axios) 하나로 `/api/v1/admin/**` 를 호출한다. 데모 환경에서 모든 서비스(8081~8090)와 DB를 띄우지 않고도 화면을 보여줘야 하므로, **axios adapter 를 가로채는 인메모리 목업 계층**을 추가했다. 페이지/Provider 코드는 전혀 손대지 않고, 데이터 계층만 교체하는 방식이라 운영 코드 영향이 0이다.

## 접근

1. 전체 엔드포인트(약 90개)·타입을 `src/api/*.ts` 에서 추출(경로/메서드/반환형).
2. 공통 봉투(`{ success, data, error }`) + `Page`/`SpringPage` 헬퍼 작성.
3. method+URL 정규식 라우트 표로 모든 엔드포인트를 매핑. 목록은 시드 배열을 페이지로, 액션(승인/숨김/생성/삭제)은 인메모리 배열을 변경해 화면에 즉시 반영.
4. 로그인은 어떤 자격으로도 성공하며 `adminRole=SUPER_ADMIN` 을 반환 → 모든 메뉴 노출.
5. 플래그(`USE_ADMIN_MOCK`)가 켜질 때만 `client.ts` 가 `apiClient.defaults.adapter` 를 목업으로 교체.

## 안전장치(운영 비혼입)

- `USE_ADMIN_MOCK = (MODE === 'mock') || (VITE_ADMIN_MOCK === '1')`. 일반 `vite`/`vite build` 는 MODE 가 development/production 이라 **항상 꺼짐**.
- 목업 모듈은 import 만 정적으로 되고, 플래그가 꺼지면 어댑터가 설치되지 않아 실제 백엔드를 그대로 호출.
- DB·서버를 전혀 건드리지 않음(메모리 배열만 변경). 새로고침/재시작 시 시드 초기 상태로 복귀.
- 활성화 시 콘솔에 경고 로그를 남겨 운영 오인을 방지.

## 변경 파일

- `admin-web/src/api/mock/mockAdapter.ts` (신규) — 어댑터 + 시드 + 라우트 표.
- `admin-web/src/config/env.ts` — `USE_ADMIN_MOCK` 추가.
- `admin-web/src/api/client.ts` — 플래그 시 어댑터 교체.
- `admin-web/package.json` — `dev:mock` 스크립트(`vite --mode mock`).
- (로컬 전용) `admin-web/.env.mock` — `.gitignore` 대상이라 커밋되지 않음. MODE 판별로 대체.

## 실행 방법

```bash
cd admin-web
npm run dev:mock     # http://localhost:5173 — 백엔드 불필요
# 로그인: 아이디/비밀번호 아무 값 → SUPER_ADMIN 으로 전체 메뉴 진입
```

## 검증

- `npx tsc --noEmit` 통과, `npm run build`(tsc && vite build) 통과.
- 런타임 스모크(Chrome): 대시보드/회원 관리/QT 영상 관리/AI 평가 세트/자가진단 렌더 확인.
- 자가진단(AD-18) **전체 점검 16/16 성공 · 실패 0** — 모든 백엔드 엔드포인트가 200 응답.
- 참고: 기존 `scripts/admin-page-contracts.test.mjs` 는 06-15에 삭제된 `PraiseSongsPage.tsx`(AD-05→AD-12 통합)를 참조해 ENOENT 로 실패하나, 본 작업과 무관한 **기존 결함**이다.
