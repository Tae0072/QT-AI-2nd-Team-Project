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
