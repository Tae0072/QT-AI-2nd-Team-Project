import 'package:dio/dio.dart';

import '../models/dashboard_response.dart';
import '../models/member_response.dart';
import '../models/notification_response.dart';
import '../models/praise_response.dart';
import '../models/settings_response.dart';

/// 마이페이지 API 호출 레이어.
///
/// Dio 인스턴스를 주입받아 `/me/**` 엔드포인트를 호출하고,
/// JSON 응답을 모델 클래스로 변환한다.
class MyPageRepository {
  final Dio _dio;

  MyPageRepository(this._dio);

  // ── 대시보드 ──

  /// 대시보드 위젯 데이터 조회.
  ///
  /// GET /me/dashboard → [DashboardResponse]
  Future<DashboardResponse> getDashboard() async {
    final response = await _dio.get('/me/dashboard');
    final data = response.data['data'] as Map<String, dynamic>;
    return DashboardResponse.fromJson(data);
  }

  // ── 프로필 ──

  /// 내 프로필 조회.
  ///
  /// GET /me → [MemberResponse]
  Future<MemberResponse> getProfile() async {
    final response = await _dio.get('/me');
    final data = response.data['data'] as Map<String, dynamic>;
    return MemberResponse.fromJson(data);
  }

  /// 프로필 부분 수정 (닉네임, 프로필 이미지).
  ///
  /// PATCH /me → [MemberResponse]
  Future<MemberResponse> updateProfile({
    String? nickname,
    String? profileImageUrl,
  }) async {
    final body = <String, dynamic>{};
    if (nickname != null) body['nickname'] = nickname;
    if (profileImageUrl != null) body['profileImageUrl'] = profileImageUrl;

    final response = await _dio.patch('/me', data: body);
    final data = response.data['data'] as Map<String, dynamic>;
    return MemberResponse.fromJson(data);
  }

  // ── 닉네임 ──

  /// 닉네임 변경 (7일 잠금 적용).
  ///
  /// PATCH /me/nickname → [MemberResponse]
  Future<MemberResponse> changeNickname(String nickname) async {
    final response = await _dio.patch(
      '/me/nickname',
      data: {'nickname': nickname},
    );
    final data = response.data['data'] as Map<String, dynamic>;
    return MemberResponse.fromJson(data);
  }

  /// 닉네임 중복 확인.
  ///
  /// GET /me/nickname/available?nickname={nickname} → bool
  Future<bool> checkNicknameAvailable(String nickname) async {
    final response = await _dio.get(
      '/me/nickname/available',
      queryParameters: {'nickname': nickname},
    );
    return response.data['data'] as bool;
  }

  // ── 알림 ──

  /// 알림 목록 조회.
  Future<NotificationListResponse> getNotifications({bool? unreadOnly, int page = 0}) async {
    final response = await _dio.get('/notifications', queryParameters: {
      if (unreadOnly == true) 'unreadOnly': true,
      'page': page,
      'size': 20,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return NotificationListResponse.fromJson(data);
  }

  /// 개별 알림 읽음 처리.
  Future<void> markNotificationRead(int notificationId) async {
    await _dio.patch('/notifications/$notificationId/read');
  }

  /// 전체 알림 읽음 처리.
  Future<void> markAllNotificationsRead() async {
    await _dio.patch('/notifications/read-all');
  }

  // ── 설정 ──

  /// 설정 조회.
  Future<SettingsData> getSettings() async {
    final response = await _dio.get('/me/settings');
    final data = response.data['data'] as Map<String, dynamic>;
    return SettingsData.fromJson(data);
  }

  /// 설정 수정.
  Future<SettingsData> updateSettings({
    bool? notificationEnabled,
    String? fontSize,
    bool? musicEnabled,
    int? musicVolume,
    String? musicCategory,
  }) async {
    final body = <String, dynamic>{};
    if (notificationEnabled != null) body['notificationEnabled'] = notificationEnabled;
    if (fontSize != null) body['fontSize'] = fontSize;
    if (musicEnabled != null) body['musicEnabled'] = musicEnabled;
    if (musicVolume != null) body['musicVolume'] = musicVolume;
    if (musicCategory != null) body['musicCategory'] = musicCategory;
    final response = await _dio.patch('/me/settings', data: body);
    final data = response.data['data'] as Map<String, dynamic>;
    return SettingsData.fromJson(data);
  }

  // ── 찬양 ──

  /// 큐레이션 곡 목록 조회.
  Future<List<PraiseSong>> getCurationSongs() async {
    final response = await _dio.get('/praise-songs');
    final data = response.data['data'] as List<dynamic>;
    return data.map((e) => PraiseSong.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// 내 찬양 목록 조회.
  Future<List<MyPraiseSong>> getMyPraiseSongs() async {
    final response = await _dio.get('/me/praise-songs');
    final data = response.data['data'] as List<dynamic>;
    return data.map((e) => MyPraiseSong.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// 내 찬양 저장 (큐레이션 곡).
  ///
  /// POST /me/praise-songs — praiseSongId + displayTitle 필수 (04 §4.6.4).
  Future<void> saveMyPraiseSong(int praiseSongId, String displayTitle) async {
    await _dio.post('/me/praise-songs', data: {
      'praiseSongId': praiseSongId,
      'displayTitle': displayTitle,
    });
  }

  /// 내 찬양 삭제.
  Future<void> deleteMyPraiseSong(int id) async {
    await _dio.delete('/me/praise-songs/$id');
  }

  // ── 탈퇴 ──

  /// 회원 탈퇴.
  ///
  /// DELETE /me → 204 No Content
  Future<void> withdraw({String? reason}) async {
    await _dio.delete(
      '/me',
      data: reason != null ? {'reason': reason} : null,
    );
  }
}
