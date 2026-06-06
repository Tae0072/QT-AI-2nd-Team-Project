import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_theme.dart';
import '../../../routes/app_router.dart';
import '../providers/auth_providers.dart';

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
        Navigator.of(context).pushReplacementNamed(AppRouter.nicknameSetup);
      } else {
        ref.read(authStatusProvider.notifier).setAuthenticated();
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMessage = '로그인에 실패했습니다. 다시 시도해주세요.';
      });
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.bg,
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
                  TextSpan(text: '·', style: TextStyle(color: AppTheme.accent)),
                  const TextSpan(text: 'AI'),
                ]),
                style: const TextStyle(
                  fontFamily: 'GowunDodum',
                  fontSize: 40,
                  fontWeight: FontWeight.w400,
                  color: AppTheme.text,
                  letterSpacing: -1,
                ),
              ),
              const SizedBox(height: 18),

              // 헤드라인
              const Text(
                '매일의 묵상을\n가장 단순하게.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'GowunDodum',
                  fontSize: 28,
                  fontWeight: FontWeight.w400,
                  color: AppTheme.text,
                  height: 1.2,
                  letterSpacing: -0.4,
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                '오늘의 QT 본문과 검증된 해설을 읽고,\n나만의 묵상 노트를 남겨보세요.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 17, color: AppTheme.textMuted, height: 1.5),
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
                  onPressed: _isLoading ? null : _handleKakaoLogin,
                  icon: _isLoading
                      ? const SizedBox(
                          width: 20, height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFF191600)))
                      : const Icon(Icons.chat_bubble, size: 20),
                  label: const Text('카카오로 시작하기'),
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

              const SizedBox(height: 18),

              // 법적 고지
              Text.rich(
                TextSpan(
                  text: '계속 진행하면 ',
                  children: [
                    TextSpan(
                      text: '이용약관',
                      style: TextStyle(color: AppTheme.accent),
                    ),
                    const TextSpan(text: ' 및 '),
                    TextSpan(
                      text: '개인정보처리방침',
                      style: TextStyle(color: AppTheme.accent),
                    ),
                    const TextSpan(text: '에\n동의하는 것으로 간주합니다.'),
                  ],
                ),
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 12, color: AppTheme.textMuted),
              ),

              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }
}
