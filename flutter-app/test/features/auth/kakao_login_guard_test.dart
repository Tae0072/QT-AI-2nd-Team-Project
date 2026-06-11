import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/auth/services/kakao_login_guard.dart';

/// 웹 카카오 로그인 가드 판정(순수 함수) 테스트 (코드리뷰 TODO 2).
///
/// kIsWeb은 테스트에서 분기 제어가 어려워 판정을 순수 함수로 분리했다 —
/// 여기서 4가지 조합을 고정한다. 화면 연결(버튼 비활성+안내)은 login_screen이
/// 이 함수 결과만 소비하므로 판정 고정으로 충분하다.
void main() {
  group('isKakaoLoginUnsupported', () {
    test('웹 + 우회 꺼짐 → 미지원(버튼 비활성 + 안내)', () {
      expect(
        isKakaoLoginUnsupported(isWeb: true, webDevBypassEnabled: false),
        isTrue,
      );
    });

    test('웹 + dev 우회 켜짐 → 가드 제외(기존 dev 동작 유지)', () {
      expect(
        isKakaoLoginUnsupported(isWeb: true, webDevBypassEnabled: true),
        isFalse,
      );
    });

    test('모바일 → 항상 지원(우회 여부 무관)', () {
      expect(
        isKakaoLoginUnsupported(isWeb: false, webDevBypassEnabled: false),
        isFalse,
      );
      expect(
        isKakaoLoginUnsupported(isWeb: false, webDevBypassEnabled: true),
        isFalse,
      );
    });
  });
}
