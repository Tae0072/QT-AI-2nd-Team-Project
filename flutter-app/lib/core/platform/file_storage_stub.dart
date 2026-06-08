/// 웹용 로컬 파일 저장 대체 구현.
///
/// 웹(브라우저)에는 로컬 파일 시스템이 없으므로 디스크 캐시/임시파일을
/// 지원하지 않는다. 시그니처는 `file_storage_io.dart`와 1:1로 동일해야 한다.
library;

/// 웹은 디스크 캐시를 지원하지 않는다.
Future<String> ensureCacheDir(String subdir) async =>
    throw UnsupportedError('웹에서는 로컬 파일 캐시를 지원하지 않습니다.');

/// 웹에는 로컬 파일이 없으므로 항상 false.
Future<bool> fileExists(String path) async => false;

/// 웹은 삭제할 로컬 디렉터리가 없으므로 아무 일도 하지 않는다.
Future<void> deleteDirIfExists(String path) async {}

/// 웹은 임시 파일 저장을 지원하지 않는다.
Future<String> saveTempBytes(String filename, List<int> bytes) async =>
    throw UnsupportedError('웹에서는 임시 파일 저장을 지원하지 않습니다.');
