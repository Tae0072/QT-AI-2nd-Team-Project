// 관리자 세부 권한(admin_role) 상수.
// 기준: 03_아키텍처_정의서 / 04_API_명세서.
// 인증 시 회원 토큰의 ADMIN 역할(members.role=ADMIN)과 아래 세부 권한(admin_users.admin_role)을
// 함께 확인한다. (CLAUDE.md §5)
// 프런트 역할 체크는 메뉴/라우트 노출 제어용이며, API 최종 인가는 백엔드가 담당한다.
export const ADMIN_ROLES = {
  OPERATOR: 'OPERATOR', // 운영: 대시보드, QT/신고/찬양/공지, 감사·AI 모니터링 조회
  REVIEWER: 'REVIEWER', // 검증: 대시보드, AI 산출물 검증, 감사·AI 모니터링 조회
  CONTENT_CREATOR: 'CONTENT_CREATOR', // 콘텐츠 작성: 평가 셋/케이스 작성 등 별도 계약 영역
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
