import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../routes/app_router.dart';
import '../models/note_models.dart';
import '../providers/note_providers.dart';
import '../widgets/note_rich_text_editor.dart';

/// N-03 라우트 인자.
///
/// - 작성(N-02→N-03): category만 채워 보냄 (noteId=null)
/// - 수정(N-04→N-03): noteId를 채워 보냄 → 편집 모드
///
/// 왜 클래스로 받냐면: 문자열/숫자 타입으로 모드를 구분하면 헷갈리고 깨지기 쉬워,
/// "무엇을 넘기는지"가 코드에 드러나는 전용 인자 객체로 받는다.
class NoteEditArgs {
  final String? category; // 작성 모드에서 필수
  final int? noteId; // 수정 모드에서 필수 (null이면 작성)

  const NoteEditArgs({this.category, this.noteId});

  bool get isEdit => noteId != null;
}

/// 개인 노트 작성/수정 화면 (N-03).
///
/// - 작성: N-02에서 NoteEditArgs(category)로 진입 → POST
/// - 수정: N-04에서 NoteEditArgs(noteId)로 진입 → 기존 노트 1회 조회해 폼 채움 → PATCH
/// - 제목 + 본문 1섹션, 저장/임시저장 버튼 (자동저장 없음)
/// - 본문은 QT 노트와 공유하는 리치텍스트 에디터(서식·@멘션·라이브 프리뷰) 사용 (QA ③⑨)
class NoteEditScreen extends ConsumerStatefulWidget {
  const NoteEditScreen({super.key});

  @override
  ConsumerState<NoteEditScreen> createState() => _NoteEditScreenState();
}

class _NoteEditScreenState extends ConsumerState<NoteEditScreen> {
  final _titleController = TextEditingController();
  final _bodyController = NoteRichBodyController();
  bool _saving = false; // 저장 중복 클릭/이중 저장 방지
  bool _initialized = false; // didChangeDependencies 1회 가드
  bool _loading = false; // 편집모드: 기존 노트 불러오는 중
  bool _loadError = false; // 편집모드: 불러오기 실패
  NoteEditArgs _args = const NoteEditArgs(category: 'PRAYER');

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // 라우트 인자는 여기서 안전하게 읽을 수 있다. build는 여러 번 불리므로
    // _initialized 가드로 "1회만" 인자를 읽고 편집 데이터를 불러온다(approach A).
    if (_initialized) return;
    _initialized = true;
    final args = ModalRoute.of(context)?.settings.arguments;
    if (args is NoteEditArgs) {
      _args = args;
    }
    if (_args.isEdit) {
      _loadForEdit();
    }
  }

  /// 편집모드: 기존 노트를 한 번 조회해 폼에 채운다(one-shot).
  Future<void> _loadForEdit() async {
    setState(() => _loading = true);
    try {
      final detail =
          await ref.read(noteRepositoryProvider).getDetail(_args.noteId!);
      // 컨트롤러는 '한 번만' 채우면 그 뒤는 사용자 입력이 주인이다.
      // 자유노트라 본문은 body에 있다(묵상 4섹션은 N-03 편집 대상 아님).
      _titleController.text = detail.title;
      _bodyController.text = detail.body ?? '';
      if (!mounted) return;
      setState(() => _loading = false);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _loadError = true;
      });
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _bodyController.dispose();
    super.dispose();
  }

  /// 저장 처리. status = 'SAVED'(저장) | 'DRAFT'(임시저장).
  Future<void> _save(String status) async {
    final l = AppLocalizations.of(context);
    final title = _titleController.text.trim();
    final body = _bodyController.text.trim();

    // ✏️ 왜 이렇게 짰냐면:
    // 저장(SAVED)은 04 명세상 본문이 필수라 비면 막는다.
    // 임시저장(DRAFT)은 작성 중 보관이 목적이라, 제목·본문 둘 다 빈 경우만 막는다.
    if (status == 'SAVED' && body.isEmpty) {
      _showMessage(l.noteEditBodyRequired);
      return;
    }
    if (status == 'DRAFT' && title.isEmpty && body.isEmpty) {
      _showMessage(l.noteEditTitleOrBodyRequired);
      return;
    }

    setState(() => _saving = true);
    try {
      final repository = ref.read(noteRepositoryProvider);
      if (_args.isEdit) {
        // ✏️ 수정: 기존 노트를 PATCH. 목록은 무효화하고, 한 단계만 pop해
        // 상세화면(N-04)으로 복귀한다 → 상세가 돌아오며 스스로 최신으로 재조회한다.
        await repository.update(
          _args.noteId!,
          title: title,
          body: body,
          status: status,
        );
        ref.invalidate(notesProvider);
        // 작성/수정 시 묵상 달력 체크리스트가 자동 ✓ 되도록 달력도 무효화(모든 월).
        ref.invalidate(meditationCalendarProvider);
        if (!mounted) return;
        Navigator.of(context).pop();
      } else {
        // ✏️ 작성: POST 후 목록을 무효화하고 N-02를 건너뛰어 목록까지 돌아간다.
        await repository.create(
          category: _args.category ?? 'PRAYER',
          title: title,
          body: body,
          status: status,
        );
        ref.invalidate(notesProvider);
        ref.invalidate(meditationCalendarProvider);
        if (!mounted) return;
        Navigator.of(context).popUntil(
          (route) => route.settings.name == AppRouter.noteList || route.isFirst,
        );
      }
      _showMessage(status == 'SAVED' ? l.noteSaved : l.noteDraftSaved);
    } catch (e) {
      // ✏️ 실패 시 저장되지 않았음을 명확히 알리고 화면은 유지(재시도 가능).
      if (!mounted) return;
      setState(() => _saving = false);
      _showMessage(l.noteSaveFailed);
    }
  }

  void _showMessage(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final category = _args.category ?? 'PRAYER';
    final modeLabel = _args.isEdit ? l.commonEdit : l.noteModeWrite;

    // ✏️ 편집모드에서 기존 노트를 불러오는 동안/실패 시 폼 대신 상태 화면을 보여준다.
    if (_loading) {
      return Scaffold(
        appBar: AppBar(
            title: Text('${l.noteListTitle} $modeLabel'), centerTitle: true),
        body: const Center(child: CircularProgressIndicator()),
      );
    }
    if (_loadError) {
      return Scaffold(
        appBar: AppBar(
            title: Text('${l.noteListTitle} $modeLabel'), centerTitle: true),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(l.noteLoadFailed),
              const SizedBox(height: 8),
              OutlinedButton(
                onPressed: () => Navigator.of(context).pop(),
                child: Text(l.commonBack),
              ),
            ],
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(
            '${noteCategoryLabel(category)} ${l.noteListTitle} $modeLabel'),
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
                decoration: InputDecoration(
                  labelText: l.noteEditTitleLabel,
                  border: const OutlineInputBorder(),
                ),
                textInputAction: TextInputAction.next,
              ),
              const SizedBox(height: 12),
              // ✏️ 본문 편집(서식 툴바·@멘션·라이브 프리뷰)은 QT 노트와 공유하는
              // 리치텍스트 에디터에 위임한다(QA ③⑨). 저장 시 _bodyController.text를 읽는다.
              Expanded(
                child: NoteRichTextEditor(
                  controller: _bodyController,
                  bodyLabel: l.noteEditBodyLabel,
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: _saving ? null : () => _save('DRAFT'),
                      child: Text(l.noteDraft),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton(
                      onPressed: _saving ? null : () => _save('SAVED'),
                      child: _saving
                          ? const SizedBox(
                              height: 18,
                              width: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(l.commonSave),
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
