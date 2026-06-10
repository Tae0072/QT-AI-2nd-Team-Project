import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// 모바일/데스크톱용 로컬 파일 저장 구현.
///
/// 이 파일은 `file_storage.dart`의 조건부 import를 통해
/// `dart.library.io`가 있는 플랫폼에서만 로드된다.

/// 앱 문서 폴더 하위 [subdir] 디렉터리를 보장(없으면 생성)하고 경로를 반환한다.
Future<String> ensureCacheDir(String subdir) async {
  final base = await getApplicationDocumentsDirectory();
  final dir = Directory('${base.path}/$subdir');
  if (!await dir.exists()) {
    await dir.create(recursive: true);
  }
  return dir.path;
}

/// [path] 파일 존재 여부.
Future<bool> fileExists(String path) => File(path).exists();

/// [path] 디렉터리가 있으면 통째로 삭제한다.
Future<void> deleteDirIfExists(String path) async {
  final dir = Directory(path);
  if (await dir.exists()) {
    await dir.delete(recursive: true);
  }
}

/// 임시 폴더에 [bytes]를 [filename]으로 저장하고 파일 경로를 반환한다.
Future<String> saveTempBytes(String filename, List<int> bytes) async {
  final dir = await getTemporaryDirectory();
  final file = File('${dir.path}/$filename');
  await file.writeAsBytes(bytes);
  return file.path;
}
