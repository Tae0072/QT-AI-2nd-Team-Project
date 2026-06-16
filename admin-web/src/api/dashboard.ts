import { apiClient, unwrap } from './client';
import type { ApiResponse } from './types';

// ===== AD-01 관리자 대시보드 =====
// 연결 API (권한: ADMIN + OPERATOR/REVIEWER/SUPER_ADMIN)
//   GET /api/v1/admin/dashboard
// 화면: 운영 요약 지표(검증 대기·신고 건수) + 오늘 QT 상태 + 최근 감사 로그
//
// 타입은 백엔드 AdminDashboardResponse(admin-server)를 1:1 미러한다.
// 근거: 04_API_명세서 AD-01 / DevC_강상민 2026-06-10_admin-dashboard-api_report.md

// 오늘 QT 가용 상태. todayQt 는 항상 non-null이며, 없으면 status=MISSING + 일부 필드 null.
export type TodayQtStatus = 'READY' | 'MISSING';

export interface TodayQt {
  qtDate: string; // KST 기준 날짜(MISSING 이어도 오늘 날짜)
  qtPassageId: number | null;
  title: string | null;
  status: TodayQtStatus;
  simulatorStatus: string | null; // QT 가용 상태와 별개(READY/MISSING/FAILED/DISABLED 등)
  hasExplanation: boolean;
  cacheStatus: string | null; // STALE_FALLBACK 등 운영 참고
}

// 대시보드 전용 sanitized 감사 로그(민감 컬럼 beforeJson/afterJson 등 제외).
export interface RecentAuditLog {
  id: number;
  adminUserId: number | null;
  actorType: string;
  actionType: string;
  targetType: string | null;
  targetId: number | null;
  createdAt: string; // ISO(OffsetDateTime)
}

export interface DashboardSummary {
  pendingAiValidationCount: number;
  receivedReportCount: number;
  reviewingReportCount: number;
  todayQt: TodayQt;
  recentAuditLogs: RecentAuditLog[];
}

export function getDashboard() {
  return unwrap<DashboardSummary>(
    apiClient.get<ApiResponse<DashboardSummary>>('/admin/dashboard'),
  );
}
