import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/network/error_interceptor.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../auth/providers/auth_providers.dart';
import '../models/member_response.dart';
import '../providers/mypage_providers.dart';
import '../widgets/withdraw_dialog.dart';

/// 프로필 상세 + 닉네임 변경 화면.
///
/// - 프로필 이미지, 닉네임, 이메일, 가입일 표시
/// - 닉네임 변경: 디바운스 300ms 중복검사 (즉시 변경 가능 — 서버 잠금 폐지로
///   nicknameUnlockAt이 항상 null, 아래 잠금 안내 분기는 정책 부활 대비 잔존)
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
  bool _photoBusy = false; // 사진 업로드/삭제 중
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

  /// 아바타 — 카카오 등 외부 http URL은 NetworkImage, 우리 서버 업로드분은
  /// Dio로 바이트를 받아 MemoryImage로 표시(인증 필요), 없으면 기본 아이콘.
  Widget _avatar(MemberResponse member) {
    final url = member.profileImageUrl;
    if (url == null) {
      return const CircleAvatar(radius: 48, child: Icon(Icons.person, size: 48));
    }
    if (url.startsWith('http')) {
      return CircleAvatar(radius: 48, backgroundImage: NetworkImage(url));
    }
    // 우리 서버 업로드분(/api/v1/me/profile-photo?v=...): 버전이 바뀌면 다시 로드.
    return FutureBuilder<Uint8List?>(
      key: ValueKey(url),
      future: ref.read(myPageRepositoryProvider).getMyProfilePhotoBytes(),
      builder: (context, snapshot) {
        final bytes = snapshot.data;
        return CircleAvatar(
          radius: 48,
          backgroundImage: bytes != null ? MemoryImage(bytes) : null,
          child: bytes == null ? const Icon(Icons.person, size: 48) : null,
        );
      },
    );
  }

  Future<void> _pickAndUploadPhoto() async {
    final picker = ImagePicker();
    final XFile? picked = await picker.pickImage(
      source: ImageSource.gallery,
      maxWidth: 1024,
      maxHeight: 1024,
      imageQuality: 85,
    );
    if (picked == null) return;
    setState(() => _photoBusy = true);
    try {
      final bytes = await picked.readAsBytes();
      await ref
          .read(myPageRepositoryProvider)
          .uploadProfilePhoto(bytes, filename: picked.name);
      ref.invalidate(profileProvider);
      ref.invalidate(dashboardProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('프로필 사진을 변경했어요.')),
        );
      }
    } on ApiError catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(e.message)));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('사진을 변경하지 못했어요. 다시 시도해 주세요.')),
        );
      }
    } finally {
      if (mounted) setState(() => _photoBusy = false);
    }
  }

  Future<void> _deletePhoto() async {
    setState(() => _photoBusy = true);
    try {
      await ref.read(myPageRepositoryProvider).deleteProfilePhoto();
      ref.invalidate(profileProvider);
      ref.invalidate(dashboardProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('기본 이미지로 변경했어요.')),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('변경하지 못했어요. 다시 시도해 주세요.')),
        );
      }
    } finally {
      if (mounted) setState(() => _photoBusy = false);
    }
  }

  Future<void> _saveNickname() async {
    final l = AppLocalizations.of(context);
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
          SnackBar(content: Text(l.profileNicknameChanged)),
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
          SnackBar(content: Text(l.profileNicknameChangeFailed)),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _handleLogout() async {
    final l = AppLocalizations.of(context);
    try {
      final authRepository = ref.read(authRepositoryProvider);
      await authRepository.logout();
      ref.read(authStatusProvider.notifier).setUnauthenticated();

      if (mounted) {
        unawaited(Navigator.of(context)
            .pushNamedAndRemoveUntil('/login', (route) => false));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.profileLogoutFailed)),
        );
      }
    }
  }

  Future<void> _showWithdrawDialog() async {
    final l = AppLocalizations.of(context);
    final confirmed = await showWithdrawDialog(context);
    if (confirmed != true || !mounted) return;

    try {
      // 1) 서버 탈퇴 — 토큰이 살아있는 상태에서 호출
      final repository = ref.read(myPageRepositoryProvider);
      await repository.withdraw();

      // 2) 카카오 연결끊기(unlink) + 로컬 토큰 삭제
      //    (미정리 시 stale JWT가 남아 자동로그인/재로그인 흐름이 깨진다)
      final authRepository = ref.read(authRepositoryProvider);
      await authRepository.cleanupAfterWithdraw();

      // 3) 인증 상태 전환 — main.dart의 ValueKey(initialRoute)가 바뀌며
      //    Navigator가 재생성되어 로그인 화면으로 이동한다.
      //    (상태를 바꾸지 않으면 재로그인 성공 시 setAuthenticated()가
      //    동일 값이라 화면 전환이 일어나지 않는 버그가 있었다)
      ref.read(authStatusProvider.notifier).setUnauthenticated();

      if (mounted) {
        unawaited(Navigator.of(context)
            .pushNamedAndRemoveUntil('/login', (route) => false));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l.profileWithdrawFailed)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final profileAsync = ref.watch(profileProvider);
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.profileTitle),
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
              // 프로필 이미지 + 변경/삭제
              Center(child: _avatar(member)),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  TextButton.icon(
                    onPressed: _photoBusy ? null : _pickAndUploadPhoto,
                    icon: const Icon(Icons.photo_camera_outlined, size: 18),
                    label: const Text('사진 변경'),
                  ),
                  if (member.profileImageUrl != null)
                    TextButton.icon(
                      onPressed: _photoBusy ? null : _deletePhoto,
                      icon: const Icon(Icons.delete_outline, size: 18),
                      label: const Text('기본 이미지로'),
                    ),
                ],
              ),

              const SizedBox(height: 16),

              // 닉네임 섹션
              _buildNicknameSection(member.isNicknameChangeable,
                  member.nicknameUnlockAt, theme),

              const SizedBox(height: 16),

              // 이메일
              if (member.email != null)
                ListTile(
                  leading: const Icon(Icons.email_outlined),
                  title: Text(l.profileEmail),
                  subtitle: Text(member.email!),
                ),

              // 가입일
              if (member.createdAt != null)
                ListTile(
                  leading: const Icon(Icons.calendar_today_outlined),
                  title: Text(l.profileJoinDate),
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
                label: Text(l.profileLogout),
              ),

              const SizedBox(height: 12),

              // 탈퇴 버튼
              TextButton(
                onPressed: _showWithdrawDialog,
                child: Text(
                  l.withdrawTitle,
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
    final l = AppLocalizations.of(context);

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
                  labelText: l.profileNewNickname,
                  hintText: l.profileNicknameHint2,
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
                      available ? l.profileNicknameAvailable : l.profileNicknameTaken,
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
                    child: Text(l.commonCancel),
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
                        : Text(l.profileChange),
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
      title: Text(l.profileNickname),
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
        child: Text(l.profileChange),
      ),
    );
  }
}
