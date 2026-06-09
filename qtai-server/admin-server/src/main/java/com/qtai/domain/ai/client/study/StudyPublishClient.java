package com.qtai.domain.ai.client.study;

import java.time.OffsetDateTime;

import com.qtai.domain.ai.client.AiClientException;

public interface StudyPublishClient {

    void publishApprovedVerseExplanation(PublishVerseExplanationCommand command) throws AiClientException;

    void hidePublishedVerseExplanation(HideVerseExplanationCommand command) throws AiClientException;

    record PublishVerseExplanationCommand(
            Long bibleVerseId,
            String summary,
            String explanation,
            String sourceLabel,
            Long aiAssetId,
            OffsetDateTime approvedAt
    ) {
    }

    record HideVerseExplanationCommand(
            Long aiAssetId
    ) {
    }
}
