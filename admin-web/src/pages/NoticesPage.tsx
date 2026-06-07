import PagePlaceholder from '../components/PagePlaceholder';

// AD-06 시스템 공지
export default function NoticesPage() {
  return (
    <PagePlaceholder
      code="AD-06"
      title="시스템 공지"
      description="앱 공지사항을 등록·수정·발행·숨김 처리합니다."
      roles="OPERATOR"
      endpoints={[
        'GET /api/v1/admin/notices',
        'POST /api/v1/admin/notices',
        'PATCH /api/v1/admin/notices/{id}',
        'POST /api/v1/admin/notices/{id}/publish',
        'POST /api/v1/admin/notices/{id}/hide',
      ]}
    />
  );
}
