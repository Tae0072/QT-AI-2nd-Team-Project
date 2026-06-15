package com.qtai.domain.sharing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.dto.AdminSharingPostResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 관리자 나눔 운영 서비스 단위 테스트 (AD-15).
 *
 * <p>모더레이션은 숨김/복원만 — 멱등 처리와 삭제본 차단, 미존재 404를 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class AdminSharingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-16T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private SharingPostRepository sharingPostRepository;

    private AdminSharingService service() {
        return new AdminSharingService(sharingPostRepository, CLOCK);
    }

    private SharingPost postWithStatus(SharingPostStatus status) {
        SharingPost post = org.mockito.Mockito.mock(SharingPost.class);
        when(post.getStatus()).thenReturn(status);
        return post;
    }

    @Test
    void hide_publishedPost_callsHide() {
        SharingPost post = postWithStatus(SharingPostStatus.PUBLISHED);
        when(sharingPostRepository.findById(1L)).thenReturn(Optional.of(post));

        AdminSharingPostResponse res = service().hide(1L);

        verify(post).hide(any());
        assertThat(res).isNotNull();
    }

    @Test
    void hide_alreadyHidden_isIdempotent() {
        SharingPost post = postWithStatus(SharingPostStatus.HIDDEN);
        when(sharingPostRepository.findById(1L)).thenReturn(Optional.of(post));

        service().hide(1L);

        verify(post, never()).hide(any());
    }

    @Test
    void hide_deletedPost_isRejected() {
        SharingPost post = postWithStatus(SharingPostStatus.DELETED);
        when(sharingPostRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service().hide(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        verify(post, never()).hide(any());
    }

    @Test
    void restore_hiddenPost_callsShow() {
        SharingPost post = postWithStatus(SharingPostStatus.HIDDEN);
        when(sharingPostRepository.findById(1L)).thenReturn(Optional.of(post));

        service().restore(1L);

        verify(post).show();
    }

    @Test
    void restore_alreadyPublished_isIdempotent() {
        SharingPost post = postWithStatus(SharingPostStatus.PUBLISHED);
        when(sharingPostRepository.findById(1L)).thenReturn(Optional.of(post));

        service().restore(1L);

        verify(post, never()).show();
    }

    @Test
    void restore_deletedPost_isRejected() {
        SharingPost post = postWithStatus(SharingPostStatus.DELETED);
        when(sharingPostRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service().restore(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        verify(post, never()).show();
    }

    @Test
    void getForAdmin_missingPost_throwsNotFound() {
        when(sharingPostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getForAdmin(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
