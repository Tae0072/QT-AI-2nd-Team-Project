package com.qtai.ai;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.qtai.domain.ai.client.admin.AdminAuthClientHttpAdapter;
import com.qtai.domain.ai.client.admin.AdminAuthClientMock;
import com.qtai.domain.ai.client.audit.AuditLogClientHttpAdapter;
import com.qtai.domain.ai.client.audit.AuditLogClientMock;
import com.qtai.domain.ai.client.bible.BibleVerseClientHttpAdapter;
import com.qtai.domain.ai.client.bible.BibleVerseClientMock;
import com.qtai.domain.ai.client.http.AiClientConfiguration;
import com.qtai.domain.ai.client.qt.GetQtUseCaseMock;
import com.qtai.domain.ai.client.qt.QtContextClientHttpAdapter;
import com.qtai.domain.ai.client.study.StudyPublishClientHttpAdapter;
import com.qtai.domain.ai.client.study.StudyPublishClientMock;

@Configuration
@Import({
        AiClientConfiguration.class,
        GetQtUseCaseMock.class,
        BibleVerseClientMock.class,
        StudyPublishClientMock.class,
        AuditLogClientMock.class,
        AdminAuthClientMock.class,
        QtContextClientHttpAdapter.class,
        BibleVerseClientHttpAdapter.class,
        StudyPublishClientHttpAdapter.class,
        AuditLogClientHttpAdapter.class,
        AdminAuthClientHttpAdapter.class
})
public class AiServiceClientConfiguration {
}
