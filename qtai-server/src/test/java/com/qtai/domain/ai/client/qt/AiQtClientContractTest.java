package com.qtai.domain.ai.client.qt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.qtai.domain.ai.client.qt.dto.QtContextResult;

class AiQtClientContractTest {

    @Test
    void mockAdapterProvidesQtContextThroughAiClientBoundary() {
        QtContextClient client = new GetQtUseCaseMock();

        QtContextResult result = client.getQtContext(10L, 35L);

        assertThat(result.qtPassageId()).isEqualTo(35L);
        assertThat(result.passageReference()).isNotBlank();
        assertThat(result.promptContextSummary()).isNotBlank();
    }

    @Test
    void qtContextResultDoesNotExposeScriptureBodyText() {
        assertThat(QtContextResult.class.getRecordComponents())
                .extracting(component -> component.getName().toLowerCase())
                .doesNotContain("content", "body", "text", "scripturetext", "passagetext");
    }
}
