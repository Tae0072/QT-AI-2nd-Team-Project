import PagePlaceholder from '../components/PagePlaceholder';

// AD-03 AI 산출물 검증
export default function AiAssetsPage() {
  return (
    <PagePlaceholder
      code="AD-03"
      title="AI 산출물 검증"
      description="AI가 생성한 해설/구절 산출물을 검토하고 승인·반려·숨김·재생성합니다. 승인 전 원문은 일반 목록에 노출하지 않습니다."
      roles="REVIEWER / SUPER_ADMIN"
      endpoints={[
        'GET /api/v1/admin/ai/assets',
        'GET /api/v1/admin/ai/assets/{assetId}',
        'POST /api/v1/admin/ai/assets/{assetId}/approve',
        'POST /api/v1/admin/ai/assets/{assetId}/reject',
        'POST /api/v1/admin/ai/assets/{assetId}/hide',
        'POST /api/v1/admin/ai/assets/{assetId}/regenerate',
      ]}
    />
  );
}
