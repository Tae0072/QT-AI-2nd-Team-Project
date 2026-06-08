package com.qtai.domain.ai.client.qt;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;
import com.qtai.domain.ai.client.http.AiClientProperties;
import com.qtai.domain.ai.client.http.AiHttpSupport;
import com.qtai.domain.ai.client.qt.dto.QtContextResult;

@Component("aiQtContextClientHttpAdapter")
@ConditionalOnProperty(name = "qtai.ai.client.mode", havingValue = "http")
@ConditionalOnMissingBean(QtContextClient.class)
public class QtContextClientHttpAdapter implements QtContextClient {

    private static final String DOWNSTREAM = "qt";
    private static final String QT_CONTEXT_PATH = "/api/v1/system/qt/passages/%d/context";
    private static final String TODAY_STATUS_PATH = "/api/v1/system/qt/passages/today/status";

    private final AiHttpSupport http;
    private final JavaType qtContextType;
    private final JavaType todayStatusType;

    public QtContextClientHttpAdapter(ObjectMapper objectMapper, AiClientProperties properties) {
        this.http = new AiHttpSupport(objectMapper, properties, properties.getQt(), DOWNSTREAM);
        this.qtContextType = objectMapper.getTypeFactory().constructType(QtContextResult.class);
        this.todayStatusType = objectMapper.getTypeFactory().constructType(TodayQtPassageStatus.class);
    }

    @Override
    public QtContextResult getQtContext(Long viewerId, Long qtPassageId) {
        if (qtPassageId == null) {
            throw validationFailure("qtPassageId must not be null");
        }
        return http.get(QT_CONTEXT_PATH.formatted(qtPassageId), Map.of(), qtContextType);
    }

    @Override
    public TodayQtPassageStatus getTodayQtPassageStatus(LocalDate qtDate) {
        Map<String, Object> queryParameters = new LinkedHashMap<>();
        if (qtDate != null) {
            queryParameters.put("date", qtDate);
        }
        return http.get(TODAY_STATUS_PATH, queryParameters, todayStatusType);
    }

    private static AiClientException validationFailure(String message) {
        return new AiClientException(FailureCode.VALIDATION_FAILED, DOWNSTREAM, message);
    }
}
