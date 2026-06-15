package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.ListMemberNotesForAdminUseCase;
import com.qtai.domain.note.api.dto.AdminNoteItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 상세용 노트 조회 서비스.
 *
 * <p>사용자용 NoteService와 분리된 운영 전용 읽기 경로. 회원이 작성한 모든(삭제 제외) 노트를
 * 카테고리/상태 구분 없이 최신순으로 돌려준다. 본문은 제외하고 메타데이터만 노출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNoteQueryService implements ListMemberNotesForAdminUseCase {

    private final NoteRepository noteRepository;

    @Override
    public Page<AdminNoteItem> listNotesByMember(Long memberId, Pageable pageable) {
        return noteRepository.search(memberId, null, null, null, pageable)
                .map(note -> new AdminNoteItem(
                        note.getId(),
                        note.getQtPassageId(),
                        note.getCategory() == null ? null : note.getCategory().name(),
                        note.getStatus() == null ? null : note.getStatus().name(),
                        note.getVisibility() == null ? null : note.getVisibility().name(),
                        note.getTitle(),
                        note.getCreatedAt()
                ));
    }
}
