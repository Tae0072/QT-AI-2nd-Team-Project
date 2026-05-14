import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 묵상 노트 편집 화면.
///
/// MVP UX (DECISIONS.md §3.1):
/// - 4필드 자동 저장: felt, memorableVerse, application, prayer.
/// - 저장 버튼 없음, 글자 수 제한 노출 없음.
/// - PATCH /api/v1/journals/{id} 호출은 debounce 600ms.
///
/// TODO(김지민):
/// - Riverpod AsyncNotifier로 GET /api/v1/journals/{id} → 4필드 controller에 채우기
/// - 각 controller listener에서 _debounceSave 호출
class JournalEditorScreen extends ConsumerStatefulWidget {
  final int journalId;
  const JournalEditorScreen({super.key, required this.journalId});

  @override
  ConsumerState<JournalEditorScreen> createState() => _JournalEditorScreenState();
}

class _JournalEditorScreenState extends ConsumerState<JournalEditorScreen> {
  final _felt = TextEditingController();
  final _memorableVerse = TextEditingController();
  final _application = TextEditingController();
  final _prayer = TextEditingController();
  Timer? _debounce;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    for (final c in [_felt, _memorableVerse, _application, _prayer]) {
      c.addListener(_debounceSave);
    }
    // TODO: GET /api/v1/journals/{widget.journalId} → 4필드 채우기
  }

  void _debounceSave() {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 600), () async {
      setState(() => _saving = true);
      // TODO: PATCH /api/v1/journals/{widget.journalId} body = 4필드
      await Future.delayed(const Duration(milliseconds: 300));
      if (mounted) setState(() => _saving = false);
    });
  }

  @override
  void dispose() {
    _debounce?.cancel();
    for (final c in [_felt, _memorableVerse, _application, _prayer]) {
      c.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('묵상 노트'),
        actions: [
          if (_saving)
            const Padding(
              padding: EdgeInsets.only(right: 16),
              child: Center(child: Text('저장 중…', style: TextStyle(fontSize: 12))),
            )
          else
            const Padding(
              padding: EdgeInsets.only(right: 16),
              child: Center(child: Text('자동 저장됨', style: TextStyle(fontSize: 12, color: Colors.green))),
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _field('느낀 점', _felt),
          _field('마음에 남는 구절', _memorableVerse),
          _field('적용 / 결단', _application),
          _field('기도', _prayer),
        ],
      ),
    );
  }

  Widget _field(String label, TextEditingController controller) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: TextField(
        controller: controller,
        maxLines: null, // 글자 수 제한 노출 안 함
        minLines: 3,
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
        ),
      ),
    );
  }
}
