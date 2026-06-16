package com.qtai.domain.qt.internal;

import com.qtai.domain.qtvideo.api.PrepareQtVideoClipUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 관리자 QT 본문 등록/수정/게시 후, 같은 프로세스(admin-server)에서 QT영상 클립을 자동 준비하는 오케스트레이터.
 *
 * <p>설계 배경: admin-server는 #686으로 qtvideo 도메인({@link PrepareQtVideoClipUseCase})을 갖게 되어,
 * 별도 폴링 스케줄러나 service-bible 호출 없이 <b>인-프로세스·공유 DB</b>로 클립을 즉시 준비할 수 있다.
 * (이전 service-bible 주기 스케줄러 방식은 수동 클립을 덮어쓰고 폴링 비용이 있어 폐기)
 *
 * <p>흐름(본문 트랜잭션 <b>커밋 이후</b> 실행):
 * <ol>
 *   <li>{@link AdminQtPassageVerseMapper#mapVerses}로 {@code qt_passage_verses}를 채운다(절별 구간 기반 prepare가 읽는 입력).</li>
 *   <li>{@code prepareClip=true}면 {@link PrepareQtVideoClipUseCase#prepareClip}로 클립을 준비한다.
 *       공개되지 않은 본문은 prepareClip이 게이트에서 {@code prepared=false}로 no-op 처리한다.</li>
 * </ol>
 *
 * <p>트랜잭션: {@code afterCommit}에서 어노테이션 {@code @Transactional}(REQUIRED) 쓰기는 완료 중 트랜잭션에
 * 묶여 커밋되지 않으므로, 클립 준비를 <b>프로그래매틱 REQUIRES_NEW</b>로 감싼다(매핑도 매퍼 내부에서 동일 처리).
 * 모든 단계는 best-effort — 실패해도 등록/게시 요청은 성공으로 유지하고 로그만 남긴다.
 */
@Slf4j
@Component
public class AdminQtVideoAutoPreparer {

    private final AdminQtPassageVerseMapper verseMapper;
    private final PrepareQtVideoClipUseCase prepareQtVideoClipUseCase;
    private final TransactionTemplate prepareTx;

    public AdminQtVideoAutoPreparer(AdminQtPassageVerseMapper verseMapper,
                                    PrepareQtVideoClipUseCase prepareQtVideoClipUseCase,
                                    PlatformTransactionManager transactionManager) {
        this.verseMapper = verseMapper;
        this.prepareQtVideoClipUseCase = prepareQtVideoClipUseCase;
        this.prepareTx = new TransactionTemplate(transactionManager);
        this.prepareTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 현재 본문 트랜잭션이 커밋된 뒤 매핑(+선택적 클립 준비)을 실행한다. 트랜잭션이 없으면(테스트 등) 즉시 실행.
     */
    public void syncAfterCommit(Long adminUserId, Long qtPassageId, Short bookId,
                                short startChapter, short endChapter, short startVerse, short endVerse,
                                boolean prepareClip) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runSync(adminUserId, qtPassageId, bookId, startChapter, endChapter, startVerse, endVerse, prepareClip);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runSync(adminUserId, qtPassageId, bookId, startChapter, endChapter, startVerse, endVerse, prepareClip);
            }
        });
    }

    private void runSync(Long adminUserId, Long qtPassageId, Short bookId,
                         short startChapter, short endChapter, short startVerse, short endVerse,
                         boolean prepareClip) {
        // 1) 절 매핑 채움(매퍼가 자체 REQUIRES_NEW 쓰기 + best-effort 처리)
        verseMapper.mapVerses(qtPassageId, bookId, startChapter, endChapter, startVerse, endVerse);
        if (!prepareClip) {
            return;
        }
        // 2) 매핑 커밋 이후 클립 준비 — REQUIRES_NEW로 커밋 보장, best-effort
        try {
            prepareTx.executeWithoutResult(status ->
                    prepareQtVideoClipUseCase.prepareClip(adminUserId, qtPassageId));
        } catch (RuntimeException exception) {
            log.warn("QT영상 클립 자동 준비 실패 — best-effort(등록/게시는 유지). qtPassageId={}, errorType={}, errorMessage={}",
                    qtPassageId, exception.getClass().getSimpleName(), exception.getMessage());
        }
    }
}
