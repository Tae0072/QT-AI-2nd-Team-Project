package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsResult;

public interface PublishApprovedGlossaryTermsUseCase {

    PublishApprovedGlossaryTermsResult publishApprovedGlossaryTerms(
            PublishApprovedGlossaryTermsCommand command
    );
}
