import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/config/app_config.dart';
import 'routes/app_router.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  AppConfig.initialize();

  runApp(const ProviderScope(child: QTAIApp()));
}

class QTAIApp extends StatelessWidget {
  const QTAIApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'QT AI',
      debugShowCheckedModeBanner: AppConfig.instance.isDev,
      theme: ThemeData(
        colorSchemeSeed: const Color(0xFF6750A4),
        useMaterial3: true,
        fontFamily: 'Pretendard',
      ),
      initialRoute: AppRouter.home,
      onGenerateRoute: AppRouter.onGenerateRoute,
    );
  }
}
