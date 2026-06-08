/// 나눔/노트 카테고리 코드 → 한글 라벨 매핑 (④ 컴포넌트화: 중복 제거).
///
/// 동일한 `switch (category)` 라벨 매핑이 여러 화면(my_sharing, sharing_detail 등)에
/// 복사돼 있던 것을 한 곳으로 모은다. 카테고리가 늘거나 라벨이 바뀌면 여기만 고친다.
/// (다국어 전환은 후속 — 현재는 한국어 단일 라벨)
String postCategoryLabel(String code) {
  switch (code) {
    case 'MEDITATION':
      return '묵상';
    case 'SERMON':
      return '설교';
    case 'PRAYER':
      return '기도';
    case 'GRATITUDE':
      return '감사';
    case 'REPENTANCE':
      return '회개';
    default:
      return code;
  }
}
