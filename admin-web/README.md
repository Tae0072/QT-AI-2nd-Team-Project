# QT-AI 관리자 웹(admin-web)

QT-AI 관리자 웹은 Flutter 앱과 분리된 React 기반 관리자 프런트엔드입니다.
같은 `qtai-server` 또는 MSA 게이트웨이의 `/api/v1/admin/**` API를 호출합니다.

## 현재 상태

- 구현 완료 화면: AD-03 AI 산출물 검증, AD-04 신고 처리, AD-05 찬양 큐레이션, AD-07 감사 로그, AD-08 AI 운영 모니터링
- 백엔드 확정 대기 화면: AD-01 대시보드, AD-02 오늘 QT 관리, AD-06 시스템 공지
- 로그인: 임시 ADMIN 액세스 토큰 직접 입력
- 권한 확인: 로그인 직후 `GET /api/v1/admin/me`를 호출해 `adminRole` 기준으로 메뉴와 라우트를 제한
- 에러 처리: `/admin/me` 401/403은 세션 종료, 네트워크/5xx/timeout은 세션 유지 후 재시도 안내
- 운영 전에는 임시 토큰 입력 방식을 제거하고 공식 관리자 로그인 흐름으로 대체해야 합니다.

## 실행 준비

Node.js LTS가 필요합니다.

```bash
cd admin-web
copy .env.example .env
npm ci
npm run dev
```

macOS/Linux에서는 `.env` 복사 명령만 아래처럼 바꾸면 됩니다.

```bash
cp .env.example .env
```

개발 서버 기본 주소는 `http://localhost:5173`입니다.

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `VITE_API_BASE_URL` | `/api/v1` | 프런트에서 호출할 API base URL. 개발 proxy를 쓰면 기본값 유지 |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Vite 개발 서버가 `/api` 요청을 전달할 백엔드 주소 |

로컬에서 모놀리식 `qtai-server`를 직접 호출하면 `VITE_API_PROXY_TARGET=http://localhost:8080`을 사용합니다.
MSA 게이트웨이를 경유하면 이 값을 게이트웨이 주소(예: `http://localhost:8000`)로 바꿉니다.

배포 환경에서 별도 proxy가 없다면 `VITE_API_BASE_URL`을 전체 API 주소로 지정합니다.

```env
VITE_API_BASE_URL=https://api.example.com/api/v1
```

`.env`는 로컬 전용 파일이며 git에 올리지 않습니다.

## 명령어

| 명령 | 설명 |
|---|---|
| `npm run dev` | Vite 개발 서버 실행 |
| `npm run typecheck` | TypeScript 타입 검사 |
| `npm run build` | 타입 검사 후 배포 산출물 `dist/` 생성 |
| `npm run preview` | `dist/` 산출물 로컬 미리보기 |
| `npm audit --omit=dev` | 운영 의존성 취약점 확인 |

현재 `npm audit --omit=dev`는 운영 의존성 기준 0건을 목표로 봅니다.
전체 `npm audit`의 Vite/esbuild dev dependency moderate 2건은 Vite 8 breaking upgrade가 필요해 별도 후속으로 추적합니다.

## 폴더 구조

```text
admin-web/
├─ index.html
├─ package-lock.json
├─ package.json
├─ vite.config.ts
├─ .env.example
└─ src/
   ├─ api/             # 관리자 API 호출 함수와 공통 axios client
   ├─ auth/            # 토큰 저장, 로그인 상태, /admin/me 권한 조회
   ├─ components/      # 공통 레이아웃/표시 컴포넌트
   ├─ config/          # 환경 변수 읽기
   ├─ constants/       # 메뉴와 관리자 권한 상수
   ├─ hooks/           # 목록 화면 공통 hook
   ├─ pages/           # AD-01~AD-08 화면
   ├─ routes/          # 로그인/권한 라우트 가드
   └─ utils/           # 날짜 등 화면 유틸
```

## 화면과 권한

| 코드 | 화면 | 주요 API | 접근 권한 |
|---|---|---|---|
| AD-01 | 대시보드 | `GET /api/v1/admin/dashboard` | ADMIN 공통, 백엔드 미구현 |
| AD-02 | 오늘 QT 관리 | `GET/POST/PATCH /api/v1/admin/qt-passages` | OPERATOR, SUPER_ADMIN, 백엔드 미구현 |
| AD-03 | AI 산출물 검증 | `GET/POST /api/v1/admin/ai/assets` | REVIEWER, SUPER_ADMIN |
| AD-04 | 신고 처리 | `GET/POST /api/v1/admin/reports` | OPERATOR, SUPER_ADMIN |
| AD-05 | 찬양 큐레이션 | `GET/POST/PATCH /api/v1/admin/praise-songs` | OPERATOR, SUPER_ADMIN |
| AD-06 | 시스템 공지 | `GET/POST/PATCH /api/v1/admin/notices` | OPERATOR, SUPER_ADMIN, 백엔드 미구현 |
| AD-07 | 감사 로그 | `GET /api/v1/admin/audit-logs` | OPERATOR, REVIEWER, SUPER_ADMIN |
| AD-08 | AI 운영 모니터링 | `GET /api/v1/admin/ai/monitoring` | OPERATOR, REVIEWER, SUPER_ADMIN |

권한 표는 `src/constants/menu.ts`와 백엔드 `AdminAiAuthentication`, `AdminAuditAuthentication`,
`VerifyAdminRoleUseCase` 기준으로 맞춥니다. 정책이 바뀌면 README와 메뉴 정의를 함께 갱신합니다.

## 배포 메모

- `npm ci && npm run build`로 `dist/`를 생성합니다.
- `dist/`는 정적 파일 호스팅 또는 웹 서버에 배포합니다.
- 운영 환경에서 `/api` reverse proxy를 제공하지 않으면 `VITE_API_BASE_URL`을 실제 API 전체 주소로 빌드합니다.
- `node_modules/`, `dist/`, `.env`는 커밋하지 않습니다.

## 주의

- 관리자 화면은 `admin-web/` 안에서만 관리합니다. Flutter 앱에는 관리자 UI를 넣지 않습니다.
- AD-05는 가사, 음원 파일, 직접 URL 입력을 받지 않습니다.
- AI 산출물 화면은 승인 전 본문 전문 노출을 피합니다.
