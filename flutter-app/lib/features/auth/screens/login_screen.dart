import 'dart:async';

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qtai_app/l10n/app_localizations.dart';

import '../../../core/dev/web_dev_access.dart';
import '../../../core/theme/app_theme.dart';
import '../../../routes/app_router.dart';
import '../providers/auth_providers.dart';
import '../services/kakao_login_guard.dart';

/// 카카오 로그인 화면 (A-02) — 웜 파스텔 디자인.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  bool _isLoading = false;
  String? _errorMessage;

  Future<void> _handleKakaoLogin() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final authRepository = ref.read(authRepositoryProvider);
      final result = await authRepository.loginWithKakao();

      if (!mounted) return;

      if (result.isNewMember) {
        // 화면 전환 Future는 대기하지 않는다(다음 라우트 pop까지 완료되지 않음).
        unawaited(
            Navigator.of(context).pushReplacementNamed(AppRouter.nicknameSetup));
      } else {
        ref.read(authStatusProvider.notifier).setAuthenticated();
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMessage = AppLocalizations.of(context).loginFailed;
      });
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    // 웹에서는 카카오 dart SDK가 동작하지 않아 버튼을 비활성화하고 안내한다(TODO 2).
    // dev 웹 우회(webDevNoLogin, 삼중 게이트)가 켜진 경우는 기존 동작 유지.
    final webLoginUnsupported =
        isKakaoLoginUnsupported(isWeb: kIsWeb, webDevBypassEnabled: webDevNoLogin);
    // 다크 모드 대응 — 정적 토큰 대신 현재 모드의 색 토큰을 받는다(fallback 내장).
    final colors = context.appColors;
    return Scaffold(
      backgroundColor: colors.bg,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            children: [
              const Spacer(flex: 2),

              // 로고
              Text.rich(
                TextSpan(children: [
                  const TextSpan(text: 'QT'),
                  // 로고 가운뎃점 — 유일한 유채색 포인트(탭 도트와 동일 토큰).
                  TextSpan(text: '·', style: TextStyle(color: colors.accentDot)),
                  const TextSpan(text: 'AI'),
                ]),
                style: TextStyle(
                  fontFamily: 'GowunDodum',
                  fontSize: 40,
                  fontWeight: FontWeight.w400,
                  color: colors.text,
                  letterSpacing: -1,
                ),
              ),
              const SizedBox(height: 18),

              // 헤드라인
              Text(
                l.loginHeadline,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'GowunDodum',
                  fontSize: 28,
                  fontWeight: FontWeight.w400,
                  color: colors.text,
                  height: 1.2,
                  letterSpacing: -0.4,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                l.loginSubtitle,
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 17, color: colors.textMuted, height: 1.5),
              ),

              const Spacer(flex: 2),

              // 에러 메시지
              if (_errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.red.shade50,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.error_outline, color: Colors.red.shade700, size: 20),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(_errorMessage!,
                            style: TextStyle(color: Colors.red.shade700, fontSize: 14)),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
              ],

              // 카카오 로그인 버튼
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: (_isLoading || webLoginUnsupported)
                      ? null
                      : _handleKakaoLogin,
                  icon: _isLoading
                      ? const SizedBox(
                          width: 20, height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFF191600)))
                      : const Icon(Icons.chat_bubble, size: 20),
                  label: Text(l.loginKakaoButton),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFEE500),
                    foregroundColor: const Color(0xFF191600),
                    shape: const StadiumBorder(),
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    textStyle: const TextStyle(
                      fontFamily: 'GowunDodum',
                      fontSize: 17,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),

              // 웹 미지원 안내 (버튼 비활성 사유)
              if (webLoginUnsupported) ...[
                const SizedBox(height: 12),
                Text(
                  l.loginWebNotSupported,
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 13, color: colors.textMuted),
                ),
              ],

              const SizedBox(height: 18),

              // 법적 고지
              Text.rich(
                TextSpan(
                  text: l.loginLegalPrefix,
                  children: [
                    TextSpan(
                      text: l.loginTermsOfService,
                      // 링크 강조는 무채색(본문색) + 밑줄 — 유채색 포인트는 도트 전용(§2).
                      style: TextStyle(
                          color: colors.text,
                          decoration: TextDecoration.underline),
                    ),
                    TextSpan(text: l.loginLegalAnd),
                    TextSpan(
                      text: l.loginPrivacyPolicy,
                      style: TextStyle(
                          color: colors.text,
                          decoration: TextDecoration.underline),
                    ),
                    TextSpan(text: l.loginLegalSuffix),
                  ],
                ),
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 12, color: colors.textMuted),
              ),

              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }
}
