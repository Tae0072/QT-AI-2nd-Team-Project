import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../routes/app_router.dart';
import '../models/note_models.dart';
import '../providers/note_providers.dart';

/// 개인 노트 작성 화면 (N-03).
///
/// - N-02에서 arguments로 받은 카테고리(기도/회개/감사)로 작성
/// - 제목 + 본문 1섹션, 저장/임시저장 버튼 (자동저장 없음)
/// - 저장 성공 → 목록(N-01)까지 돌아가고 목록 새로고침
class NoteEditScreen extends ConsumerStatefulWidget {
  const NoteEditScreen({super.key});

  @override
  ConsumerState<NoteEditScreen> createState() => _NoteEditScreenState();
}

class _NoteEditScreenState extends ConsumerState<NoteEditScreen> {
  final _titleController = TextEditingController();
  final _bodyController = TextEditingController();
  bool _saving = false; // 저장 중복 클릭/이중 저장 방지

  @override
  void dispose() {
    _titleController.dispose();
    _bodyController.dispose();
    super.dispose();
  }

  /// 저장 처리. status = 'SAVED'(저장) | 'DRAFT'(임시저장).
  Future<void> _save(String category, String status) async {
    final title = _titleController.text.trim();
    final body = _bodyController.text.trim();

    // ✏️ 왜 이렇게 짰냐면:
    // 저장(SAVED)은 04 명세상 본문이 필수라 비면 막는다.
    // 임시저장(DRAFT)은 작성 중 보관이 목적이라, 제목·본문 둘 다 빈 경우만 막는다.
    if (status == 'SAVED' && body.isEmpty) {
      _showMessage('본문을 입력해 주세요');
      return;
    }
    if (status == 'DRAFT' && title.isEmpty && body.isEmpty) {
      _showMessage('제목이나 본문을 입력해 주세요');
      return;
    }

    setState(() => _saving = true);
    try {
      await ref.read(noteRepositoryProvider).create(
            category: category,
            title: title,
            body: body,
            status: status,
          );
      // ✏️ 저장 성공: 목록 provider를 무효화해 N-01이 새 노트를 다시 불러오게 하고,
      // 카테고리 선택(N-02)을 건너뛰고 목록까지 한 번에 돌아간다.
      ref.invalidate(notesProvider);
      if (!mounted) return;
      Navigator.of(context).popUntil(
        (route) => route.settings.name == AppRouter.noteList || route.isFirst,
      );
      _showMessage(status == 'SAVED' ? '저장되었습니다' : '임시저장되었습니다');
    } catch (e) {
      // ✏️ 실패 시 저장되지 않았음을 명확히 알리고 화면은 유지(재시도 가능).
      if (!mounted) return;
      setState(() => _saving = false);
      _showMessage('저장에 실패했습니다. 다시 시도해 주세요');
    }
  }

  void _showMessage(String msg) {
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    // ✏️ N-02에서 pushNamed(arguments: code)로 넘겨준 카테고리 코드를 꺼낸다.
    final category =
        ModalRoute.of(context)?.settings.arguments as String? ?? 'PRAYER';

    return Scaffold(
      appBar: AppBar(
        title: Text('${noteCategoryLabel(category)} 노트 작성'),
        centerTitle: true,
      ),
      body: AbsorbPointer(
        absorbing: _saving, // 저장 중엔 입력 막기
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(
                controller: _titleController,
                decoration: const InputDecoration(
                  labelText: '제목 (선택)',
                  border: OutlineInputBorder(),
                ),
                textInputAction: TextInputAction.next,
              ),
              const SizedBox(height: 12),
              Expanded(
                child: TextField(
                  controller: _bodyController,
                  decoration: const InputDecoration(
                    labelText: '본문',
                    alignLabelWithHint: true,
                    border: OutlineInputBorder(),
                  ),
                  maxLines: null, // 여러 줄 입력
                  expands: true,
                  textAlignVertical: TextAlignVertical.top,
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed:
                          _saving ? null : () => _save(category, 'DRAFT'),
                      child: const Text('임시저장'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton(
                      onPressed:
                          _saving ? null : () => _save(category, 'SAVED'),
                      child: _saving
                          ? const SizedBox(
                              height: 18,
                              width: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text('저장'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
