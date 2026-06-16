# 리포트 — 기능 테스트(AD-18)에 '신고 처리 테스트' 추가

- 일자: 2026-06-14
- 작성: Claude (Lead T 요청)
- 관련 워크플로우: `workflows/2026-06-14_selftest-report-handling.md`

## 한 줄 요약
신고 목록이 비어 신고 처리(AD-04) 흐름을 점검할 수 없던 문제를 해결하기 위해, 기능 테스트(자가 진단) 화면에 **테스트 신고 생성 → 처리/반려** 시연 기능을 추가했다.

## 변경 파일
| 파일 | 구분 | 내용 |
|---|---|---|
| `admin-server/.../report/web/AdminReportController.java` | 수정 | `POST /test-seed`(CreateReportUseCase 주입) |
| `admin-server/.../report/web/AdminReportControllerTest.java` | 신규 | test-seed 201/401/403 |
| `admin-web/src/api/reports.ts` | 수정 | `seedTestReport()` + 타입 |
| `admin-web/src/pages/SelfTestPage.tsx` | 수정 | '신고 처리 테스트(쓰기)' 카드 |

## 사용 방법
1. 좌측 메뉴 '기능 테스트(자가 진단)' 진입.
2. 아래 '신고 처리 테스트' 카드에서 **① 테스트 신고 생성** → 신고 #N(RECEIVED) 생성.
3. **② 처리(인정)** 또는 **반려** → 상태가 RESOLVED/REJECTED로 바뀌는지 확인.
4. **신고 처리 화면에서 보기** → /reports에서 동일 신고 확인.

## 안전성
- 대상 COMMENT + 매번 고유 targetId(중복 차단 회피) + 사유 TEST. 존재하는 실제 게시물/댓글을 건드리지 않음.
- 처리/반려는 상태 전이 + 처리기록 + 감사로그까지(현재 구현 범위). HIDE_TARGET(게시물 숨김)은 POST+action 지정 시만 동작하므로 테스트(COMMENT)에선 발생 안 함.
- 처리 시 `notifyReporter=false`로 신고자(=관리자 본인) 실제 알림 미발송.
- 권한: OPERATOR/SUPER_ADMIN(ROLE_ADMIN + admin_users 2차 검증).

## 검증 상태
- 코드 대조 검증(샌드박스 빌드 불가). 신규 컨트롤러 테스트 3케이스.
- **PR 전 T님 PC 필수**: `./gradlew -p qtai-server :admin-server:build :admin-server:test` · `cd admin-web && npm run build && npm run test`.

## 리스크 / 메모
- test-seed는 실(테스트) 데이터를 생성. 정리(삭제) 기능은 없음(신고 hard-delete UseCase 부재) → 처리/반려로 종결만. 누적 시 수동 정리 필요.
- 운영 노출 차단(프로파일/플래그)은 후속 검토. 현재 OPERATOR 권한으로만 보호.
