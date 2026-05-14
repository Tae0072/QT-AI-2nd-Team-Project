import 'package:flutter/material.dart';

/// Google OAuth 로그인 화면. (DECISIONS.md §3: 소프트 로그인 정책)
/// 비로그인도 오늘 QT 미리보기 가능. 로그인 후 AI/Journal 사용.
///
/// TODO(김지민): google_sign_in 패키지로 idToken 수령 → POST /auth/login/google.
class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('QT-AI', style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold)),
            const SizedBox(height: 48),
            FilledButton.icon(
              icon: const Icon(Icons.login),
              label: const Text('Google로 시작'),
              onPressed: () {
                // TODO
              },
            ),
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('나중에 로그인'),
            ),
          ],
        ),
      ),
    );
  }
}
