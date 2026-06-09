package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.JournalChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 묵상 노트 변경을 <b>호출자와 같은 트랜잭션</b>에서 journal_events(PENDING)로 적재하는 트랜잭션 아웃박스(P1-10).
 *
 * <p>리뷰 H-13 보강: 기존엔 {@code AFTER_COMMIT} 이벤트 리스너가 커밋 <i>이후</i> 별도 트랜잭션으로
 * 기록해, "노트 커밋"과 "이벤트 기록" 사이에 크래시가 나면 흔적 없이 유실되는 창이 있었다.
 * 이 아웃박스는 {@link NoteService}의 {@code @Transactional} 경계 안에서 PENDING 행을 적재하므로
 * 노트 변경과 이벤트 적재가 <b>원자적으로 함께 커밋/롤백</b>된다.
 *
 * <p>실제 후속 처리(PENDING→PROCESSED 전이, 재시도)는 {@link JournalEventReprocessor}가 비동기로 수행한다.
 * 따라서 사용자 요청 경로에는 가벼운 적재만 남고, 후속 처리 실패가 사용자 응답(500)으로 전파되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class JournalEventOutbox {

    private final JournalEventRepository journalEventRepository;

    /**
     * 호출자 트랜잭션에 합류해 PENDING 이벤트를 적재한다. 별도 트랜잭션을 열지 않는다.
     *
     * <p>호출자는 반드시 {@code @Transactional} 경계 안에서 호출해야 원자성이 보장된다.
     */
    public void append(JournalChangedEvent event) {
        journalEventRepository.save(JournalEvent.pending(event));
    }
}
