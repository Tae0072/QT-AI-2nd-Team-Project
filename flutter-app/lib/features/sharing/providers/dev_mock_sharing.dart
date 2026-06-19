import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../mypage/providers/mypage_providers.dart';
import '../models/sharing_post_response.dart';

/// ⚠️ 테스트(목업) 전용 — 운영 배포 전 제거 대상.
///
/// 목적: '내 계정'에서만 나눔(내 글) 목록에 가짜 테스트 글 2개가 보이게 한다.
/// 안전장치(운영에 절대 안 섞이도록):
///   1) [kDebugMode] 가 아닐 때(릴리스)는 분기 자체가 컴파일에서 제거된다.
///   2) 로그인 계정 이메일이 [kMockSharingAccountEmail] 과 정확히 같을 때만 끼운다.
///   3) DB를 건드리지 않는다(메모리 목록 앞에 덧붙이기만) → 공개 피드/운영 0 오염.
///   4) id는 음수(실제와 충돌 불가), 제목에 '[목업]' 표시.
const String kMockSharingAccountEmail = 'rkdxodh41@gmail.com';

/// 나눔(내 글) 목록에 끼울 가짜 글 2개(테스트용).
List<MySharingPostItem> debugMockMySharingItems() {
  final now = DateTime.now();
  return [
    MySharingPostItem(
      id: -9101,
      titleSnapshot: '[목업] 나눔 글 1 (테스트)',
      category: 'GRATITUDE',
      status: 'PUBLISHED',
      commentsEnabled: true,
      likeCount: 0,
      commentCount: 0,
      publishedAt: now,
    ),
    MySharingPostItem(
      id: -9102,
      titleSnapshot: '[목업] 나눔 글 2 (테스트)',
      category: 'PRAYER',
      status: 'PUBLISHED',
      commentsEnabled: true,
      likeCount: 0,
      commentCount: 0,
      publishedAt: now,
    ),
  ];
}

/// 디버그 + 내 계정일 때만 [base] 목록 맨 앞에 목업 2개를 끼워 돌려준다.
/// 그 외(릴리스, 다른 계정, 프로필 조회 실패)에는 [base] 를 그대로 돌려준다.
Future<MySharingPostListResponse> withDebugMockMySharing(
    Ref ref, MySharingPostListResponse base) async {
  if (!kDebugMode) return base; // 릴리스: 아래 코드는 트리 셰이킹으로 제거됨.
  try {
    final profile = await ref.read(profileProvider.future);
    if (profile.email != kMockSharingAccountEmail) return base;
  } catch (_) {
    return base; // 프로필을 못 읽으면 원본 그대로(앱 안전).
  }
  return MySharingPostListResponse(
    items: [...debugMockMySharingItems(), ...base.items],
    hasNext: base.hasNext,
  );
}
