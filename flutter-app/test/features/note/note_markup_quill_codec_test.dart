import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/note/models/note_markup_quill_codec.dart';

void main() {
  // 마커 평문 → Delta → 마커 평문 라운드트립이 원본과 같아야 한다(저장 형식 보존).
  String roundTrip(String markers) =>
      NoteMarkupQuillCodec.deltaToMarkers(
        NoteMarkupQuillCodec.markersToDelta(markers),
      );

  group('NoteMarkupQuillCodec 라운드트립', () {
    test('일반 텍스트', () {
      expect(roundTrip('평범한 노트 본문'), '평범한 노트 본문');
    });

    test('굵게/기울임/밑줄/취소선', () {
      expect(roundTrip('**굵게**'), '**굵게**');
      expect(roundTrip('//기울임//'), '//기울임//');
      expect(roundTrip('__밑줄__'), '__밑줄__');
      expect(roundTrip('~~취소~~'), '~~취소~~');
    });

    test('글자 크기/색/배경', () {
      expect(roundTrip('[fs=24]큰 글씨[fs=]'), '[fs=24]큰 글씨[fs=]');
      expect(roundTrip('[fg=#DC2626]빨강[fg=]'), '[fg=#DC2626]빨강[fg=]');
      expect(roundTrip('[bg=#FFF2A8]형광[bg=]'), '[bg=#FFF2A8]형광[bg=]');
    });

    test('서식이 섞인 문장', () {
      expect(roundTrip('안녕 **굵게** 그리고 //기울임// 끝'),
          '안녕 **굵게** 그리고 //기울임// 끝');
    });

    test('여러 줄', () {
      expect(roundTrip('첫 줄\n둘째 **줄**\n셋째'), '첫 줄\n둘째 **줄**\n셋째');
    });
  });

  group('NoteMarkupQuillCodec 매핑', () {
    test('굵게는 bold 속성으로 변환된다', () {
      final ops = NoteMarkupQuillCodec.markersToDelta('**강조**').toList();
      expect(ops.first.data, '강조');
      expect(ops.first.attributes?['bold'], true);
    });

    test('색/배경/크기 속성이 올바르게 매핑된다', () {
      final ops =
          NoteMarkupQuillCodec.markersToDelta('[fs=20][fg=#123456][bg=#ABCDEF]x[bg=][fg=][fs=]')
              .toList();
      final attrs = ops.first.attributes!;
      expect(attrs['size'], '20');
      expect(attrs['color'], '#123456');
      expect(attrs['background'], '#ABCDEF');
    });
  });

  group('Quill 문서 생성', () {
    test('변환 결과는 항상 Quill Document로 만들 수 있다(끝 개행 보장)', () {
      for (final sample in [
        '평문',
        '**굵게**',
        '[fg=#DC2626]색[fg=]',
        '여러\n줄\n노트',
        '',
      ]) {
        final delta = NoteMarkupQuillCodec.markersToDelta(sample);
        // 예외 없이 생성되면 통과.
        final doc = Document.fromDelta(delta);
        expect(doc, isNotNull);
      }
    });

    test('빈 문자열은 빈 본문으로 라운드트립된다', () {
      expect(roundTrip(''), '');
    });
  });
}
