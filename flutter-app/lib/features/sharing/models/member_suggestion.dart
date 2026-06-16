/// 멘션 자동완성 후보(닉네임). GET /api/v1/members/search 응답 1건.
class MemberSuggestion {
  final int id;
  final String nickname;

  const MemberSuggestion({required this.id, required this.nickname});

  factory MemberSuggestion.fromJson(Map<String, dynamic> json) {
    return MemberSuggestion(
      id: json['id'] as int,
      nickname: json['nickname'] as String? ?? '',
    );
  }
}
