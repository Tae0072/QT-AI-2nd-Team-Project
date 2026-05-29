import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../services/auth_repository.dart';

/// AuthRepository Provider.
final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(dio: ref.watch(dioProvider));
});

/// 인증 상태 — 로그인 여부.
enum AuthStatus { unknown, authenticated, unauthenticated }

/// 인증 상태 Notifier.
final authStatusProvider =
    StateNotifierProvider<AuthStatusNotifier, AuthStatus>((ref) {
  return AuthStatusNotifier(ref.watch(authRepositoryProvider));
});

class AuthStatusNotifier extends StateNotifier<AuthStatus> {
  final AuthRepository _repository;

  AuthStatusNotifier(this._repository) : super(AuthStatus.unknown) {
    _checkAuthStatus();
  }

  Future<void> _checkAuthStatus() async {
    final hasToken = await _repository.hasToken();
    state = hasToken ? AuthStatus.authenticated : AuthStatus.unauthenticated;
  }

  void setAuthenticated() => state = AuthStatus.authenticated;
  void setUnauthenticated() => state = AuthStatus.unauthenticated;
}
