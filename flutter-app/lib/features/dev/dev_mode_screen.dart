// [DEV_MODE] =================================================================
// 개발자 모드 화면 + 설정 화면용 "버전 정보(5탭 진입)" 타일.
// 개발 종료 시: `[DEV_MODE]` 검색 후 이 파일과 core/dev/dev_mode.dart 삭제.
// ===========================================================================
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/dev/dev_mode.dart';
import '../../core/notifications/local_notification_service.dart';
import '../../core/theme/font_scale_provider.dart';
import '../../core/theme/theme_providers.dart';
import '../../routes/app_router.dart';
import '../auth/providers/auth_providers.dart';
import '../onboarding/providers/onboarding_providers.dart';

/// [DEV_MODE] 설정 화면 하단의 "버전 정보" 타일.
/// 5번 연속 탭 → 비밀번호 입력 → 개발자 모드 진입.
class DevVersionTile extends StatefulWidget {
  const DevVersionTile({super.key});

  @override
  State<DevVersionTile> createState() => _DevVersionTileState();
}

class _DevVersionTileState extends State<DevVersionTile> {
  int _tapCount = 0;
  DateTime? _lastTap;

  void _onTap() {
    final now = DateTime.now();
    // 직전 탭과 2초 이상 벌어지면 카운트 리셋(연속 탭만 인정).
    if (_lastTap != null && now.difference(_lastTap!) > const Duration(seconds: 2)) {
      _tapCount = 0;
    }
    _lastTap = now;
    _tapCount++;
    if (_tapCount >= 5) {
      _tapCount = 0;
      _promptPassword();
    }
  }

  Future<void> _promptPassword() async {
    final controller = TextEditingController();
    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('개발자 모드'),
        content: TextField(
          controller: controller,
          obscureText: true,
          autofocus: true,
          decoration: const InputDecoration(labelText: '비밀번호'),
          onSubmitted: (_) =>
              Navigator.of(ctx)
              .pop(kDevModePassword.isNotEmpty && controller.text == kDevModePassword),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(), // 취소 → null
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () =>
                Navigator.of(ctx)
              .pop(kDevModePassword.isNotEmpty && controller.text == kDevModePassword),
            child: const Text('확인'),
          ),
        ],
      ),
    );
    if (!mounted) return;
    if (result == null) return; // 취소
    if (result) {
      unawaited(Navigator.of(context).pushNamed(AppRouter.devMode));
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('비밀번호가 올바르지 않습니다.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: const Icon(Icons.info_outline),
      title: const Text('버전 정보'),
      subtitle: Text('v$kAppVersion ($kAppBuild)'),
      onTap: _onTap,
    );
  }
}

/// [DEV_MODE] 화면 바로가기 항목.
class _DevRoute {
  final String label;
  final String route;
  const _DevRoute(this.label, this.route);
}

/// [DEV_MODE] 개발자 모드 화면 — 모든 화면 바로가기 + 상태 토글 + 알림/로그 테스트.
class DevModeScreen extends ConsumerStatefulWidget {
  const DevModeScreen({super.key});

  @override
  ConsumerState<DevModeScreen> createState() => _DevModeScreenState();
}

class _DevModeScreenState extends ConsumerState<DevModeScreen> {
  final _postIdCtrl = TextEditingController();
  final _noteIdCtrl = TextEditingController();

  // 인자가 필요 없는 화면들 — 바로 push로 이동해 테스트한다.
  static const _routes = <_DevRoute>[
    _DevRoute('홈', AppRouter.home),
    _DevRoute('온보딩', AppRouter.onboarding),
    _DevRoute('닉네임 설정', AppRouter.nicknameSetup),
    _DevRoute('로그인', AppRouter.login),
    _DevRoute('마이페이지', AppRouter.myPage),
    _DevRoute('프로필 편집', AppRouter.profileEdit),
    _DevRoute('알림 목록', AppRouter.notifications),
    _DevRoute('설정', AppRouter.appSettings),
    _DevRoute('TTS 설정', AppRouter.ttsSettings),
    _DevRoute('음악 설정', AppRouter.musicSettings),
    _DevRoute('찬양', AppRouter.praise),
    _DevRoute('나눔 피드', AppRouter.sharing),
    _DevRoute('저장한 글', AppRouter.sharingBookmarks),
    _DevRoute('나를 태그한 글', AppRouter.sharingMentions),
    _DevRoute('내 나눔', AppRouter.mySharing),
    _DevRoute('기록(노트 목록)', AppRouter.noteList),
    _DevRoute('노트 카테고리 선택', AppRouter.noteCategorySelect),
    _DevRoute('노트 작성', AppRouter.noteEdit),
  ];

