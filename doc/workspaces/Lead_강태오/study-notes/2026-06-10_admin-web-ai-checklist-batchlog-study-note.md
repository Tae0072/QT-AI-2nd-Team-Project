# 2026-06-10 · admin-web AD-09/AD-10 학습 노트 (입문자용)

## 1. "화면이 이미 있는지" 먼저 확인하기

요청은 "빈 화면을 구현해 달라"였지만, 막상 브랜치를 열어 보니 AD-01~08은 이미 다 만들어져 머지된 상태였다.
새 코드를 쓰기 전에 **현재 저장소 상태를 먼저 읽는 것**이 중요하다. 안 그러면 이미 있는 걸 또 만드는 낭비가 생긴다.
이번엔 점검 결과 "정말로 없는 부분"은 백엔드에는 있는데 프런트만 없던 두 화면(AD-09, AD-10)이었다.

## 2. 백엔드 컨트롤러가 곧 화면의 '계약서'

프런트는 마음대로 필드를 정하면 안 되고, 백엔드가 주는/받는 모양을 그대로 맞춰야 한다.
이번에는 백엔드 Java 파일을 읽어서 다음을 확인했다.

- 주소(엔드포인트): `/api/v1/admin/ai/validation-checklists`, `/api/v1/admin/ai/batch-run-logs`
- 보내는 값(요청): 체크리스트 등록은 `checklistType`, `version`, `contentHash`, `status`
- 받는 값(응답): `id`, `status`, `createdAt` 등 정확한 필드 이름
- 고를 수 있는 값(enum): 유형 `EXPLANATION/SIMULATOR/QA`, 상태 `DRAFT/ACTIVE/RETIRED`, 배치 `SUCCEEDED/PARTIAL_FAILED/FAILED`

이렇게 "실제 값"을 확인하고 select 옵션과 타입을 만들면, 화면과 서버가 어긋나지 않는다.

## 3. 권한(Role)은 메뉴와 라우트 두 곳에서 막는다

관리자라고 다 같은 권한이 아니다. 세부 역할(OPERATOR/REVIEWER/CONTENT_CREATOR/SUPER_ADMIN)이 있다.

- 메뉴: `MENU_ITEMS`의 `requiredRoles`로 "보일지 말지"를 정한다.
- 라우트: `RoleGuard`가 "직접 주소로 들어와도" 권한 없으면 403 화면을 보여준다.
- `SUPER_ADMIN`은 우월권으로 항상 통과한다(`canAccessAdminRoute`).

AD-09는 REVIEWER, AD-10은 OPERATOR/REVIEWER로 백엔드 인가 헬퍼(`requireReviewer`/`requireMonitoring`)와 똑같이 맞췄다.

## 4. 하면 안 되는 것(금지 정책)을 화면에서도 지킨다

QT-AI는 AI 산출물의 **원문(검증 전 본문)**을 사용자나 일반 목록에 보여주면 안 된다(CLAUDE.md §7).
그래서 검증·체크리스트·로그 화면은 모두 "메타데이터(버전, 상태, 시각, 건수)"만 보여주고,
AD-03 상세에는 "원문·검증 참조 자료는 정책상 노출하지 않습니다"라고 직접 적어 두었다.

## 5. 공통 도구를 재사용하면 코드가 짧아진다

목록+페이지네이션은 이미 `usePagedList`라는 공통 훅이 있어서, 새 화면도 그대로 가져다 썼다.
백엔드 페이징 응답이 `content/page/size/totalElements` 모양으로 같았기 때문에 가능했다.
"비슷한 일은 공통 함수로 모아두면, 새 화면 만들 때 표 상태 관리를 다시 안 짜도 된다"는 점을 체감했다.

## 6. 빌드는 여러 번 확인한다

타입 오류는 눈으로 안 보이니, `tsc`(타입 검사) + `vite build`(실제 빌드)를 돌려서 확인했다.
이번엔 mount(공유 폴더)에 `node_modules` 설치가 너무 느려서, 소스만 격리 폴더에 복사해 빌드했다.
중요한 건 "내가 쓴 타입과 import가 실제로 컴파일되는지"를 2~3번 확인하는 습관이다.
