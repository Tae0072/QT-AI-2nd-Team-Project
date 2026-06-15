import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../models/dashboard_response.dart';
import '../models/member_response.dart';
import '../models/notification_response.dart';
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

  // ── 프로필 사진(서버 DB 저장) ──

  /// 프로필 사진 업로드(multipart). [bytes]는 이미지 바이트, [filename]은 확장자 포함.
  ///
  /// POST /me/profile-photo → [MemberResponse]
  Future<MemberResponse> uploadProfilePhoto(
    Uint8List bytes, {
    required String filename,
  }) async {
    final subtype = _imageSubtype(filename);
    final form = FormData.fromMap({
      'file': MultipartFile.fromBytes(
        bytes,
        filename: filename,
        contentType: DioMediaType('image', subtype),
      ),
    });
    final response = await _dio.post('/me/profile-photo', data: form);
    final data = response.data['data'] as Map<String, dynamic>;
    return MemberResponse.fromJson(data);
  }

  /// 프로필 사진 삭제(기본 아바타로).
  ///
  /// DELETE /me/profile-photo → [MemberResponse]
  Future<MemberResponse> deleteProfilePhoto() async {
    final response = await _dio.delete('/me/profile-photo');
    final data = response.data['data'] as Map<String, dynamic>;
    return MemberResponse.fromJson(data);
  }

  /// 내가 업로드한 프로필 사진 바이트 조회(없으면 null).
  ///
  /// GET /me/profile-photo (raw bytes)
  Future<Uint8List?> getMyProfilePhotoBytes() async {
    try {
      final response = await _dio.get<List<int>>(
        '/me/profile-photo',
        options: Options(responseType: ResponseType.bytes),
      );
      final data = response.data;
      return data == null ? null : Uint8List.fromList(data);
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) return null;
      rethrow;
    }
  }

  /// 확장자 → image 서브타입(jpeg/png/webp). 기본 jpeg.
  String _imageSubtype(String filename) {
    final lower = filename.toLowerCase();
    if (lower.endsWith('.png')) return 'png';
    if (lower.endsWith('.webp')) return 'webp';
    return 'jpeg';
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
