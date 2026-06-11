package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsResult;

public interface HidePublishedGlossaryTermsUseCase {

    HidePublishedGlossaryTermsResult hidePublishedGlossaryTerms(
            HidePublishedGlossaryTermsCommand command
    );
}
