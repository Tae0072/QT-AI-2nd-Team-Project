# [Workflow] 관리자 웹 작업 일정·역할 분담 정리 (2026-06-08)

## 목적

admin-web 골격(#318, dev 머지 완료) 이후, 강태오·김지민 2인의 관리자 웹 후속 작업을 분담하기 위한 작업 일정(TODO) 문서화.

## 진행

1. `origin/dev` 기준 현황 점검
   - admin-web 골격: 화면 8개(AD-01~08) 빈 페이지 + 메뉴/라우팅/로그인/API 호출 틀.
   - 백엔드 관리자 API 준비 현황: `reports`(완성)·`audit-logs`·`ai/assets`·`ai/monitoring`·`praise-songs`(등록) 존재 / `dashboard`·`qt-passages`·`notices` 미존재.
   - dev 인증 현실: `dev-bypass: true`(무인증 통과), 일부 admin API는 `admin_users` 재검증, dev 관리자 계정 미시드.
2. 전체 작업을 A(준비)~F(인증 정식화·마무리) 단계로 분해.
3. 담당 분담 제안: 강태오=환경·dev 관리자 인증·권한 정책·백엔드 협의·카카오 웹 로그인 / 김지민=화면(AD-01~08)·공통 컴포넌트·권한 화면 적용·빌드/배포.
4. 담당자별 작업 일정 md 작성(Lead_강태오, DevE_김지민).

## 산출물

- `doc/workspaces/Lead_강태오/2026-06-08_admin-web-todo-kangtaeo.md`
- `doc/workspaces/DevE_김지민/2026-06-08_admin-web-todo-kimjimin.md`

## 기준 문서

- `admin-web/README.md`(화면↔API 매핑), `04_API_명세서.md`(권한표), `CLAUDE.md` §5/§6/§8
- 관련: #318 admin-web scaffold, `doc/workspaces/Lead_강태오/designs/2026-06-07_web-kakao-login-server-oauth_design.md`

## 다음

- A(준비) → B(dev 관리자 로그인 경로) → C(화면) 순으로 착수. 착수 시 단계별 워크플로우/리포트 별도 기록.
