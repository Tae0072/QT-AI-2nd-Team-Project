import PagePlaceholder from '../components/PagePlaceholder';

// AD-07 감사 로그
export default function AuditLogsPage() {
  return (
    <PagePlaceholder
      code="AD-07"
      title="감사 로그"
      description="관리자/시스템(SYSTEM_BATCH)의 주요 행위 기록을 조회·필터링합니다."
      roles="SUPER_ADMIN"
      endpoints={['GET /api/v1/admin/audit-logs']}
    />
  );
}
