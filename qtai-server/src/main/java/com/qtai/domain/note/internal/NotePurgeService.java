package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.PurgeMemberNoteDataUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * note 도메인 — 회원 보존기간 만료 정리 구현.
 *
 * <p>자기 도메인 테이블(journal_events, note_verses, notes)만 삭제한다.
 * 노트 단위 대량 삭제라 Entity 로딩 없이 JdbcTemplate로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotePurgeService implements PurgeMemberNoteDataUseCase {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public int purgeByMemberId(Long memberId) {
        int deleted = 0;
        // 1) 묵상 일지 이벤트 — 회원 본인 것 + 회원 노트를 참조하는 것 (notes FK 선행 해제)
        deleted += jdbc.update("DELETE FROM journal_events WHERE member_id = ? "
                + "OR note_id IN (SELECT id FROM notes WHERE member_id = ?)", memberId, memberId);
        // 2) 노트-구절 연결
        deleted += jdbc.update("DELETE FROM note_verses WHERE note_id IN "
                + "(SELECT id FROM notes WHERE member_id = ?)", memberId);
        // 3) 노트 본체
        deleted += jdbc.update("DELETE FROM notes WHERE member_id = ?", memberId);
        return deleted;
    }
}
