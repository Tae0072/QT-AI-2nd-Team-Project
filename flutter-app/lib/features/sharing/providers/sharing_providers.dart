import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/sharing_post_response.dart';
import '../services/sharing_repository.dart';

final sharingRepositoryProvider = Provider<SharingRepository>((ref) {
  return SharingRepository(ref.watch(dioProvider));
});

/// 카테고리 필터 상태.
final sharingCategoryFilterProvider = StateProvider<String?>((ref) => null);

/// 검색어 상태.
final sharingQueryProvider = StateProvider<String?>((ref) => null);

/// 나눔 피드 목록.
final sharingPostsProvider = FutureProvider.autoDispose<SharingPostListResponse>((ref) {
  final repository = ref.watch(sharingRepositoryProvider);
  final category = ref.watch(sharingCategoryFilterProvider);
  final query = ref.watch(sharingQueryProvider);
  return repository.getSharingPosts(category: category, query: query);
});

/// 내 나눔 글 목록 (M-05).
///
/// 왜 이렇게 짰냐면:
/// status 미지정으로 공개+숨김을 한 목록에 받아 상태 뱃지로 구분한다(v1).
/// autoDispose = 화면 떠나면 캐시 정리. 숨김/되돌리기/삭제 후 invalidate로 새로고침한다.
final mySharingPostsProvider =
    FutureProvider.autoDispose<MySharingPostListResponse>((ref) {
  return ref.watch(sharingRepositoryProvider).getMySharingPosts();
});
