import 'package:dio/dio.dart';

import '../models/dashboard_response.dart';
import '../models/member_response.dart';

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
