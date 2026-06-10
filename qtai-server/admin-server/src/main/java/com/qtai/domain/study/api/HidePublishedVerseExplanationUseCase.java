package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationResult;

public interface HidePublishedVerseExplanationUseCase {

    HidePublishedVerseExplanationResult hidePublishedVerseExplanation(
            HidePublishedVerseExplanationCommand command
    );
}
