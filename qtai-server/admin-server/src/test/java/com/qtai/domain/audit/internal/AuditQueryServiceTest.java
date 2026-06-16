package com.qtai.domain.audit.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.dto.AuditLogListResponse;
import com.qtai.domain.audit.api.dto.ListAuditQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** AD-07: 감사 조회가 비-AI 운영 관리자 액션을 노출하되, 민감 영역은 deny-by-default로 계속 차단하는지 검증. */
class AuditQueryServiceTest {

    private AuditQueryRepository repository;
    private AuditQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditQueryRepository.class);
        service = new AuditQueryService(repository);
        when(repository.findAll(any(), any()))
                .thenReturn(new AuditQueryRepository.AuditLogPage(List.of(), 0L));
    }

    private static ListAuditQuery query(String actionType, String targetType) {
        return new ListAuditQuery(1L, "ADMIN", "OPERATOR", null, null, actionType, targetType, null, null, null, 0, 20);
    }

    @Test
    void defaultListIncludesNonAiAdminActions() {
        service.listAuditLogs(query(null, null));

        ArgumentCaptor<AuditQueryRepository.Filter> captor =
                ArgumentCaptor.forClass(AuditQueryRepository.Filter.class);
        verify(repository).findAll(captor.capture(), any());
        assertThat(captor.getValue().actionTypes())
                .contains("REPORT_RESOLVE", "REPORT_REJECT", "NOTICE_PUBLISH",
                        "MUSIC_TRACK_CREATE", "QT_PASSAGE_PUBLISH", "AI_ASSET_APPROVE",
                        "QT_VIDEO_SOURCE_DELETE", "QT_VIDEO_CLIP_DELETE");
    }

    @Test
    void nonAiActionTypeFilterIsAccepted() {
        AuditLogListResponse response = service.listAuditLogs(query("REPORT_RESOLVE", null));

        ArgumentCaptor<AuditQueryRepository.Filter> captor =
                ArgumentCaptor.forClass(AuditQueryRepository.Filter.class);
        verify(repository).findAll(captor.capture(), any());
        assertThat(captor.getValue().actionTypes()).containsExactly("REPORT_RESOLVE");
        assertThat(response).isNotNull();
    }

    @Test
    void nonAiTargetTypeFilterIsAccepted() {
        service.listAuditLogs(query(null, "REPORT"));

        ArgumentCaptor<AuditQueryRepository.Filter> captor =
                ArgumentCaptor.forClass(AuditQueryRepository.Filter.class);
        verify(repository).findAll(captor.capture(), any());
        assertThat(captor.getValue().targetType()).isEqualTo("REPORT");
    }

    @Test
    void sensitiveActionTypeIsRejected() {
        assertThatThrownBy(() -> service.listAuditLogs(query("VALIDATION_REFERENCE_JOB_CREATE", null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void sensitiveTargetTypeIsRejected() {
        assertThatThrownBy(() -> service.listAuditLogs(query(null, "VALIDATION_REFERENCE_JOB")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
