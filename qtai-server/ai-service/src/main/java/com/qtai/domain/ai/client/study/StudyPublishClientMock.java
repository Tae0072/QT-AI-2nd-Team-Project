package com.qtai.domain.ai.client.study;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("aiStudyPublishClientMock")
@Profile({"local", "test"})
@ConditionalOnProperty(name = "qtai.ai.client.mode", havingValue = "mock", matchIfMissing = true)
@ConditionalOnMissingBean(StudyPublishClient.class)
public class StudyPublishClientMock implements StudyPublishClient {

    private final List<PublishVerseExplanationCommand> publishedCommands = new ArrayList<>();
    private final List<HideVerseExplanationCommand> hiddenCommands = new ArrayList<>();

    @Override
    public void publishApprovedVerseExplanation(PublishVerseExplanationCommand command) {
        publishedCommands.add(command);
    }

    @Override
    public void hidePublishedVerseExplanation(HideVerseExplanationCommand command) {
        hiddenCommands.add(command);
    }

    public List<PublishVerseExplanationCommand> publishedCommands() {
        return List.copyOf(publishedCommands);
    }

    public List<HideVerseExplanationCommand> hiddenCommands() {
        return List.copyOf(hiddenCommands);
    }
}
