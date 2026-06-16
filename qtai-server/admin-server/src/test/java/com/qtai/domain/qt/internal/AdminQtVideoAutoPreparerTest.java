package com.qtai.domain.qt.internal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.qtai.domain.qtvideo.api.PrepareQtVideoClipUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class AdminQtVideoAutoPreparerTest {

    @Mock
    private AdminQtPassageVerseMapper verseMapper;

    @Mock
    private PrepareQtVideoClipUseCase prepareQtVideoClipUseCase;

    @Mock
    private PlatformTransactionManager transactionManager;

    private AdminQtVideoAutoPreparer preparer() {
        // 활성 트랜잭션이 없으면 syncAfterCommit은 즉시 실행한다(단위 테스트).
        // REQUIRES_NEW TransactionTemplate은 manager.getTransaction()=null이어도 콜백을 실행한다.
        return new AdminQtVideoAutoPreparer(verseMapper, prepareQtVideoClipUseCase, transactionManager);
    }

    @Test
    @DisplayName("prepareClip=true — 절 매핑 + 클립 준비 둘 다 호출")
    void sync_prepareTrue_mapsAndPrepares() {
        preparer().syncAfterCommit(3L, 9L, (short) 46, (short) 9, (short) 9, (short) 1, (short) 7, true);

        verify(verseMapper).mapVerses(9L, (short) 46, (short) 9, (short) 9, (short) 1, (short) 7);
        verify(prepareQtVideoClipUseCase).prepareClip(3L, 9L);
    }

    @Test
    @DisplayName("prepareClip=false — 절 매핑만, 클립 준비는 안 함(미공개 등록)")
    void sync_prepareFalse_mapsOnly() {
        preparer().syncAfterCommit(3L, 9L, (short) 46, (short) 9, (short) 9, (short) 1, (short) 7, false);

        verify(verseMapper).mapVerses(9L, (short) 46, (short) 9, (short) 9, (short) 1, (short) 7);
        verify(prepareQtVideoClipUseCase, never()).prepareClip(anyLong(), anyLong());
    }
}
