import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
// TODO(김지민): SSEClient 사용 시 'package:flutter_client_sse/flutter_client_sse.dart' import.

/// AI 대화 화면 (오늘 QT 본문 기반 1회성 Q&A).
///
/// 08_프론트엔드 § 8: SSE 토큰 4종(token / sources / turn_completed / end / error).
///
/// TODO(김지민):
/// - 메시지 입력 시 SSEClient.subscribeToSSE(...) POST /ai/sessions/{id}/turns
/// - token 이벤트: 마지막 assistant 메시지에 delta append
/// - sources 이벤트: 출처 칩 표시
/// - turn_completed: 메시지 마감 + 입력창 활성화
/// - end / [DONE]: 스트림 종료, error → SnackBar
class AiChatScreen extends ConsumerStatefulWidget {
  final int sessionId;
  const AiChatScreen({super.key, required this.sessionId});

  @override
  ConsumerState<AiChatScreen> createState() => _AiChatScreenState();
}

class _AiChatScreenState extends ConsumerState<AiChatScreen> {
  final List<_Msg> _messages = [];
  final TextEditingController _input = TextEditingController();
  bool _streaming = false;

  void _send() {
    final text = _input.text.trim();
    if (text.isEmpty || _streaming) return;
    setState(() {
      _messages.add(_Msg(role: 'USER', content: text));
      _messages.add(_Msg(role: 'ASSISTANT', content: ''));
      _streaming = true;
      _input.clear();
    });
    // TODO: SSEClient.subscribeToSSE(method: SSERequestType.POST,
    //   url: '${baseUrl}/ai/sessions/${widget.sessionId}/turns',
    //   header: { 'Authorization': 'Bearer ...', 'Accept': 'text/event-stream' },
    //   body: { 'userMessage': text } )
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('AI에게 질문하기')),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: _messages.length,
              itemBuilder: (_, i) {
                final m = _messages[i];
                final isUser = m.role == 'USER';
                return Align(
                  alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 4),
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                    decoration: BoxDecoration(
                      color: isUser ? Colors.indigo.shade50 : Colors.grey.shade200,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(m.content),
                  ),
                );
              },
            ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(8),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _input,
                      decoration: const InputDecoration(
                        hintText: '본문에 대해 질문해보세요',
                        border: OutlineInputBorder(),
                      ),
                      onSubmitted: (_) => _send(),
                    ),
                  ),
                  IconButton(
                    onPressed: _send,
                    icon: const Icon(Icons.send),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Msg {
  final String role;
  String content;
  _Msg({required this.role, required this.content});
}
