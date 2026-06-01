import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/error_interceptor.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../auth/providers/auth_providers.dart';
import '../providers/mypage_providers.dart';
import '../widgets/withdraw_dialog.dart';

/// 프로필 상세 + 닉네임 변경 화면.
///
/// - 프로필 이미지, 닉네임, 이메일, 가입일 표시
/// - 닉네임 변경: 디바운스 300ms 중복검사, 7일 잠금 비활성화+안내문
/// - 하단에 탈퇴 버튼
class ProfileEditScreen extends ConsumerStatefulWidget {
  const ProfileEditScreen({super.key});

  @override
  ConsumerState<ProfileEditScreen> createState() => _ProfileEditScreenState();
}

class _ProfileEditScreenState extends ConsumerState<ProfileEditScreen> {
  final _nicknameController = TextEditingController();
  bool _isEditing = false;
  bool _isSaving = false;
  Timer? _debounceTimer;

  @override
  void dispose() {
    _nicknameController.dispose();
    _debounceTimer?.cancel();
    super.dispose();
  }

  void _onNicknameChanged(String value) {
    _debounceTimer?.cancel();
    _debounceTimer = Timer(const Duration(milliseconds: 300), () {
      ref.read(nicknameQueryProvider.notifier).state = value.trim();
    });
  }

  Future<void> _saveNickname() async {
    final nickname = _nicknameController.text.trim();
    if (nickname.isEmpty || nickname.length < 2) return;

    setState(() => _isSaving = true);
    try {
      final repository = ref.read(myPageRepositoryProvider);
      await repository.changeNickname(nickname);

      // 프로필·대시보드 갱신
      ref.invalidate(profileProvider);
      ref.invalidate(dashboardProvider);

      if (mounted) {
        setState(() => _isEditing = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('닉네임이 변경되었습니다.')),
        );
      }
    } on ApiError catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.message)),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('닉네임 변경에 실패했습니다.')),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _handleLogout() async {
    try {
      final authRepository = ref.read(authRepositoryProvider);
      await authRepository.logout();
      ref.read(authStatusProvider.notifier).setUnauthenticated();

      if (mounted) {
        Navigator.of(context)
            .pushNamedAndRemoveUntil('/login', (route) => false);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('로그아웃에 실패했습니다.')),
        );
      }
    }
  }

  Future<void> _showWithdrawDialog() async {
    final confirmed = await showWithdrawDialog(context);
    if (confirmed != true || !mounted) return;

    try {
      final repository = ref.read(myPageRepositoryProvider);
      await repository.withdraw();

      if (mounted) {
        // 토큰 정리 후 로그인 화면으로 이동
        Navigator.of(context)
            .pushNamedAndRemoveUntil('/login', (route) => false);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('탈퇴 처리에 실패했습니다.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final profileAsync = ref.watch(profileProvider);
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('프로필'),
        centerTitle: true,
      ),
      body: profileAsync.whenOrDefault(
        data: (member) {
          // 닉네임 컨트롤러 초기화 (편집 시작 시)
          if (!_isEditing) {
            _nicknameController.text = member.nickname;
          }

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              // 프로필 이미지
              Center(
                child: CircleAvatar(
                  radius: 48,
                  backgroundImage: member.profileImageUrl != null
                      ? NetworkImage(member.profileImageUrl!)
                      : null,
                  child: member.profileImageUrl == null
                      ? const Icon(Icons.person, size: 48)
                      : null,
                ),
              ),

              const SizedBox(height: 24),

              // 닉네임 섹션
              _buildNicknameSection(member.isNicknameChangeable,
                  member.nicknameUnlockAt, theme),

              const SizedBox(height: 16),

              // 이메일
              if (member.email != null)
                ListTile(
                  leading: const Icon(Icons.email_outlined),
                  title: const Text('이메일'),
                  subtitle: Text(member.email!),
                ),

              // 가입일
              if (member.createdAt != null)
                ListTile(
                  leading: const Icon(Icons.calendar_today_outlined),
                  title: const Text('가입일'),
                  subtitle: Text(
                    '${member.createdAt!.year}년 '
                    '${member.createdAt!.month}월 '
                    '${member.createdAt!.day}일',
                  ),
                ),

              const SizedBox(height: 32),

              // 로그아웃 버튼
              OutlinedButton.icon(
                onPressed: _handleLogout,
                icon: const Icon(Icons.logout),
                label: const Text('로그아웃'),
              ),

              const SizedBox(height: 12),

              // 탈퇴 버튼
              TextButton(
                onPressed: _showWithdrawDialog,
                child: Text(
                  '회원 탈퇴',
                  style: TextStyle(color: theme.colorScheme.error),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildNicknameSection(
      bool isChangeable, DateTime? unlockAt, ThemeData theme) {
    final nicknameAvailable = ref.watch(nicknameAvailableProvider);

    if (_isEditing) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                controller: _nicknameController,
                onChanged: _onNicknameChanged,
                maxLength: 20,
                decoration: InputDecoration(
                  labelText: '새 닉네임',
                  hintText: '2~20자',
                  suffixIcon: nicknameAvailable.when(
                    data: (available) {
                      if (available == null) return null;
                      return Icon(
                        available ? Icons.check_circle : Icons.cancel,
                        color: available ? Colors.green : Colors.red,
                      );
                    },
                    loading: () => const SizedBox(
                      width: 20,
                      height: 20,
                      child: Padding(
                        padding: EdgeInsets.all(12),
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    ),
                    error: (e, _) => const Icon(Icons.error, color: Colors.red),
                  ),
                ),
              ),
              // 중복 검사 결과 텍스트
              nicknameAvailable.when(
                data: (available) {
                  if (available == null) return const SizedBox.shrink();
                  return Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      available ? '사용 가능한 닉네임입니다.' : '이미 사용 중인 닉네임입니다.',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: available ? Colors.green : Colors.red,
                      ),
                    ),
                  );
                },
                loading: () => const SizedBox.shrink(),
                error: (e, _) => const SizedBox.shrink(),
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    onPressed: _isSaving
                        ? null
                        : () => setState(() => _isEditing = false),
                    child: const Text('취소'),
                  ),
                  const SizedBox(width: 8),
                  FilledButton(
                    onPressed: _isSaving ||
                            nicknameAvailable.valueOrNull != true
                        ? null
                        : _saveNickname,
                    child: _isSaving
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child:
                                CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('변경'),
                  ),
                ],
              ),
            ],
          ),
        ),
      );
    }

    // 닉네임 표시 모드
    return ListTile(
      leading: const Icon(Icons.badge_outlined),
      title: const Text('닉네임'),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(_nicknameController.text),
          if (!isChangeable && unlockAt != null)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                '${unlockAt.year}년 ${unlockAt.month}월 ${unlockAt.day}일 이후 변경 가능',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.outline,
                ),
              ),
            ),
        ],
      ),
      trailing: FilledButton.tonal(
        onPressed: isChangeable ? () => setState(() => _isEditing = true) : null,
        child: const Text('변경'),
      ),
    );
  }
}