  @override
  void dispose() {
    _postIdCtrl.dispose();
    _noteIdCtrl.dispose();
    super.dispose();
  }

  void _go(String route, {Object? args}) =>
      Navigator.of(context).pushNamed(route, arguments: args);

  void _goById(TextEditingController ctrl, String route) {
    final id = int.tryParse(ctrl.text.trim());
    if (id == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('숫자 ID를 입력하세요.')),
      );
      return;
    }
    _go(route, args: id);
  }

  @override
  Widget build(BuildContext context) {
    final themeMode = ref.watch(themeModeProvider);
    final fontSize = ref.watch(fontSizeProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('개발자 모드'), centerTitle: true),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // ── 화면 바로가기 ────────────────────────────────────────────────
          const Text('화면 바로가기',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          const Text('앱의 모든 화면을 바로 열어 테스트합니다.',
              style: TextStyle(fontSize: 12, color: Colors.grey)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final r in _routes)
                OutlinedButton(
                  onPressed: () => _go(r.route),
                  child: Text(r.label),
                ),
            ],
          ),
          const SizedBox(height: 16),
          // ── 인자가 필요한 화면(ID 입력) ──────────────────────────────────
          const Text('인자 필요 화면 (ID 입력)',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          _IdRow(
            label: '나눔 상세',
            hint: '나눔 글 ID',
            controller: _postIdCtrl,
            onGo: () => _goById(_postIdCtrl, AppRouter.sharingDetail),
          ),
          const SizedBox(height: 8),
          _IdRow(
            label: '노트 상세',
            hint: '노트 ID',
            controller: _noteIdCtrl,
            onGo: () => _goById(_noteIdCtrl, AppRouter.noteDetail),
          ),
          const Divider(height: 32),

          // ── 상태 토글 ────────────────────────────────────────────────────
          const Text('상태 토글',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          const Text('테마 모드', style: TextStyle(fontWeight: FontWeight.w600)),
          const SizedBox(height: 6),
          SegmentedButton<ThemeMode>(
            segments: const [
              ButtonSegment(value: ThemeMode.light, label: Text('라이트')),
              ButtonSegment(value: ThemeMode.dark, label: Text('다크')),
              ButtonSegment(value: ThemeMode.system, label: Text('시스템')),
            ],
            selected: {themeMode},
            onSelectionChanged: (s) =>
                unawaited(ref.read(themeModeProvider.notifier).setMode(s.first)),
          ),
          const SizedBox(height: 12),
          const Text('폰트 크기', style: TextStyle(fontWeight: FontWeight.w600)),
          const SizedBox(height: 6),
          SegmentedButton<String>(
            segments: const [
              ButtonSegment(value: 'SMALL', label: Text('작게')),
              ButtonSegment(value: 'MEDIUM', label: Text('보통')),
              ButtonSegment(value: 'LARGE', label: Text('크게')),
            ],
            selected: {fontSize},
            onSelectionChanged: (s) =>
                unawaited(ref.read(fontSizeProvider.notifier).set(s.first)),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              OutlinedButton.icon(
                icon: const Icon(Icons.restart_alt, size: 18),
                label: const Text('온보딩 다시 보기 리셋'),
                onPressed: () async {
                  await ref.read(onboardingCompleteProvider.notifier).reset();
                  if (!context.mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('온보딩이 리셋됐어요. 앱을 재시작하면 온보딩부터 시작합니다.')),
                  );
                },
              ),
              OutlinedButton.icon(
                icon: const Icon(Icons.logout, size: 18),
                label: const Text('로그아웃'),
                onPressed: () async {
                  await ref.read(authRepositoryProvider).logout();
                  ref.read(authStatusProvider.notifier).setUnauthenticated();
                  if (!context.mounted) return;
                  unawaited(Navigator.of(context).pushNamedAndRemoveUntil(
                      AppRouter.login, (route) => false));
                },
              ),
            ],
          ),
          const Divider(height: 32),
          // ── 알림 보내기(테스트) ──────────────────────────────────────────
          // 내 기기에 OS 알림(상단 알림 표시줄)이 실제로 오는지 바로 확인하는 버튼들.
          const Text('알림 보내기 (테스트)',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          const Text(
            '버튼을 누르면 이 기기로 알림을 보냅니다. 좋아요·댓글·돌파 알림이 실제로 오는지 확인하세요. '
            '(Android 13+/iOS는 처음에 알림 권한 허용이 필요합니다)',
            style: TextStyle(fontSize: 12, color: Colors.grey),
          ),
          const SizedBox(height: 8),
          const _NotificationTestButtons(),
          const Divider(height: 32),
          Row(
            children: [
              const Text('카카오 로그인 로그',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              const Spacer(),
              TextButton(
                onPressed: KakaoLoginLog.clear,
                child: const Text('지우기'),
              ),
            ],
          ),
          const SizedBox(height: 8),
          ValueListenableBuilder<List<String>>(
            valueListenable: KakaoLoginLog.entries,
            builder: (context, logs, _) {
              if (logs.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.symmetric(vertical: 12),
                  child: Text('아직 로그가 없습니다. 카카오 로그인을 시도해 보세요.'),
                );
              }
              return Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.black.withValues(alpha: 0.05),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: SelectableText(
                  logs.join('\n'),
                  style: const TextStyle(
                    fontFamily: 'monospace',
                    fontSize: 12,
                    height: 1.5,
                  ),
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}
/// [DEV_MODE] 알림 테스트 버튼 묶음 — 누르면 이 기기로 OS 알림을 보낸다.
class _NotificationTestButtons extends StatelessWidget {
  const _NotificationTestButtons();

  Future<void> _send(BuildContext context, Future<bool> Function() action) async {
    final ok = await action();
    if (!context.mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        content: Text(ok
            ? '알림을 보냈어요. 상단 알림 표시줄을 확인하세요.'
            : '알림을 보내지 못했어요. 알림 권한을 확인하세요.'),
        duration: const Duration(seconds: 2),
      ));
  }

  @override
  Widget build(BuildContext context) {
    final service = LocalNotificationService.instance;
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        OutlinedButton.icon(
          onPressed: () => _send(context, () => service.showLike()),
          icon: const Icon(Icons.favorite_border, size: 18),
          label: const Text('좋아요 알림'),
        ),
        OutlinedButton.icon(
          onPressed: () => _send(context, () => service.showComment()),
          icon: const Icon(Icons.mode_comment_outlined, size: 18),
          label: const Text('댓글 알림'),
        ),
        OutlinedButton.icon(
          onPressed: () => _send(context, () => service.showMilestone(count: 100)),
          icon: const Icon(Icons.emoji_events_outlined, size: 18),
          label: const Text('100개 돌파'),
        ),
        OutlinedButton.icon(
          onPressed: () =>
              _send(context, () => service.showMilestone(count: 1000)),
          icon: const Icon(Icons.emoji_events, size: 18),
          label: const Text('1000개 돌파'),
        ),
        FilledButton.icon(
          onPressed: () => _send(
            context,
            () => service.show(
              title: 'QT-AI 테스트 알림',
              body: '이 알림이 보이면 기기 알림이 정상 동작합니다.',
            ),
          ),
          icon: const Icon(Icons.notifications_active_outlined, size: 18),
          label: const Text('테스트 알림 보내기'),
        ),
      ],
    );
  }
}
/// [DEV_MODE] ID를 입력해 인자 필요 화면으로 이동하는 한 줄.
class _IdRow extends StatelessWidget {
  final String label;
  final String hint;
  final TextEditingController controller;
  final VoidCallback onGo;

  const _IdRow({
    required this.label,
    required this.hint,
    required this.controller,
    required this.onGo,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(width: 88, child: Text(label)),
        Expanded(
          child: TextField(
            controller: controller,
            keyboardType: TextInputType.number,
            decoration: InputDecoration(
              hintText: hint,
              isDense: true,
              border: const OutlineInputBorder(),
            ),
            onSubmitted: (_) => onGo(),
          ),
        ),
        const SizedBox(width: 8),
        FilledButton(onPressed: onGo, child: const Text('이동')),
      ],
    );
  }
}

// [DEV_MODE] end =============================================================
