package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.QtStudyContentResponse;

public interface GetQtStudyContentUseCase {

    QtStudyContentResponse getStudyContent(Long qtPassageId);
}
