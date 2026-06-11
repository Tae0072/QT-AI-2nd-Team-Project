package com.qtai.domain.sharing.internal;

import java.time.Clock;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.domain.sharing.api.HideSharingPostForModerationUseCase;

/**
 * 신고 처리(모더레이션) 강제 숨김 (명세 §4.7.4).
 *
 * <p>report 도메인에 의존하지 않는 독립 빈 — 순환 의존을 만들지 않는다.
 * 소유자 검증 없이 동작하므로 호출자는 관리자 권한 검증을 마친 상태여야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class SharingModerationService implements HideSharingPostForModerationUseCase {

    private final SharingPostRepository sharingPostRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void hideForModeration(Long postId) {
        SharingPost post = sharingPostRepository.findById(postId).orElse(null);
        if (post == null) {
            log.warn("모더레이션 숨김 대상 없음(멱등 통과). postId={}", postId);
            return;
        }
        if (post.getStatus() != SharingPostStatus.PUBLISHED) {
            // 이미 숨김·삭제된 글 — 추가 조치 없음(멱등)
            return;
        }
        post.hide(LocalDateTime.now(clock));
        log.info("모더레이션 강제 숨김 처리. postId={}", postId);
    }
}
