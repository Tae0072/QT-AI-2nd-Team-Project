// [DEV_MODE] =================================================================
// 개발자 모드 — 설정 화면의 "버전 정보"를 5번 연속 탭 → 비밀번호 입력 → 진입.
// 온보딩 화면 다시 보기 / 카카오 로그인 로그 확인용.
//
// ★ 개발 종료 시 제거 방법:
//   1) 프로젝트 전체에서 `[DEV_MODE]` 를 검색해 표시된 코드/임포트를 삭제한다.
//   2) 이 파일(dev_mode.dart)과 features/dev/dev_mode_screen.dart 를 삭제한다.
// ===========================================================================
import 'package:flutter/foundation.dart';

/// [DEV_MODE] 개발자 모드 진입 비밀번호.
/// 평문 비밀번호를 소스에 커밋하지 않도록 실행 시 주입한다(미지정 시 빈 값 → 개발자 모드 잠김).
///   --dart-define=DEV_MODE_PASSWORD=qtai-admin-1234
/// dev-up.ps1 이 자동으로 주입한다.
const String kDevModePassword =
    String.fromEnvironment('DEV_MODE_PASSWORD', defaultValue: '');

/// [DEV_MODE] 앱 버전 — pubspec.yaml 의 `version:` 값과 수동 동기화한다.
const String kAppVersion = '0.1.0';
const String kAppBuild = '1';

/// [DEV_MODE] 카카오 로그인 흐름 로그 버퍼.
///
/// 인메모리(앱 종료 시 소멸)이며 최근 200줄만 유지한다.
/// 로그인 단계/결과/실패를 기록해 개발자 모드 화면에서 확인한다.
class KakaoLoginLog {
  KakaoLoginLog._();

  /// 로그 목록(최신이 뒤). 화면은 이 ValueNotifier를 구독해 자동 갱신한다.
  static final ValueNotifier<List<String>> entries =
      ValueNotifier<List<String>>(<String>[]);

  /// 로그 한 줄 추가(시각 prefix 포함).
  static void add(String message) {
    final t = DateTime.now();
    final ts =
        '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}:${t.second.toString().padLeft(2, '0')}';
    final next = List<String>.from(entries.value)..add('[$ts] $message');
    if (next.length > 200) {
      next.removeRange(0, next.length - 200);
    }
    entries.value = next;
  }

  /// 로그 비우기.
  static void clear() => entries.value = <String>[];
}
// [DEV_MODE] end =============================================================
