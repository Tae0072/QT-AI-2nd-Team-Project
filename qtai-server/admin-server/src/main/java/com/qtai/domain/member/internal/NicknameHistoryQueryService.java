package com.qtai.domain.member.internal;

import com.qtai.domain.member.api.ListNicknameHistoryForAdminUseCase;
import com.qtai.domain.member.api.dto.NicknameHistoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 상세용 닉네임 변경 이력 조회 서비스.
 *
 * <p>append-only 기록을 최신순으로 돌려준다. 기록 주체는 service-user다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NicknameHistoryQueryService implements ListNicknameHistoryForAdminUseCase {

    private final NicknameChangeHistoryRepository nicknameChangeHistoryRepository;

    @Override
    public Page<NicknameHistoryItem> listNicknameHistory(Long memberId, Pageable pageable) {
        return nicknameChangeHistoryRepository.findByMemberIdOrderByChangedAtDesc(memberId, pageable)
                .map(h -> new NicknameHistoryItem(h.getOldNickname(), h.getNewNickname(), h.getChangedAt()));
    }
}
