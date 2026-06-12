import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/sharing/models/sharing_post_response.dart';
import 'package:qtai_app/features/sharing/widgets/post_card.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        home: Scaffold(body: child),
      );

  testWidgets('작성자·제목·본문·좋아요/댓글·상대시간을 표시하고 탭을 전달한다',
      (tester) async {
    var tapped = false;
    final item = SharingPostItem(
      id: 1,
      nicknameSnapshot: '새벽이슬',
      titleSnapshot: '이기는 것보다 회복이 먼저',
      category: 'MEDITATION',
      bodyPreview: '오늘 본문을 읽으며 갈등이 떠올랐다',
      likeCount: 12,
      commentCount: 4,
      likedByMe: false,
      publishedAt: DateTime.now().subtract(const Duration(hours: 2)),
    );

    await tester.pumpWidget(
        wrap(PostCard(item: item, onTap: () => tapped = true)));
    await tester.pumpAndSettle();

    expect(find.text('새벽이슬'), findsOneWidget);
    expect(find.text('이기는 것보다 회복이 먼저'), findsOneWidget);
    expect(find.text('오늘 본문을 읽으며 갈등이 떠올랐다'), findsOneWidget);
    expect(find.text('12'), findsOneWidget);
    expect(find.text('4'), findsOneWidget);
    expect(find.text('2시간 전'), findsOneWidget);

    await tester.tap(find.byType(PostCard));
    expect(tapped, isTrue);
  });

  testWidgets('좋아요한 글은 채워진 하트, 안 한 글은 빈 하트', (tester) async {
    final liked = SharingPostItem(
      id: 1,
      nicknameSnapshot: 'a',
      titleSnapshot: 't',
      category: 'PRAYER',
      bodyPreview: '',
      likeCount: 1,
      commentCount: 0,
      likedByMe: true,
    );
    await tester.pumpWidget(wrap(PostCard(item: liked, onTap: () {})));
    expect(find.byIcon(Icons.favorite), findsOneWidget);
    expect(find.byIcon(Icons.favorite_border), findsNothing);
  });

  testWidgets('긴 닉네임이어도 좁은 폭에서 오버플로하지 않는다', (tester) async {
    final item = SharingPostItem(
      id: 2,
      nicknameSnapshot: '아주아주아주아주아주아주아주아주긴닉네임입니다정말로깁니다그래도',
      titleSnapshot: '제목',
      category: 'PRAYER',
      bodyPreview: '',
      likeCount: 0,
      commentCount: 0,
      likedByMe: false,
      publishedAt: DateTime.now().subtract(const Duration(hours: 1)),
    );

    await tester.pumpWidget(wrap(
      SizedBox(width: 220, child: PostCard(item: item, onTap: () {})),
    ));
    await tester.pumpAndSettle();

    // 오버플로 예외 없이 렌더되면 통과.
    expect(tester.takeException(), isNull);
  });
}
