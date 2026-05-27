package com.qtai.domain.note.client.qt;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class GetQtUseCaseMock implements NoteQtClient {

    @Override
    public void validateReadable(Long memberId, Long qtPassageId) {
        if (memberId == null || qtPassageId == null || qtPassageId < 1) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }
    }
}
