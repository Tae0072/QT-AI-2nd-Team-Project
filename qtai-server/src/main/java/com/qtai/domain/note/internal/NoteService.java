package com.qtai.domain.note.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.dto.NoteListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 노트 도메인 진입점. 4개 UseCase 구현 + 트랜잭션 경계.
 *
 * 권한 정책:
 * - 조회: 본인 노트만 (memberId 강제 필터) + deletedAt IS NULL
 * - 작성/수정/삭제: 작성자 본인만 (FORBIDDEN)
 *
 * 타 도메인 접근:
 * - member.GetMemberUseCase → client/member 어댑터
 * - qt.GetQtUseCase         → client/qt 어댑터 (qt 존재/권한 검증용)
 *
 * 현재 상태:
 * - ListNotesUseCase: 시그니처만 구현 (본문은 NOT_IMPLEMENTED 반환)
 * - 이승욱 notes Entity·Repository 머지 후 본문 구현 + 단위/통합 테스트 추가 예정
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService implements ListNotesUseCase {

    @Override
    public NoteListResponse list(Long memberId, NoteCategory category, NoteStatus status, String q, Pageable pageable) {
        // 다음 PR에서 구현 예정 (이승욱 notes Entity·Repository 머지 후):
        //   1. noteRepository.findByMemberIdAndDeletedAtIsNull...(memberId, category, status, q, pageable)
        //   2. Page<Note> → List<NoteListItem> 매핑
        //   3. new NoteListResponse(content, page, size, totalElements, totalPages, first, last, sort)
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED,
                "GET /api/v1/notes 본문은 다음 PR(이승욱 notes Entity 머지 후)에서 구현 예정입니다.");
    }
}
