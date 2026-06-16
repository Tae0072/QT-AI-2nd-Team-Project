import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/note_drawing.dart';

/// 노트의 "페이지 모드"와 "손그림(획)"을 **이 기기 안에만** 저장하는 로컬 저장소.
///
/// - 페이지 모드: 작아서 `SharedPreferences`에 키-값으로 저장.
/// - 손그림 획: 양이 많을 수 있어 앱 문서 폴더의 JSON 파일로 저장.
///
/// [canvasKey]는 노트를 구분하는 안정적인 키다.
/// - QT 묵상 노트: `'qt:<qtPassageId>'`
/// - 자유 노트: `'note:<noteId>'`
class NoteCanvasStore {
  static const _modePrefix = 'note_page_mode_';

  /// 페이지 모드 저장. (로컬 캐시는 best-effort — 실패해도 앱 흐름을 막지 않는다)
  Future<void> saveMode(String canvasKey, NotePageMode mode) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('$_modePrefix$canvasKey', mode.storageValue);
    } catch (_) {
      // 저장 실패는 무시(플러그인 미탑재 환경 등).
    }
  }

  /// 페이지 모드 불러오기(없거나 실패하면 일반).
  Future<NotePageMode> loadMode(String canvasKey) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return NotePageMode.fromStorage(prefs.getString('$_modePrefix$canvasKey'));
    } catch (_) {
      return NotePageMode.plain;
    }
  }

  /// 손그림 획 저장(빈 목록이면 파일 삭제).
  Future<void> saveStrokes(
    String canvasKey,
    List<DrawingStroke> strokes,
  ) async {
    try {
      final file = await _strokeFile(canvasKey);
      if (strokes.isEmpty) {
        if (await file.exists()) await file.delete();
        return;
      }
      final data = jsonEncode([for (final s in strokes) s.toJson()]);
      await file.writeAsString(data);
    } catch (_) {
      // 저장 실패는 무시.
    }
  }

  /// 손그림 획 불러오기(없거나 실패하면 빈 목록).
  Future<List<DrawingStroke>> loadStrokes(String canvasKey) async {
    try {
      final file = await _strokeFile(canvasKey);
      if (!await file.exists()) return <DrawingStroke>[];
      final raw = jsonDecode(await file.readAsString()) as List<dynamic>;
      return [
        for (final item in raw)
          DrawingStroke.fromJson(item as Map<String, dynamic>),
      ];
    } catch (_) {
      // 파일이 없거나 깨졌으면 빈 목록으로 안전하게 시작한다.
      return <DrawingStroke>[];
    }
  }

  Future<File> _strokeFile(String canvasKey) async {
    final dir = await getApplicationDocumentsDirectory();
    final safe = canvasKey.replaceAll(RegExp(r'[^A-Za-z0-9_-]'), '_');
    final drawingDir = Directory('${dir.path}/note_drawings');
    if (!await drawingDir.exists()) await drawingDir.create(recursive: true);
    return File('${drawingDir.path}/$safe.json');
  }
}
