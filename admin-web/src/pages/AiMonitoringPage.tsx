import PagePlaceholder from '../components/PagePlaceholder';

// AD-08 AI 운영 모니터링
export default function AiMonitoringPage() {
  return (
    <PagePlaceholder
      code="AD-08"
      title="AI 운영 모니터링"
      description="AI 작업의 실패율·대기 건수·차단 건수 등 운영 집계를 봅니다. (산출물 원문 조회는 AD-03에서만 가능합니다.)"
      roles="OPERATOR (집계만)"
      endpoints={['GET /api/v1/admin/ai/monitoring']}
    />
  );
}
