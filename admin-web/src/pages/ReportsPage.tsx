import PagePlaceholder from '../components/PagePlaceholder';

// AD-04 신고 처리
export default function ReportsPage() {
  return (
    <PagePlaceholder
      code="AD-04"
      title="신고 처리"
      description="나눔 글·댓글 신고를 검토하고 처리(인정)하거나 반려합니다."
      roles="OPERATOR"
      endpoints={[
        'GET /api/v1/admin/reports',
        'POST /api/v1/admin/reports/{reportId}/resolve',
        'POST /api/v1/admin/reports/{reportId}/reject',
      ]}
    />
  );
}
