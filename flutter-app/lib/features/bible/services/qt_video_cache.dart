import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';

class QtVideoCache {
  static const _directoryName = 'qt-video-cache';
  static const _filePrefix = 'qt-video-';
  static const _maxCacheAge = Duration(hours: 24);

  @visibleForTesting
  static Directory? debugCacheRootOverride;

  static Future<File?> existingFile({
    required String cacheKey,
    required String videoUrl,
  }) async {
    final uri = Uri.tryParse(videoUrl);
    if (uri == null || (!uri.isScheme('http') && !uri.isScheme('https'))) {
      return null;
    }

    try {
      final directory = await _cacheDirectory();
      final fileName = _fileNameFor(cacheKey);
      await _deleteStaleFiles(directory, keepFileName: fileName);

      final file = File(_join(directory.path, fileName));
      if (await file.exists() &&
          await file.length() > 0 &&
          !await _isExpired(file)) {
        return file;
      }

      return null;
    } catch (_) {
      return null;
    }
  }

  static Future<void> download({
    required String cacheKey,
    required String videoUrl,
  }) async {
    final uri = Uri.tryParse(videoUrl);
    if (uri == null || (!uri.isScheme('http') && !uri.isScheme('https'))) {
      return;
    }

    try {
      final directory = await _cacheDirectory();
      final fileName = _fileNameFor(cacheKey);
      await _deleteStaleFiles(directory, keepFileName: fileName);

      final file = File(_join(directory.path, fileName));
      if (await file.exists() && await file.length() > 0) {
        return;
      }
      await _download(uri, file);
    } catch (_) {
      // Network cache is an optimization; playback can continue streaming.
    }
  }

  static Future<Directory> _cacheDirectory() async {
    final root = debugCacheRootOverride ?? await getTemporaryDirectory();
    final directory = Directory(_join(root.path, _directoryName));
    if (!await directory.exists()) {
      await directory.create(recursive: true);
    }
    return directory;
  }

  static Future<void> _deleteStaleFiles(
    Directory directory, {
    required String keepFileName,
  }) async {
    if (!await directory.exists()) {
      return;
    }

    await for (final entity in directory.list(followLinks: false)) {
      if (entity is! File) {
        continue;
      }
      final name =
          entity.uri.pathSegments.isEmpty ? '' : entity.uri.pathSegments.last;
      if (!name.startsWith(_filePrefix)) {
        continue;
      }
      if (name != keepFileName || await _isExpired(entity)) {
        await _deleteQuietly(entity);
      }
    }
  }

  static Future<File?> _download(Uri uri, File destination) async {
    final tempFile = File('${destination.path}.part');
    if (await tempFile.exists()) {
      await _deleteQuietly(tempFile);
    }

    final client = HttpClient()
      ..connectionTimeout = const Duration(seconds: 10);
    try {
      final request = await client.getUrl(uri);
      request.followRedirects = true;
      final response = await request.close();
      final isSuccess = response.statusCode == HttpStatus.ok ||
          response.statusCode == HttpStatus.partialContent;
      if (!isSuccess) {
        return null;
      }

      await response.pipe(tempFile.openWrite());
      if (await destination.exists()) {
        await _deleteQuietly(destination);
      }
      await tempFile.rename(destination.path);
      return destination;
    } finally {
      client.close(force: true);
      if (await tempFile.exists()) {
        await _deleteQuietly(tempFile);
      }
    }
  }

  static Future<void> _deleteQuietly(File file) async {
    try {
      await file.delete();
    } catch (_) {
      // Cache cleanup should never block playback.
    }
  }

  static Future<bool> _isExpired(File file) async {
    try {
      final stat = await file.stat();
      return stat.modified.isBefore(DateTime.now().subtract(_maxCacheAge));
    } catch (_) {
      return true;
    }
  }

  static String _fileNameFor(String cacheKey) {
    final safeKey = cacheKey.replaceAll(RegExp(r'[^A-Za-z0-9._-]'), '_');
    if (safeKey.toLowerCase().endsWith('.mp4')) {
      return safeKey;
    }
    return '$safeKey.mp4';
  }

  static String _join(String left, String right) {
    if (left.endsWith(Platform.pathSeparator)) {
      return '$left$right';
    }
    return '$left${Platform.pathSeparator}$right';
  }
}

String qtVideoCacheKey(int qtPassageId, String videoUrl) {
  final uri = Uri.tryParse(videoUrl);
  final fileName = uri == null || uri.pathSegments.isEmpty
      ? 'video.mp4'
      : uri.pathSegments.last;
  final safeFileName = fileName.replaceAll(RegExp(r'[^A-Za-z0-9._-]'), '_');
  final extensionIndex = safeFileName.toLowerCase().lastIndexOf('.mp4');
  final baseName = extensionIndex > 0
      ? safeFileName.substring(0, extensionIndex)
      : safeFileName;
  final extension =
      extensionIndex > 0 ? safeFileName.substring(extensionIndex) : '';
  final urlHash = _stableUrlHash(uri?.toString() ?? videoUrl);
  return 'qt-video-$qtPassageId-$baseName-$urlHash$extension';
}

String _stableUrlHash(String value) {
  var hash = 0x811c9dc5;
  for (final codeUnit in value.codeUnits) {
    hash ^= codeUnit;
    hash = (hash * 0x01000193) & 0xffffffff;
  }
  return hash.toRadixString(16).padLeft(8, '0');
}
