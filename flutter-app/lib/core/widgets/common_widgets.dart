import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';

/// 로딩 화면
class LoadingView extends StatelessWidget {
  final String? message;
  const LoadingView({super.key, this.message});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          if (message != null) ...[
            const SizedBox(height: 16),
            Text(message!, style: Theme.of(context).textTheme.bodyMedium),
          ],
        ],
      ),
    );
  }
}

/// 에러 화면
class ErrorView extends StatelessWidget {
  final String message;
  final VoidCallback? onRetry;
  const ErrorView({super.key, required this.message, this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Colors.red),
            const SizedBox(height: 16),
            Text(message,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyLarge),
            if (onRetry != null) ...[
              const SizedBox(height: 16),
              ElevatedButton(onPressed: onRetry, child: Text(AppLocalizations.of(context).commonRetry)),
            ],
          ],
        ),
      ),
    );
  }
}

/// 빈 화면 표시
class EmptyView extends StatelessWidget {
  final String? message;
  final IconData icon;
  const EmptyView({
    super.key,
    this.message,
    this.icon = Icons.inbox_outlined,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 48, color: Colors.grey),
          const SizedBox(height: 16),
          Text(message ?? AppLocalizations.of(context).emptyDefault,
              style: Theme.of(context)
                  .textTheme
                  .bodyLarge
                  ?.copyWith(color: Colors.grey)),
        ],
      ),
    );
  }
}

/// AsyncValue 확장 — 기본 로딩/에러 위젯을 제공하는 when 헬퍼.
///
/// [loading], [error] 콜백을 생략하면 각각 [LoadingView], [ErrorView]를
/// 기본값으로 사용한다. 모든 AsyncValue 상태를 한 줄로 처리할 수 있다.
extension AsyncValueUI<T> on AsyncValue<T> {
  Widget whenOrDefault({
    required Widget Function(T data) data,
    Widget Function()? loading,
    Widget Function(Object error, StackTrace stackTrace)? error,
  }) {
    return when(
      skipLoadingOnRefresh: false,
      data: data,
      loading: () => loading?.call() ?? const LoadingView(),
      error: (e, st) => error?.call(e, st) ?? ErrorView(message: e.toString()),
    );
  }
}
