/// 회원 상세 정보 응답 모델.
///
/// GET /api/v1/me 응답에 대응한다.
class MemberResponse {
  final int id;
  final String nickname;
  final String? email;
  final String? profileImageUrl;
  final String status;
  final String role;
  final DateTime? nicknameUnlockAt;
  final DateTime? createdAt;

  const MemberResponse({
    required this.id,
    required this.nickname,
    this.email,
    this.profileImageUrl,
    required this.status,
    required this.role,
    this.nicknameUnlockAt,
    this.createdAt,
  });

  factory MemberResponse.fromJson(Map<String, dynamic> json) {
    return MemberResponse(
      id: (json['id'] as num).toInt(),
      nickname: json['nickname'] as String,
      email: json['email'] as String?,
      profileImageUrl: json['profileImageUrl'] as String?,
      status: json['status'] as String,
      role: json['role'] as String,
      nicknameUnlockAt: json['nicknameUnlockAt'] != null
          ? DateTime.parse(json['nicknameUnlockAt'] as String)
          : null,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
    );
  }

  /// 닉네임 변경 가능 여부.
  ///
  /// [nicknameUnlockAt]이 null이면 변경 가능 (한 번도 변경한 적 없음).
  /// 현재 시각이 [nicknameUnlockAt] 이후이면 변경 가능.
  bool get isNicknameChangeable {
    if (nicknameUnlockAt == null) return true;
    return DateTime.now().isAfter(nicknameUnlockAt!);
  }
}

/// 다른 회원의 공개 프로필 응답 모델.
///
/// GET /api/v1/members/{id} 응답에 대응한다.
class MemberPublicResponse {
  final int id;
  final String nickname;
  final String? profileImageUrl;

  const MemberPublicResponse({
    required this.id,
    required this.nickname,
    this.profileImageUrl,
  });

  factory MemberPublicResponse.fromJson(Map<String, dynamic> json) {
    return MemberPublicResponse(
      id: (json['id'] as num).toInt(),
      nickname: json['nickname'] as String,
      profileImageUrl: json['profileImageUrl'] as String?,
    );
  }
}
