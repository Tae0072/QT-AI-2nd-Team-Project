import PagePlaceholder from '../components/PagePlaceholder';

// AD-05 찬양 큐레이션
export default function PraiseSongsPage() {
  return (
    <PagePlaceholder
      code="AD-05"
      title="찬양 큐레이션"
      description="추천 찬양 목록을 등록·수정·숨김 처리합니다. (가사·음원 파일·직접 YouTube URL은 저장하지 않습니다.)"
      roles="OPERATOR"
      endpoints={[
        'GET /api/v1/admin/praise-songs',
        'POST /api/v1/admin/praise-songs',
        'PATCH /api/v1/admin/praise-songs/{id}',
        'POST /api/v1/admin/praise-songs/{id}/hide',
      ]}
    />
  );
}
