package com.qtai.domain.sharing.internal;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.domain.sharing.api.MarkSourceNoteDeletedUseCase;

/**
 * 원본 노트 삭제 통지 처리 (명세 §4.3.7 — 유지+안내 표시안).
 *
 * <p>의도적으로 note 도메인에 의존하지 않는 독립 빈이다 —
 * note→sharing(이 포트) / sharing→note(발행 스냅샷 조회)가 같은 빈에 모이면
 * 순환 의존이 되므로 분리했다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class SharingSourceNoteService implements MarkSourceNoteDeletedUseCase {

    private final SharingPostRepository sharingPostRepository;

    @Override
    @Transactional
    public void markSourceNoteDeleted(Long noteId, LocalDateTime deletedAt) {
        sharingPostRepository.findByNoteId(noteId).ifPresent(post -> {
            boolean marked = post.markSourceNoteDeleted(deletedAt);
            if (marked) {
                log.info("나눔 글 원본 노트 삭제 기록. sharingPostId={}, noteId={}", post.getId(), noteId);
            }
        });
    }
}
