package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationResult;

public interface PublishApprovedVerseExplanationUseCase {

    PublishApprovedVerseExplanationResult publishApprovedVerseExplanation(
            PublishApprovedVerseExplanationCommand command
    );
}
