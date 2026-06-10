# 2026-06-10 · admin-web AD-09/AD-10 신규 화면 리포트

## 요약

관리자 웹에서 비어 있던 AI 운영 화면 두 개를 백엔드 계약에 맞춰 신규 구현했다. `AdminAiValidationChecklistController`에 대응하는 **AD-09 AI 검증 체크리스트 관리**(목록·등록·활성화·폐기)와 `AdminAiBatchRunLogController`에 대응하는 **AD-10 AI 배치 실행 로그**(필터 목록·오류 상세)를 추가했다. 더불어 AD-08 모니터링에 두 화면으로의 바로가기, AD-03 검증 화면에 메타데이터 상세 펼침(원문 미노출)을 보강했다.

## 현황 점검 결과

작업 착수 전 점검에서, AD-01~08 화면은 이미 구현되어 `dev-admin-web`에 머지된 상태임을 확인했다(이 브랜치가 동일 커밋). 따라서 기존 화면 재구현 대신, 계약상 존재하나 프런트가 없던 두 컨트롤러를 채우는 방향으로 진행했다.

## 변경 내용

| 영역 | 파일 | 내용 |
|---|---|---|
| API | `api/aiChecklists.ts` (신규) | 목록/등록/활성화/폐기 호출, 타입 정의 |
| API | `api/aiBatchRunLogs.ts` (신규) | 배치 로그 목록 호출, 타입 정의 |
| 화면 | `pages/AiChecklistsPage.tsx` (신규, AD-09) | 표·필터(유형/상태)·등록 모달·활성화/폐기(Popconfirm) |
| 화면 | `pages/AiBatchRunLogsPage.tsx` (신규, AD-10) | 표·필터(배치명/상태/기간)·행 펼침 오류 상세 |
| 메뉴/라우트 | `constants/menu.ts`, `App.tsx` | AD-09/AD-10 등록, 권한 기준 노출 |
| 보강 | `pages/AiMonitoringPage.tsx` | 배치/체크리스트 카드 → AD-10/AD-09 바로가기 |
| 보강 | `pages/AiAssetsPage.tsx` | 산출물 행 펼침 메타데이터 상세(원문 미노출 안내) |

## 백엔드 계약 대조

| 항목 | 값 |
|---|---|
| 체크리스트 엔드포인트 | `GET/POST /api/v1/admin/ai/validation-checklists`, `POST /{id}/activate`, `POST /{id}/retire` |
| 체크리스트 유형 | `EXPLANATION`, `SIMULATOR`, `QA` |
| 체크리스트 상태 | `DRAFT`, `ACTIVE`, `RETIRED` |
| 체크리스트 권한 | REVIEWER (SUPER_ADMIN 우월권 포함) |
| 배치 로그 엔드포인트 | `GET /api/v1/admin/ai/batch-run-logs` (batchName/status/from/to) |
| 배치 상태 | `SUCCEEDED`, `PARTIAL_FAILED`, `FAILED` |
| 배치 로그 권한 | OPERATOR/REVIEWER (SUPER_ADMIN 포함) |

응답 페이징은 기존 `Page<T>`(content/page/size/totalElements/totalPages) 형식과 호환되어 공통 `usePagedList` 훅을 그대로 사용했다.

## 금지 정책 확인 (CLAUDE.md §7/§8)

- AD-09: 버전·콘텐츠 해시·상태 등 메타데이터만 입력/표시. 체크 항목 원문 필드 없음.
- AD-10: 읽기 전용. 산출물 원문 필드 없음.
- AD-03 상세: "산출물 원문·검증 참조 자료는 정책상 노출하지 않습니다" 안내를 명시하고 메타데이터만 표시.

## 검증

- `npm ci`: 통과 (mount 성능 문제로 node_modules는 격리 환경에서 설치 후 빌드).
- `npm run build`(tsc + vite): 통과, 3119 modules transformed.
- `tsc --noEmit` 2차 패스: 통과.
- Vite chunk size 경고는 기존 antd 번들 경고로 비차단.

## 한계 / 후속

- 실제 ADMIN 토큰 기반 브라우저 E2E QA는 백엔드와 유효 토큰이 준비된 환경에서 별도 확인 필요.
- 체크리스트 활성화 시 "같은 유형 기존 활성본 교체" 동작은 백엔드 정책에 의존하며, 화면에서는 안내(Alert/Popconfirm)만 제공한다.
- AD-01/02/06은 백엔드(E단계) 구현 시 "준비 중" 안내가 자동 해제되도록 기존 화면이 처리한다.
