# 2026-06-09 · admin-web 환경/배포 학습 노트

## Vite 환경 변수

Vite 프런트엔드에서 브라우저 코드가 읽을 수 있는 환경 변수는 `VITE_` prefix가 붙은 값뿐이다.
이번 관리자 웹은 다음 두 값을 사용한다.

- `VITE_API_BASE_URL`: axios의 `baseURL`
- `VITE_API_PROXY_TARGET`: 로컬 Vite dev server proxy target

## 로컬 proxy와 배포 base URL

로컬 개발에서는 브라우저가 `/api/v1/...`로 요청하고, Vite dev server가 `/api` 요청을 백엔드로 전달한다.
이 방식은 CORS 설정 없이도 로컬 백엔드를 붙이기 편하다.

배포에서는 정적 파일 서버와 API 서버 구성이 다를 수 있다.
운영 reverse proxy가 `/api`를 백엔드로 넘겨주면 `VITE_API_BASE_URL=/api/v1`을 유지할 수 있다.
그렇지 않으면 빌드 시점에 `VITE_API_BASE_URL=https://.../api/v1`처럼 전체 주소를 넣어야 한다.

## 예시 변수는 실제 코드와 맞아야 한다

`.env.example`은 팀원이 그대로 복사해서 쓰는 파일이다.
실제 코드가 읽지 않는 `VITE_DEV_ADMIN_MEMBER_ID` 같은 값이 들어 있으면, 설정만으로 인증 우회 헤더가 전송된다고 착각할 수 있다.
환경 예시는 현재 코드가 실제로 사용하는 값만 남기는 편이 안전하다.

## 취약점 추적

`npm audit --omit=dev`는 운영 의존성 기준 확인에 사용한다.
dev dependency의 Vite/esbuild moderate 이슈는 개발 서버 영향권이고, 자동 수정이 Vite 8 breaking upgrade를 요구하므로 별도 업그레이드 PR에서 처리한다.
