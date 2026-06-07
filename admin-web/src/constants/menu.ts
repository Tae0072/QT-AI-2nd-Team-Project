import type { AdminRole } from './roles';
import { ADMIN_ROLES } from './roles';

// ===== 사이드바 메뉴 + 라우팅에 함께 쓰는 메뉴 정의 =====
// 각 항목은 화면 코드(AD-xx), 주소(path), 한글 라벨, 접근 가능 권한을 가진다.
// requiredRoles 가 빈 배열이면 'ADMIN 이면 누구나' 접근 가능으로 본다.
export interface MenuItem {
  code: string; // 화면 코드 (예: AD-01)
  path: string; // 주소 (예: /dashboard)
  label: string; // 메뉴에 보이는 이름
  requiredRoles: AdminRole[]; // 접근 가능한 세부 권한 (빈 배열이면 ADMIN 공통)
}

// TODO: requiredRoles 일부는 추정값이다. 04_API_명세서 §AD-01~08 권한표 기준으로 최종 확정한다.
//       (현재 확인: AD-02/04 OPERATOR, AD-03 REVIEWER·SUPER_ADMIN, AD-08 OPERATOR 집계)
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
    requiredRoles: [ADMIN_ROLES.REVIEWER, ADMIN_ROLES.SUPER_ADMIN],
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
    requiredRoles: [ADMIN_ROLES.OPERATOR],
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
    requiredRoles: [ADMIN_ROLES.SUPER_ADMIN],
  },
  {
    code: 'AD-08',
    path: '/ai-monitoring',
    label: 'AI 운영 모니터링',
    requiredRoles: [ADMIN_ROLES.OPERATOR],
  },
];
