import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/auth_interceptor.dart';
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
  final AuthRepository? _repository;

  AuthStatusNotifier(AuthRepository repository)
      : _repository = repository,
        super(AuthStatus.unknown) {
    AuthInterceptor.globalOnAuthFailure = setUnauthenticated;
    _checkAuthStatus();
  }

  /// 테스트 전용 — Dio/SecureStorage 없이 즉시 상태 설정.
  AuthStatusNotifier.withInitial(super.initial) : _repository = null;

  Future<void> _checkAuthStatus() async {
    if (_repository == null) return;
    final hasToken = await _repository.hasToken();
    state = hasToken ? AuthStatus.authenticated : AuthStatus.unauthenticated;
  }

  void setAuthenticated() => state = AuthStatus.authenticated;
  void setUnauthenticated() => state = AuthStatus.unauthenticated;

  @override
  void dispose() {
    if (AuthInterceptor.globalOnAuthFailure == setUnauthenticated) {
      AuthInterceptor.globalOnAuthFailure = null;
    }
    super.dispose();
  }
}
