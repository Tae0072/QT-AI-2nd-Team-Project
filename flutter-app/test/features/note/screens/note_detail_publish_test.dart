import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/note_detail_screen.dart';
import 'package:qtai_app/features/sharing/providers/sharing_providers.dart';
import 'package:qtai_app/features/sharing/services/sharing_repository.dart';

/// publishNote 호출 여부/인자를 기록하고, 필요 시 예외를 던지는 가짜 레포.
class _FakeSharingRepository extends SharingRepository {
  _FakeSharingRepository({this.error}) : super(Dio());

  /// null이면 정상 완료, 값이 있으면 publishNote에서 그 예외를 던진다.
  final Object? error;
  int publishCallCount = 0;
  int? lastNoteId;
  bool? lastCommentsEnabled;

  @override
  Future<void> publishNote(int noteId, {bool commentsEnabled = true}) async {
    publishCallCount++;
    lastNoteId = noteId;
    lastCommentsEnabled = commentsEnabled;
    if (error != null) throw error!;
  }
}

NoteDetail _detail({required String status, bool shared = false}) => NoteDetail(
      id: 7,
      category: 'PRAYER', // 자유노트 — 공개 버튼은 카테고리 무관
      title: '테스트 노트',
      body: '본문 내용',
      status: status,
      visibility: 'PRIVATE',
      shared: shared,
      verses: const [],
    );

/// 노트 상세 화면을 가짜 provider로 띄운다.
Future<_FakeSharingRepository> _pump(
  WidgetTester tester, {
  required String status,
  bool shared = false,
  Object? publishError,
}) async {
  final fakeRepo = _FakeSharingRepository(error: publishError);
  final detail = _detail(status: status, shared: shared);

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        // 상세는 family라 해당 noteId 인스턴스만 가짜 데이터로 교체.
        noteDetailProvider(detail.id).overrideWith((ref) async => detail),
        sharingRepositoryProvider.overrideWithValue(fakeRepo),
      ],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        home: NoteDetailScreen(noteId: detail.id),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return fakeRepo;
}

void main() {
  // 경로 1: SAVED 노트 → 공개 → 시트에서 [공개] → publishNote 호출 + 성공 안내
  testWidgets('SAVED 노트를 공개하면 publishNote가 호출되고 성공 안내가 뜬다',
      (tester) async {
    final repo = await _pump(tester, status: 'SAVED');

    await tester.tap(find.byTooltip('나눔 공개'));
    await tester.pumpAndSettle();
    expect(find.text('닉네임 나눔에 공개'), findsOneWidget); // 시트 제목

    await tester.tap(find.text('공개')); // 확정 버튼
    await tester.pumpAndSettle();

    expect(repo.publishCallCount, 1);
    expect(repo.lastNoteId, 7);
    expect(find.text('나눔에 공개되었습니다'), findsOneWidget);
  });

  // 경로 2: 시트에서 [취소] → publishNote 호출 안 됨
  testWidgets('공개 시트에서 취소하면 publishNote를 호출하지 않는다', (tester) async {
    final repo = await _pump(tester, status: 'SAVED');

    await tester.tap(find.byTooltip('나눔 공개'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('취소'));
    await tester.pumpAndSettle();

    expect(repo.publishCallCount, 0);
  });

  // 경로 3: DRAFT 노트 → 시트 안 뜨고 안내만, 호출 안 됨
  testWidgets('DRAFT 노트는 공개 시트를 열지 않고 저장 안내만 띄운다', (tester) async {
    final repo = await _pump(tester, status: 'DRAFT');

    await tester.tap(find.byTooltip('나눔 공개'));
    await tester.pumpAndSettle();

    expect(find.text('닉네임 나눔에 공개'), findsNothing); // 시트 안 뜸
    expect(find.text('저장을 완료한 노트만 공개할 수 있어요'), findsOneWidget);
    expect(repo.publishCallCount, 0);
  });

  // 경로 4: publishNote 일반 실패 → 실패 안내
  testWidgets('publishNote가 실패하면 실패 안내가 뜬다', (tester) async {
    await _pump(tester, status: 'SAVED', publishError: Exception('boom'));

    await tester.tap(find.byTooltip('나눔 공개'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('공개'));
    await tester.pumpAndSettle();

    expect(find.text('나눔 공개에 실패했습니다. 다시 시도해 주세요'), findsOneWidget);
  });

  // 경로 5: 409(이미 공개됨) → 실패가 아니라 "이미 공개됨" 안내 (클라이언트 임시 가드)
  testWidgets('이미 공개된 노트(409)는 실패가 아니라 이미 공개됨 안내가 뜬다',
      (tester) async {
    final dioError = DioException(
      requestOptions: RequestOptions(path: '/notes/7/share'),
      response: Response(
        requestOptions: RequestOptions(path: '/notes/7/share'),
        statusCode: 409,
      ),
      type: DioExceptionType.badResponse,
    );
    await _pump(tester, status: 'SAVED', publishError: dioError);

    await tester.tap(find.byTooltip('나눔 공개'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('공개'));
    await tester.pumpAndSettle();

    expect(find.text('이미 나눔에 공개된 노트예요'), findsOneWidget);
    expect(find.text('나눔 공개에 실패했습니다. 다시 시도해 주세요'), findsNothing);
  });
}
