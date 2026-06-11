package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.domain.ai.client.admin.AdminAuthClient;
import com.qtai.domain.ai.client.admin.AdminAuthClientHttpAdapter;
import com.qtai.domain.ai.client.admin.AdminAuthClientMock;
import com.qtai.domain.ai.client.audit.AuditLogClient;
import com.qtai.domain.ai.client.audit.AuditLogClientHttpAdapter;
import com.qtai.domain.ai.client.audit.AuditLogClientMock;
import com.qtai.domain.ai.client.bible.BibleVerseClient;
import com.qtai.domain.ai.client.bible.BibleVerseClientHttpAdapter;
import com.qtai.domain.ai.client.bible.BibleVerseClientMock;
import com.qtai.domain.ai.client.qt.GetQtUseCaseMock;
import com.qtai.domain.ai.client.qt.QtContextClient;
import com.qtai.domain.ai.client.qt.QtContextClientHttpAdapter;
import com.qtai.domain.ai.client.study.StudyPublishClient;
import com.qtai.domain.ai.client.study.StudyPublishClientHttpAdapter;
import com.qtai.domain.ai.client.study.StudyPublishClientMock;

@SpringBootTest(classes = AiServiceApplication.class)
@ActiveProfiles("test")
class AiServiceClientBoundaryTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void defaultModeRegistersMockClientsOnly() {
        assertThat(context.getBean(QtContextClient.class)).isInstanceOf(GetQtUseCaseMock.class);
        assertThat(context.getBean(BibleVerseClient.class)).isInstanceOf(BibleVerseClientMock.class);
        assertThat(context.getBean(StudyPublishClient.class)).isInstanceOf(StudyPublishClientMock.class);
        assertThat(context.getBean(AuditLogClient.class)).isInstanceOf(AuditLogClientMock.class);
        assertThat(context.getBean(AdminAuthClient.class)).isInstanceOf(AdminAuthClientMock.class);

        assertThat(context.getBeansOfType(QtContextClientHttpAdapter.class)).isEmpty();
        assertThat(context.getBeansOfType(BibleVerseClientHttpAdapter.class)).isEmpty();
        assertThat(context.getBeansOfType(StudyPublishClientHttpAdapter.class)).isEmpty();
        assertThat(context.getBeansOfType(AuditLogClientHttpAdapter.class)).isEmpty();
        assertThat(context.getBeansOfType(AdminAuthClientHttpAdapter.class)).isEmpty();
    }
}
