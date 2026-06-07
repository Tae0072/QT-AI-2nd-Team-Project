/// 플랫폼별 로컬 파일 저장 헬퍼의 진입점.
///
/// 조건부 import로 구현을 분리한다.
/// - 모바일/데스크톱(`dart.library.io` 사용 가능): 실제 파일 시스템 사용
///   → `file_storage_io.dart`
/// - 웹(브라우저): 파일 시스템이 없으므로 안전한 대체 동작
///   → `file_storage_stub.dart`
///
/// 이렇게 분리하면 앱 코드가 `dart:io`를 직접 import하지 않게 되어,
/// 웹 빌드(`flutter run -d chrome`)가 깨지지 않는다.
library;

export 'file_storage_stub.dart' if (dart.library.io) 'file_storage_io.dart';
