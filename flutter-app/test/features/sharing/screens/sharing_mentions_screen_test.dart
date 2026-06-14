import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/sharing/models/sharing_post_response.dart';
import 'package:qtai_app/features/sharing/providers/sharing_providers.dart';
import 'package:qtai_app/features/sharing/screens/sharing_mentions_screen.dart';

Widget _wrap(List<Override> overrides) => ProviderScope(
      overrides: overrides,
      child: const MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: Locale('ko'),
        home: SharingMentionsScreen(),
      ),
    );

SharingPostItem _item(int id, String title) => SharingPostItem(
      id: id,
      nicknameSnapshot: '닉',
      titleSnapshot: title,
      category: 'MEDITATION',
      bodyPreview: '본문',
      likeCount: 0,
      commentCount: 0,
      likedByMe: false,
    );

void main() {
  testWidgets('태그된 글이 없으면 빈 안내를 보여준다', (tester) async {
    await tester.pumpWidget(_wrap([
      mentionsProvider.overrideWith(
          (ref) async => SharingPostListResponse(items: const [], hasNext: false)),
    ]));
    await tester.pumpAndSettle();

    expect(find.textContaining('아직 나를 태그한 글이 없어요'), findsOneWidget);
  });

  testWidgets('태그된 글 목록을 카드로 보여준다', (tester) async {
    await tester.pumpWidget(_wrap([
      mentionsProvider.overrideWith((ref) async => SharingPostListResponse(
            items: [_item(1, '첫 태그 글'), _item(2, '둘째 태그 글')],
            hasNext: false,
          )),
    ]));
    await tester.pumpAndSettle();

    expect(find.text('첫 태그 글'), findsOneWidget);
    expect(find.text('둘째 태그 글'), findsOneWidget);
  });
}
