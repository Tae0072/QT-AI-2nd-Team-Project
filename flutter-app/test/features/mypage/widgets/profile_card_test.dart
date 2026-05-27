import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/mypage/models/dashboard_response.dart';
import 'package:qtai_app/features/mypage/widgets/profile_card.dart';

void main() {
  group('ProfileCard', () {
    testWidgets('닉네임과 프로필 보기 텍스트 표시', (tester) async {
      const profile = ProfileSummary(memberId: 1, nickname: '테스트유저');

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ProfileCard(profile: profile),
          ),
        ),
      );

      expect(find.text('테스트유저'), findsOneWidget);
      expect(find.text('프로필 보기'), findsOneWidget);
    });

    testWidgets('이미지 없으면 기본 아이콘 표시', (tester) async {
      const profile = ProfileSummary(memberId: 1, nickname: '테스트유저');

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ProfileCard(profile: profile),
          ),
        ),
      );

      expect(find.byIcon(Icons.person), findsOneWidget);
    });

    testWidgets('탭 시 onTap 콜백 호출', (tester) async {
      const profile = ProfileSummary(memberId: 1, nickname: '테스트유저');
      var tapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProfileCard(
              profile: profile,
              onTap: () => tapped = true,
            ),
          ),
        ),
      );

      await tester.tap(find.byType(ProfileCard));
      expect(tapped, isTrue);
    });
  });
}
