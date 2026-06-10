import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/widgets/qt_video_player.dart';

void main() {
  test('qtVideoCacheKey sanitizes unsafe filename characters', () {
    final key = qtVideoCacheKey(
      7,
      'https://cdn.example.com/videos/video@bad.mp4',
    );

    expect(key, 'qt-video-7-video_bad.mp4');
  });

  testWidgets('QtVideoSection does not render a player when status is not READY', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          qtVideoClipProvider(7).overrideWith((ref) async => const QtVideoClip(
                status: 'MISSING',
                clipId: null,
                qtPassageId: 7,
                title: null,
                videoUrl: null,
                sourceVideoId: null,
                startTimeSec: null,
                endTimeSec: null,
                compositionType: null,
                clipStatus: null,
              )),
        ],
        child: const MaterialApp(
          home: Scaffold(body: QtVideoSection(qtPassageId: 7)),
        ),
      ),
    );
    await tester.pump();

    expect(find.byType(QtVideoPlayer), findsNothing);
  });

  testWidgets('QtVideoSection renders a player when status is READY and URL exists', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          qtVideoClipProvider(7).overrideWith((ref) async => const QtVideoClip(
                status: 'READY',
                clipId: 10,
                qtPassageId: 7,
                title: 'QT video',
                videoUrl: 'https://cdn.example.com/videos/qt.mp4',
                sourceVideoId: 1,
                startTimeSec: 0,
                endTimeSec: 90,
                compositionType: 'SINGLE_CUT',
                clipStatus: 'APPROVED',
              )),
        ],
        child: const MaterialApp(
          home: Scaffold(body: QtVideoSection(qtPassageId: 7)),
        ),
      ),
    );
    await tester.pump();

    expect(find.byType(QtVideoPlayer), findsOneWidget);
  });
}
