import PagePlaceholder from '../components/PagePlaceholder';

// AD-02 오늘 QT 관리
export default function QtPassagesPage() {
  return (
    <PagePlaceholder
      code="AD-02"
      title="오늘 QT 관리"
      description="QT 본문을 등록·수정·게시·숨김 처리합니다. (공개 00:00 KST / 사용자 노출 04:00 KST)"
      roles="OPERATOR"
      endpoints={[
        'GET /api/v1/admin/qt-passages',
        'POST /api/v1/admin/qt-passages',
        'PATCH /api/v1/admin/qt-passages/{id}',
        'POST /api/v1/admin/qt-passages/{id}/publish',
        'POST /api/v1/admin/qt-passages/{id}/hide',
      ]}
    />
  );
}
