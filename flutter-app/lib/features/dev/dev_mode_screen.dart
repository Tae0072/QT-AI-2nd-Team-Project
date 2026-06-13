// [DEV_MODE] =================================================================
// 개발자 모드 화면 + 설정 화면용 "버전 정보(5탭 진입)" 타일.
// 개발 종료 시: `[DEV_MODE]` 검색 후 이 파일과 core/dev/dev_mode.dart 삭제.
// ===========================================================================
import 'dart:async';

import 'package:flutter/material.dart';

import '../../core/dev/dev_mode.dart';
import '../../core/notifications/local_notification_service.dart';
import '../../routes/app_router.dart';

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

/// [DEV_MODE] 개발자 모드 화면 — 온보딩 보기 / 카카오 로그인 로그.
class DevModeScreen extends StatelessWidget {
  const DevModeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('개발자 모드'), centerTitle: true),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          ListTile(
            leading: const Icon(Icons.slideshow_outlined),
            title: const Text('온보딩 화면 보기'),
            subtitle: const Text('첫 실행 온보딩 화면을 다시 띄웁니다.'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.of(context).pushNamed(AppRouter.onboarding),
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
// [DEV_MODE] end =============================================================
