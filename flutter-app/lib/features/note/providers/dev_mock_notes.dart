import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../mypage/providers/mypage_providers.dart';
import '../models/note_models.dart';

/// ⚠️ 테스트(목업) 전용 — 운영 배포 전 제거 대상.
///
/// 목적: '내 계정'에서만 기록(노트) 목록에 가짜 테스트 노트 2개가 보이게 한다.
/// 안전장치(운영에 절대 안 섞이도록):
///   1) [kDebugMode] 가 아닐 때(=릴리스 빌드)는 컴파일 단계에서 분기 자체가 제거된다.
///   2) 로그인한 계정 이메일이 [kMockNoteAccountEmail] 과 정확히 같을 때만 끼운다.
///   3) DB를 전혀 건드리지 않는다(메모리상 목록 앞에 덧붙이기만). 서버 데이터 0 오염.
///   4) id를 음수(절대 실제와 충돌 안 함)로 둬 진짜 노트와 구분, 제목에 '[목업]' 표시.
const String kMockNoteAccountEmail = 'rkdxodh41@gmail.com';

/// 기록 목록에 끼울 가짜 노트 2개(테스트용).
List<NoteListItem> debugMockNoteItems() {
  final now = DateTime.now();
  return [
    NoteListItem(
      id: -9001,
      category: kNoteCatGratitude,
      title: '[목업] 감사 노트 (테스트)',
      bodyPreview: '이 노트는 디버그 빌드의 내 계정에서만 보이는 테스트용 가짜 노트입니다.',
      status: 'SAVED',
      visibility: 'PRIVATE',
      shared: false,
      createdAt: now,
      updatedAt: now,
    ),
    NoteListItem(
      id: -9002,
      category: kNoteCatPrayer,
      title: '[목업] 기도 노트 (테스트)',
      bodyPreview: '운영 데이터가 아니며, 탭하면 상세가 없어 동작하지 않을 수 있습니다.',
      status: 'SAVED',
      visibility: 'PRIVATE',
      shared: false,
      createdAt: now,
      updatedAt: now,
    ),
  ];
}

/// 디버그 + 내 계정일 때만 [base] 목록 맨 앞에 목업 2개를 끼워 돌려준다.
/// 그 외(릴리스, 다른 계정, 프로필 조회 실패)에는 [base] 를 그대로 돌려준다.
Future<NoteListResponse> withDebugMockNotes(Ref ref, NoteListResponse base) async {
  if (!kDebugMode) return base; // 릴리스: 아래 코드는 트리 셰이킹으로 제거됨.
  try {
    final profile = await ref.read(profileProvider.future);
    if (profile.email != kMockNoteAccountEmail) return base;
  } catch (_) {
    return base; // 프로필을 못 읽으면 원본 그대로(앱 안전).
  }
  return NoteListResponse(
    items: [...debugMockNoteItems(), ...base.items],
    hasNext: base.hasNext,
  );
}
