import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/sharing/models/sharing_post_response.dart';

void main() {
  // 공통 베이스 JSON(필수 필드). 케이스마다 verseSnapshot만 바꿔 끼운다.
  Map<String, dynamic> baseJson() => {
        'id': 1,
        'nicknameSnapshot': '새벽이슬',
        'titleSnapshot': '제목',
        'category': 'MEDITATION',
        'bodyPreview': '미리보기',
        'likeCount': 3,
        'commentCount': 1,
        'likedByMe': false,
      };

  group('SharingPostItem.fromJson — verseLabel 파싱', () {
    test('verseSnapshot.rangeLabel 있으면 트림해서 설정', () {
      final json = baseJson()..['verseSnapshot'] = {'rangeLabel': '  고전 6:7 '};
      expect(SharingPostItem.fromJson(json).verseLabel, '고전 6:7');
    });

    test('verseSnapshot 키 자체가 없으면 null', () {
      expect(SharingPostItem.fromJson(baseJson()).verseLabel, isNull);
    });

    test('verseSnapshot 은 있지만 rangeLabel 이 null 이면 null', () {
      final json = baseJson()..['verseSnapshot'] = {'rangeLabel': null};
      expect(SharingPostItem.fromJson(json).verseLabel, isNull);
    });

    test('rangeLabel 이 공백뿐이면 null', () {
      final json = baseJson()..['verseSnapshot'] = {'rangeLabel': '   '};
      expect(SharingPostItem.fromJson(json).verseLabel, isNull);
    });
  });

  test('copyWith 는 verseLabel 을 보존한다(좋아요 낙관적 업데이트 시 누락 방지)', () {
    final json = baseJson()..['verseSnapshot'] = {'rangeLabel': '창 1:1-5'};
    final item = SharingPostItem.fromJson(json);

    final copy = item.copyWith(likedByMe: true, likeCount: 4);

    expect(copy.verseLabel, '창 1:1-5');
    expect(copy.likedByMe, isTrue);
    expect(copy.likeCount, 4);
    // 변경하지 않은 필드는 유지
    expect(copy.titleSnapshot, '제목');
    expect(copy.commentCount, 1);
  });
}
