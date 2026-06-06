import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../models/note_models.dart';

/// 노트 외부 공유 바텀시트를 띄운다. (N-04에서 호출)
///
/// 텍스트 공유 / 카드 이미지 공유 두 가지를 제공한다(§19.1).
Future<void> showNoteShareSheet(BuildContext context, NoteDetail detail) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    showDragHandle: true,
    builder: (_) => _NoteShareSheet(detail: detail),
  );
}

class _NoteShareSheet extends StatefulWidget {
  final NoteDetail detail;

  const _NoteShareSheet({required this.detail});

  @override
  State<_NoteShareSheet> createState() => _NoteShareSheetState();
}

class _NoteShareSheetState extends State<_NoteShareSheet> {
  // ✏️ 카드 위젯을 이미지로 캡처하려면 그 위젯을 RepaintBoundary로 감싸고
  // 이 key로 렌더 객체를 찾아야 한다(아래 _shareAsImage에서 사용).
  final _cardKey = GlobalKey();
  bool _busy = false;

  NoteDetail get detail => widget.detail;

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // 미리보기 카드 — 이 영역이 그대로 이미지로 캡처된다.
            RepaintBoundary(
              key: _cardKey,
              child: _ShareCard(detail: detail),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _busy ? null : _shareAsText,
                    icon: const Icon(Icons.text_fields),
                    label: Text(l.noteShareAsText),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton.icon(
                    onPressed: _busy ? null : _shareAsImage,
                    icon: const Icon(Icons.image_outlined),
                    label: Text(l.noteShareAsImage),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// 텍스트 공유 — 제목+본문을 문자열로 만들어 OS 공유 시트로 보낸다.
  Future<void> _shareAsText() async {
    // ✏️ share_plus 12.x API: SharePlus.instance.share(ShareParams(...)).
    await SharePlus.instance.share(ShareParams(text: _buildShareText(detail)));
  }

  /// 카드 이미지 공유 — RepaintBoundary를 PNG로 캡처 → 임시파일 저장 → 공유.
  Future<void> _shareAsImage() async {
    setState(() => _busy = true);
    try {
      final boundary =
          _cardKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      // ✏️ pixelRatio를 올리면 더 선명한 이미지가 나온다(화면 밀도 무관 고해상도).
      final image = await boundary.toImage(pixelRatio: 3.0);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      final bytes = byteData!.buffer.asUint8List();

      // ✏️ 공유는 실제 파일 경로가 안정적이라 임시 디렉터리에 저장 후 넘긴다.
      final dir = await getTemporaryDirectory();
      final file = File('${dir.path}/qt_note_${detail.id}.png');
      await file.writeAsBytes(bytes);

      await SharePlus.instance.share(
        ShareParams(files: [XFile(file.path)], text: detail.title),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppLocalizations.of(context).noteShareImageFailed)),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  /// 공유용 텍스트 조립. 자유노트는 body, 묵상노트는 4섹션을 이어붙인다.
  String _buildShareText(NoteDetail d) {
    final buf = StringBuffer();
    if (d.title.isNotEmpty) buf.writeln(d.title);
    buf.writeln();
    if (d.isFreeNote) {
      if ((d.body ?? '').isNotEmpty) buf.writeln(d.body);
    } else {
      _appendSection(buf, '느낀 점', d.rememberSection);
      _appendSection(buf, '기억할 구절', d.interpretSection);
      _appendSection(buf, '적용할 점', d.applySection);
      _appendSection(buf, '기도', d.praySection);
    }
    buf.writeln('\n— QT-AI');
    return buf.toString().trim();
  }

  void _appendSection(StringBuffer buf, String label, String? text) {
    if (text == null || text.isEmpty) return;
    buf.writeln('[$label]');
    buf.writeln(text);
    buf.writeln();
  }
}

/// 공유용 미리보기 카드(이미지로 캡처되는 위젯).
class _ShareCard extends StatelessWidget {
  final NoteDetail detail;

  const _ShareCard({required this.detail});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            detail.title.isEmpty ? l.noteUntitled : detail.title,
            style: theme.textTheme.titleMedium,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 12),
          Text(
            _previewBody(detail),
            style: theme.textTheme.bodyMedium,
            maxLines: 8,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 16),
          Align(
            alignment: Alignment.centerRight,
            child: Text('— QT-AI',
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.primary)),
          ),
        ],
      ),
    );
  }

  // 카드에 보여줄 본문 요약 — 자유노트는 body, 묵상은 4섹션을 합친다.
  String _previewBody(NoteDetail d) {
    if (d.isFreeNote) return (d.body ?? '').isEmpty ? '(내용 없음)' : d.body!;
    final parts = [
      d.rememberSection,
      d.interpretSection,
      d.applySection,
      d.praySection,
    ].where((e) => e != null && e.isNotEmpty).cast<String>();
    return parts.isEmpty ? '(내용 없음)' : parts.join('\n\n');
  }
}
