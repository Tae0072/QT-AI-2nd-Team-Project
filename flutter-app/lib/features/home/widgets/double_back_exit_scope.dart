import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show SystemNavigator;

import 'package:qtai_app/l10n/app_localizations.dart';
import '../services/double_back_exit_policy.dart';

/// 루트 화면 전용 "뒤로가기 2번 종료" 스코프.
///
/// 첫 뒤로가기는 안내 스낵바만 띄우고, [DoubleBackExitPolicy.window](2초) 안에
/// 다시 누를 때만 [SystemNavigator.pop]으로 앱을 종료한다(실수 종료 방지).
///
/// 이 스코프는 **자신이 속한 라우트의 pop만** 가로챈다 — 위에 push된 화면
/// (알림·설정·노트 작성 등)의 뒤로가기는 각자 라우트의 일반 pop으로 처리되어
/// 부모 화면으로 정상 복귀한다. 별도 위젯으로 분리한 이유: 홈 화면은 Dio 호출
/// 화면들을 포함해 위젯 테스트가 어려우므로, 종료 동작은 이 위젯 단독으로 검증한다.
class DoubleBackExitScope extends StatefulWidget {
  const DoubleBackExitScope({super.key, required this.child});

  final Widget child;

  @override
  State<DoubleBackExitScope> createState() => _DoubleBackExitScopeState();
}

class _DoubleBackExitScopeState extends State<DoubleBackExitScope> {
  DateTime? _lastBackPressedAt;

  void _onPopInvoked(bool didPop, Object? result) {
    if (didPop) return;
    final now = DateTime.now();
    if (DoubleBackExitPolicy.shouldExit(last: _lastBackPressedAt, now: now)) {
      // pop이 아니라 앱 종료 — 루트라 pop할 라우트가 없고, 안드로이드 관례상 태스크를 닫는다.
      SystemNavigator.pop();
      return;
    }
    _lastBackPressedAt = now;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        content: Text(AppLocalizations.of(context).homeBackExitGuide),
        duration: DoubleBackExitPolicy.window,
        behavior: SnackBarBehavior.floating,
      ));
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: _onPopInvoked,
      child: widget.child,
    );
  }
}
