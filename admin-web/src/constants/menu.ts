import type { AdminRole } from './roles';
import { ADMIN_ROLES } from './roles';

// ===== 사이드바 메뉴 + 라우팅에 함께 쓰는 메뉴 정의 =====
// 각 항목은 화면 코드(AD-xx), 주소(path), 한글 라벨, 접근 가능 권한을 가진다.
// requiredRoles 가 빈 배열이면 'ADMIN 이면 누구나'(세부 역할 무관) 접근 가능으로 본다.
// SUPER_ADMIN 은 우월권으로 모든 화면에 접근 가능하므로 목록에 명시하지 않는다.
export interface MenuItem {
  code: string; // 화면 코드 (예: AD-01)
  path: string; // 주소 (예: /dashboard)
  label: string; // 메뉴에 보이는 이름
  requiredRoles: AdminRole[]; // 접근 가능한 세부 권한 (빈 배열이면 ADMIN 공통)
}

// 권한 기준(2026-06-08 D1 확정): 백엔드 컨트롤러의 실제 인가(enforce)를 기준으로 한다.
//  - AD-03 AI검증     : REVIEWER            (AdminAiAuthentication.requireReviewer)
//  - AD-04 신고처리    : OPERATOR            (AdminReportController.requireOperator)
//  - AD-05 찬양       : ADMIN 공통          (PraiseController @hasRole('ADMIN'))
//  - AD-07 감사로그    : OPERATOR/REVIEWER   (AdminAuditAuthentication.requireAudit)
//  - AD-08 AI모니터링  : OPERATOR/REVIEWER   (AdminAiAuthentication.requireMonitoring)
//  - AD-01 대시보드 / AD-02 오늘QT관리 / AD-06 시스템공지: 백엔드 미구현(E단계) → 명세 추정값, 백엔드 확정 시 갱신.
export const MENU_ITEMS: MenuItem[] = [
  { code: 'AD-01', path: '/dashboard', label: '대시보드', requiredRoles: [] },
  {
    code: 'AD-02',
    path: '/qt-passages',
    label: '오늘 QT 관리',
    requiredRoles: [ADMIN_ROLES.OPERATOR],
  },
  {
    code: 'AD-03',
    path: '/ai-assets',
    label: 'AI 산출물 검증',
    requiredRoles: [ADMIN_ROLES.REVIEWER],
  },
  {
    code: 'AD-04',
    path: '/reports',
    label: '신고 처리',
    requiredRoles: [ADMIN_ROLES.OPERATOR],
  },
  {
    code: 'AD-05',
    path: '/praise-songs',
    label: '찬양 큐레이션',
    requiredRoles: [],
  },
  {
    code: 'AD-06',
    path: '/notices',
    label: '시스템 공지',
    requiredRoles: [ADMIN_ROLES.OPERATOR],
  },
  {
    code: 'AD-07',
    path: '/audit-logs',
    label: '감사 로그',
    requiredRoles: [ADMIN_ROLES.OPERATOR, ADMIN_ROLES.REVIEWER],
  },
  {
    code: 'AD-08',
    path: '/ai-monitoring',
    label: 'AI 운영 모니터링',
    requiredRoles: [ADMIN_ROLES.OPERATOR, ADMIN_ROLES.REVIEWER],
  },
];
