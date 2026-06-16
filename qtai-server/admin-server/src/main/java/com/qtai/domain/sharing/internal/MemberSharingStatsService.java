package com.qtai.domain.sharing.internal;

import com.qtai.domain.sharing.api.MemberSharingStatsUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 상세용 나눔 통계 서비스.
 *
 * <p>admin 회원 관리가 회원 단위 나눔 지표(작성 공유글 수/받은 신고 집계용 ID 목록)를 조회할 때 사용한다.
 * 나눔 운영(AD-13) 전체 서비스와 분리된 가벼운 통계 전용 구현이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSharingStatsService implements MemberSharingStatsUseCase {

    private final SharingPostRepository sharingPostRepository;
    private final CommentRepository commentRepository;

    @Override
    public long countPostsByMember(Long memberId) {
        if (memberId == null) {
            return 0L;
        }
        return sharingPostRepository.countByMemberId(memberId);
    }

    @Override
    public List<Long> listPostIdsByMember(Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return sharingPostRepository.findIdsByMemberId(memberId);
    }

    @Override
    public List<Long> listCommentIdsByMember(Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return commentRepository.findIdsByMemberId(memberId);
    }
}
