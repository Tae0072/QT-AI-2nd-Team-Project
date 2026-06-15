package com.qtai.domain.sharing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.dto.AdminSharingPostResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 관리자 나눔 운영 서비스 단위 테스트 (AD-15).
 *
 * <p>목록(상태/검색어 파싱·이스케이프), 상세, 숨김/복원 멱등·삭제본 차단·404를 확인한다.
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

    // ── 목록/검색 ──

    @Test
    void listForAdmin_parsesStatusAndEscapesKeyword() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SharingPost> page = new PageImpl<>(List.of(postWithStatus(SharingPostStatus.PUBLISHED)));
        ArgumentCaptor<SharingPostStatus> statusCaptor = ArgumentCaptor.forClass(SharingPostStatus.class);
        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
        when(sharingPostRepository.searchForAdmin(any(), any(), eq(pageable))).thenReturn(page);

        Page<AdminSharingPostResponse> result = service().listForAdmin("hidden", "10%_a", pageable);

        verify(sharingPostRepository).searchForAdmin(statusCaptor.capture(), qCaptor.capture(), eq(pageable));
        assertThat(statusCaptor.getValue()).isEqualTo(SharingPostStatus.HIDDEN); // 대소문자 무관 파싱
        assertThat(qCaptor.getValue()).isEqualTo("10\\%\\_a"); // LIKE 와일드카드 이스케이프
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listForAdmin_blankStatusAndKeyword_passNull() {
        Pageable pageable = PageRequest.of(0, 20);
        when(sharingPostRepository.searchForAdmin(isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        service().listForAdmin("  ", "  ", pageable);

        verify(sharingPostRepository).searchForAdmin(isNull(), isNull(), eq(pageable));
    }

    @Test
    void listForAdmin_invalidStatus_isRejected() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service().listForAdmin("NOPE", null, pageable))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(sharingPostRepository);
    }

    // ── 상세 ──

    @Test
    void getForAdmin_returnsFullBody() {
        SharingPost post = postWithStatus(SharingPostStatus.PUBLISHED);
        when(post.getSnapshotBody()).thenReturn("전체 본문입니다");
        when(sharingPostRepository.findById(1L)).thenReturn(Optional.of(post));

        AdminSharingPostResponse res = service().getForAdmin(1L);

        assertThat(res.body()).isEqualTo("전체 본문입니다");
    }

    @Test
    void getForAdmin_missingPost_throwsNotFound() {
        when(sharingPostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getForAdmin(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ── 숨김 ──

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

    // ── 복원 ──

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
}
