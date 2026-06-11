package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;

import java.util.List;

public interface ListApprovedVerseExplanationUseCase {

    List<ApprovedVerseExplanationResponse> listApprovedByVerseIds(List<Long> verseIds);
}
