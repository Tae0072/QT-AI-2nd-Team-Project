package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;

@ExtendWith(MockitoExtension.class)
class AdminAiAssetQueryRepositoryFastPathTest {

    @Mock
    EntityManager entityManager;

    @Mock
    TypedQuery<Object[]> listQuery;

    @Mock
    TypedQuery<Long> countQuery;

    @Test
    void findAllWithoutChecklistVersionDoesNotJoinLatestValidationInPageQuery() {
        AdminAiAssetQueryRepository repository = new AdminAiAssetQueryRepository(entityManager);
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(listQuery);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(listQuery.setParameter(anyString(), nullable(Object.class))).thenReturn(listQuery);
        when(listQuery.setFirstResult(0)).thenReturn(listQuery);
        when(listQuery.setMaxResults(20)).thenReturn(listQuery);
        when(listQuery.getResultList()).thenReturn(List.of());
        when(countQuery.setParameter(anyString(), nullable(Object.class))).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        repository.findAll(query(null), PageRequest.of(0, 20));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(1)).createQuery(queryCaptor.capture(), eq(Object[].class));
        assertThat(queryCaptor.getValue()).doesNotContain("AiValidationLog");
    }

    private static ListAdminAiAssetsQuery query(Long checklistVersionId) {
        return new ListAdminAiAssetsQuery(
                1L,
                "ADMIN",
                "REVIEWER",
                null,
                null,
                "VALIDATING",
                null,
                checklistVersionId,
                0,
                20
        );
    }
}
