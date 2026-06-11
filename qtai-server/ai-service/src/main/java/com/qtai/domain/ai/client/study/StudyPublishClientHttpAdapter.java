package com.qtai.domain.ai.client.study;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;
import com.qtai.domain.ai.client.http.AiClientProperties;
import com.qtai.domain.ai.client.http.AiHttpSupport;

@Component("aiStudyPublishClientHttpAdapter")
@ConditionalOnProperty(name = "qtai.ai.client.mode", havingValue = "http")
@ConditionalOnMissingBean(StudyPublishClient.class)
public class StudyPublishClientHttpAdapter implements StudyPublishClient {

    private static final String DOWNSTREAM = "study";
    private static final String PUBLISH_PATH = "/api/v1/system/study/verse-explanations:publish";
    private static final String HIDE_PATH = "/api/v1/system/study/verse-explanations:hide";

    private final AiHttpSupport http;

    public StudyPublishClientHttpAdapter(ObjectMapper objectMapper, AiClientProperties properties) {
        this.http = new AiHttpSupport(objectMapper, properties, properties.getStudy(), DOWNSTREAM);
    }

    @Override
    public void publishApprovedVerseExplanation(PublishVerseExplanationCommand command) {
        if (command == null) {
            throw validationFailure("publish command must not be null");
        }
        if (command.aiAssetId() == null) {
            throw validationFailure("publish command aiAssetId must not be null");
        }
        http.postVoid(PUBLISH_PATH, command, http.idempotencyKey("study.publish", command.aiAssetId()));
    }

    @Override
    public void hidePublishedVerseExplanation(HideVerseExplanationCommand command) {
        if (command == null) {
            throw validationFailure("hide command must not be null");
        }
        if (command.aiAssetId() == null) {
            throw validationFailure("hide command aiAssetId must not be null");
        }
        http.postVoid(HIDE_PATH, command, http.idempotencyKey("study.hide", command.aiAssetId()));
    }

    private static AiClientException validationFailure(String message) {
        return new AiClientException(FailureCode.VALIDATION_FAILED, DOWNSTREAM, message);
    }
}
