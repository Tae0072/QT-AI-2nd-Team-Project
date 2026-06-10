// 관리자 세부 권한(admin_role) 상수.
// 기준: 03_아키텍처_정의서 / 04_API_명세서.
// 인증 시 회원 토큰의 ADMIN 역할(members.role=ADMIN)과 아래 세부 권한(admin_users.admin_role)을
// 함께 확인한다. (CLAUDE.md §5)
export const ADMIN_ROLES = {
  OPERATOR: 'OPERATOR', // 운영: QT 관리, 신고 처리, AI 모니터링 집계
  REVIEWER: 'REVIEWER', // 검증: AI 산출물 승인/반려
  CONTENT_CREATOR: 'CONTENT_CREATOR', // 콘텐츠 작성
  SUPER_ADMIN: 'SUPER_ADMIN', // 최고 관리자: 전체 권한
} as const;

// 위 값들의 문자열 유니온 타입 ('OPERATOR' | 'REVIEWER' | ...)
export type AdminRole = (typeof ADMIN_ROLES)[keyof typeof ADMIN_ROLES];

// 화면에 보여줄 한글 이름
export const ADMIN_ROLE_LABELS: Record<AdminRole, string> = {
  OPERATOR: '운영자',
  REVIEWER: '검증자',
  CONTENT_CREATOR: '콘텐츠 작성자',
  SUPER_ADMIN: '최고 관리자',
};

export function canAccessAdminRoute(
  adminRole: AdminRole | null | undefined,
  requiredRoles: AdminRole[],
): boolean {
  if (!adminRole) return false;
  if (adminRole === ADMIN_ROLES.SUPER_ADMIN) return true;
  if (requiredRoles.length === 0) return true;
  return requiredRoles.includes(adminRole);
}
