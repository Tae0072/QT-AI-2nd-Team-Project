import PagePlaceholder from '../components/PagePlaceholder';

// AD-01 관리자 대시보드
export default function DashboardPage() {
  return (
    <PagePlaceholder
      code="AD-01"
      title="대시보드"
      description="운영 요약 지표(대기 중 검증 건수, 신고 건수, 오늘 QT 상태 등)를 한눈에 봅니다."
      roles="ADMIN"
      endpoints={['GET /api/v1/admin/dashboard']}
    />
  );
}
