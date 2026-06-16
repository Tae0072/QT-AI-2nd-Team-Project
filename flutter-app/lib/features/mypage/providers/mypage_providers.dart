import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/dashboard_response.dart';
import '../models/member_response.dart';
import '../models/notification_response.dart';
import '../models/settings_response.dart';
import '../services/mypage_repository.dart';

// ── Repository Provider ──

final myPageRepositoryProvider = Provider<MyPageRepository>((ref) {
  return MyPageRepository(ref.watch(dioProvider));
});

// ── 대시보드 ──

/// 대시보드 데이터 (프로필·통계·알림·찬양).
///
/// autoDispose: 화면 벗어나면 캐시 해제.
final dashboardProvider =
    AutoDisposeFutureProvider<DashboardResponse>((ref) async {
  final repository = ref.watch(myPageRepositoryProvider);
  return repository.getDashboard();
});

// ── 프로필 ──

/// 내 프로필 상세.
final profileProvider =
    AutoDisposeFutureProvider<MemberResponse>((ref) async {
  final repository = ref.watch(myPageRepositoryProvider);
  return repository.getProfile();
});

// ── 닉네임 중복 검사 ──

/// 닉네임 중복 검사 상태.
///
/// [nicknameQueryProvider]에 값을 쓰면 디바운스 300ms 후 API를 호출한다.
/// 빈 문자열이면 검사하지 않는다.
final nicknameQueryProvider = StateProvider.autoDispose<String>((ref) => '');

final nicknameAvailableProvider =
    AutoDisposeFutureProvider<bool?>((ref) async {
  final query = ref.watch(nicknameQueryProvider);
  if (query.isEmpty || query.length < 2) return null;

  // 디바운스 300ms
  await Future<void>.delayed(const Duration(milliseconds: 300));

  // 디바운스 중 새 값이 들어오면 이전 호출 취소
  if (ref.read(nicknameQueryProvider) != query) return null;

  final repository = ref.watch(myPageRepositoryProvider);
  return repository.checkNicknameAvailable(query);
});

// ── 알림 ──

/// 미읽음 필터 상태.
final unreadOnlyFilterProvider = StateProvider<bool>((ref) => false);

/// 알림 목록.
final notificationsProvider = FutureProvider.autoDispose<NotificationListResponse>((ref) {
  final repository = ref.watch(myPageRepositoryProvider);
  final unreadOnly = ref.watch(unreadOnlyFilterProvider);
  return repository.getNotifications(unreadOnly: unreadOnly);
});

// ── 설정 ──

/// 사용자 설정.
final settingsProvider = FutureProvider.autoDispose<SettingsData>((ref) {
  final repository = ref.watch(myPageRepositoryProvider);
  return repository.getSettings();
});
