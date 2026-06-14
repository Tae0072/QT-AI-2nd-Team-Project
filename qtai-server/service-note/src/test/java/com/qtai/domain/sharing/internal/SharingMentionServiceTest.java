package com.qtai.domain.sharing.internal;

import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.notification.api.SendNotificationUseCase;
import com.qtai.domain.notification.api.dto.NotificationSendRequest;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 나눔 멘션 서비스 단위 테스트 — 기록·알림(본인 제외)·목록. */
@ExtendWith(MockitoExtension.class)
class SharingMentionServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock private SharingMentionRepository sharingMentionRepository;
    @Mock private SharingPostRepository sharingPostRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private SharingBookmarkRepository sharingBookmarkRepository;
    @Mock private GetMemberUseCase getMemberUseCase;
    @Mock private SendNotificationUseCase sendNotificationUseCase;

    private SharingMentionService service() {
        return new SharingMentionService(sharingMentionRepository, sharingPostRepository, postLikeRepository,
                sharingBookmarkRepository, getMemberUseCase, sendNotificationUseCase, CLOCK);
    }

    @Test
    void 멘션_기록_본인은_제외하고_저장과_알림() {
        // actor=1. 본문에 #지혜(2) #작성자(1) → 본인(1)은 제외, 지혜(2)만 기록·알림.
        when(getMemberUseCase.resolveActiveByNicknames(anyCollection())).thenReturn(List.of(
                new MemberPublicResponse(2L, "지혜", null),
                new MemberPublicResponse(1L, "작성자", null)));

        service().recordMentions(10L, 5L, 1L, "#지혜 고마워 #작성자");

        verify(sharingMentionRepository, times(1)).save(any(SharingMention.class));
        ArgumentCaptor<NotificationSendRequest> captor = ArgumentCaptor.forClass(NotificationSendRequest.class);
        verify(sendNotificationUseCase, times(1)).send(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(2L);
        assertThat(captor.getValue().type()).isEqualTo("MENTION");
    }

    @Test
    void 멘션이_없으면_해석도_저장도_안함() {
        service().recordMentions(10L, null, 1L, "그냥 본문");

        verify(getMemberUseCase, never()).resolveActiveByNicknames(anyCollection());
        verify(sharingMentionRepository, never()).save(any());
    }

    @Test
    void 해석실패는_비차단_저장없이_끝낸다() {
        when(getMemberUseCase.resolveActiveByNicknames(anyCollection()))
                .thenThrow(new RuntimeException("user 서비스 다운"));

        service().recordMentions(10L, null, 1L, "#지혜"); // 예외 전파 안 함

        verify(sharingMentionRepository, never()).save(any());
    }

    @Test
    void 태그된글_목록_빈페이지면_빈콘텐츠() {
        when(sharingMentionRepository.findMentionedPosts(any(), any(), any())).thenReturn(Page.empty());

        SharingPostListResponse response = service().listMentions(1L, PageRequest.of(0, 20));

        assertThat(response.content()).isEmpty();
        assertThat(response.sort()).isEqualTo("publishedAt,desc");
    }
}
