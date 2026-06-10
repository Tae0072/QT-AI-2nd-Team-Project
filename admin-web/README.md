# QT-AI 관리자 웹 (admin-web)

QT-AI의 **관리자 전용 웹 프런트엔드**입니다. 사용자용 Flutter 앱(`flutter-app/`)과 분리된 별도 웹이며,
같은 백엔드(`qtai-server`)의 `/api/v1/admin/**` API를 호출합니다.

> 근거: 2026-05-19 강사님 직강 결정 — "관리자에게 앱은 불편해서 못 쓴다"
> (`03_아키텍처_정의서.md` v1.3 §4.9 / §13.6, `22_구현_저장소_반영_체크리스트.md` v0.3 §3)

이 폴더는 현재 **전체 구조(골격) 단계**입니다. 각 화면은 아직 빈 페이지이며, 다음 단계에서 실제 표·폼을 붙입니다.

---

## 1. 기술 스택

| 구분 | 선택 | 이유 |
|---|---|---|
| 빌드 도구 | Vite | 빠르고 설정이 단순함 |
| 언어/프레임워크 | React 18 + TypeScript | 표준적이고 학습 자료가 많음 |
| UI 라이브러리 | Ant Design (antd) | 관리자 화면용 컴포넌트(표·폼·모달)가 풍부함 |
| 라우팅 | React Router v6 | 주소(URL)에 따른 화면 전환 |
| HTTP 통신 | axios | 토큰 자동 첨부·에러 공통 처리에 편리 |

## 2. 처음 실행하는 방법 (입문자용)

> 사전 준비: 컴퓨터에 [Node.js](https://nodejs.org) LTS 버전이 설치되어 있어야 합니다. (`node -v` 로 확인)

```bash
# 1) 이 폴더로 이동
cd admin-web

# 2) 환경 변수 파일 만들기 (.env.example 를 복사)
cp .env.example .env       # Windows PowerShell: copy .env.example .env

# 3) 라이브러리 설치 (처음 한 번)
npm install

# 4) 개발 서버 실행
npm run dev
```

실행되면 터미널에 `http://localhost:5173` 주소가 나옵니다. 브라우저로 열면 로그인 화면이 보입니다.

### 로그인 방법 (임시 토큰 방식)

지금은 카카오 웹 로그인 연동 전이라, **발급받은 ADMIN 액세스 토큰을 직접 붙여넣어** 로그인합니다.
로그인 화면 입력칸에 토큰 값만(`Bearer ` 글자는 빼고) 넣고 "로그인"을 누르면 됩니다.

### 백엔드 연결

개발 서버는 `/api` 로 시작하는 요청을 자동으로 `http://localhost:8080`(qtai-server)로 전달합니다(프록시).
백엔드 주소가 다르면 `vite.config.ts` 의 `target` 값을 바꾸세요.

## 3. 자주 쓰는 명령어

| 명령 | 설명 |
|---|---|
| `npm run dev` | 개발 서버 실행 (코드 저장하면 자동 새로고침) |
| `npm run build` | 타입 검사 + 배포용 빌드 (`dist/` 생성) |
| `npm run preview` | 빌드 결과물 미리보기 |
| `npm run typecheck` | 타입 오류만 검사 |

## 4. 폴더 구조

```
admin-web/
├─ index.html              # 시작 HTML
├─ vite.config.ts          # Vite 설정 (포트, 백엔드 프록시)
├─ tsconfig.json           # TypeScript 설정
├─ .env.example            # 환경 변수 예시 (복사해서 .env 로)
└─ src/
   ├─ main.tsx             # 앱 진입점 (Provider/Router 설정)
   ├─ App.tsx              # 주소 ↔ 화면 연결(라우팅)
   ├─ config/
   │   └─ env.ts           # .env 값 읽기
   ├─ api/                 # 백엔드 호출 코드
   │   ├─ client.ts        # axios 인스턴스 (토큰 자동 첨부, 에러 처리)
   │   ├─ types.ts         # 공통 응답/페이징 타입
   │   └─ *.ts             # 화면(AD-01~08)별 API 함수
   ├─ auth/                # 로그인 상태 관리
   │   ├─ AuthContext.tsx  # 토큰 공유 Context
   │   ├─ useAuth.ts       # 로그인 상태 사용 훅
   │   └─ tokenStorage.ts  # 토큰 저장/삭제
   ├─ routes/
   │   └─ ProtectedRoute.tsx  # 로그인 안 하면 막는 가드
   ├─ components/
   │   ├─ PagePlaceholder.tsx # 빈 화면 공통 틀
   │   └─ layout/AdminLayout.tsx # 사이드바 + 헤더 레이아웃
   ├─ constants/
   │   ├─ menu.ts          # 사이드바 메뉴 + 권한 정의
   │   └─ roles.ts         # 관리자 세부 권한 상수
   └─ pages/               # 화면들 (AD-01~08 + 로그인/404)
```

## 5. 화면 목록 (화면 코드 ↔ API)

| 코드 | 화면 | 주요 API | 필요 권한 |
|---|---|---|---|
| AD-01 | 대시보드 | `GET /api/v1/admin/dashboard` | ADMIN |
| AD-02 | 오늘 QT 관리 | `GET/POST/PATCH /api/v1/admin/qt-passages` | OPERATOR |
| AD-03 | AI 산출물 검증 | `GET/POST /api/v1/admin/ai/assets` | REVIEWER / SUPER_ADMIN |
| AD-04 | 신고 처리 | `GET/POST /api/v1/admin/reports` | OPERATOR |
| AD-05 | 찬양 큐레이션 | `GET/POST/PATCH /api/v1/admin/praise-songs` | OPERATOR |
| AD-06 | 시스템 공지 | `GET/POST/PATCH /api/v1/admin/notices` | OPERATOR |
| AD-07 | 감사 로그 | `GET /api/v1/admin/audit-logs` | SUPER_ADMIN |
| AD-08 | AI 운영 모니터링 | `GET /api/v1/admin/ai/monitoring` | OPERATOR(집계만) |

> 권한 일부는 추정값입니다. `src/constants/menu.ts` 의 TODO 주석을 보고 `04_API_명세서.md` 권한표 기준으로 최종 확정하세요.

## 6. 다음 단계 (이 골격 이후 할 일)

1. 화면별 실제 구현: 목록 표 + 필터 + 페이지네이션, 등록/수정 폼, 승인/반려 모달
2. 세부 권한별 메뉴 노출·접근 제어 (현재는 로그인 여부만 확인)
3. 카카오 웹 로그인 / 서버측 OAuth 연동 (현재는 임시 토큰 입력)
4. 공통 에러 토스트, 로딩 표시, 페이지네이션 컴포넌트
5. 환경별(.env) 백엔드 주소 분리, 배포 설정

## 7. 주의 (프로젝트 규칙)

- 관리자 화면은 **이 폴더에서만** 만듭니다. Flutter 앱(`flutter-app/`)에 관리자 화면을 넣지 않습니다.
- 가사·음원 파일·직접 YouTube URL 저장 금지(AD-05). AI 산출물은 승인 전 원문을 노출하지 않습니다(AD-03).
- 자세한 규칙은 저장소 루트 `CLAUDE.md` 를 참고하세요.
