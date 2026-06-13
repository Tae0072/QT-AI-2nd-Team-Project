import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/note/widgets/note_rich_text_editor.dart';

/// 저장/임시저장은 `controller.text`(마커 평문)를 읽어 백엔드로 보낸다.
/// 새 Quill 기반 컨트롤러에서도 이 입출력이 그대로 동작하는지 검증한다.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('타이핑한 내용이 마커 평문으로 읽힌다(저장 body)', () {
    final controller = NoteRichBodyController();
    controller.quill.document.insert(0, '안녕하세요 반갑습니다');
    expect(controller.text.trim(), '안녕하세요 반갑습니다');
    controller.dispose();
  });

  test('굵게 서식이 ** 마커로 저장된다', () {
    final controller = NoteRichBodyController();
    controller.quill.document.insert(0, '강조');
    controller.quill.document.format(0, 2, Attribute.bold);
    expect(controller.text, '**강조**');
    controller.dispose();
  });

  test('text setter로 기존 마커 본문을 그대로 불러온다(라운드트립)', () {
    final controller = NoteRichBodyController();
    controller.text = '**안녕** 하세요 //반가워//';
    expect(controller.text, '**안녕** 하세요 //반가워//');
    controller.dispose();
  });

  test('빈 본문은 빈 문자열로 읽힌다(저장 막힘 확인)', () {
    final controller = NoteRichBodyController();
    expect(controller.text.trim(), isEmpty);
    controller.dispose();
  });
}
