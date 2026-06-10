package com.qtai.domain.praise.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.praise.api.dto.MemberPraiseSongCreateRequest;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PraiseServiceTest {

    @Mock
    private PraiseSongRepository praiseSongRepository;

    @Mock
    private MemberPraiseSongRepository memberPraiseSongRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private PraiseService praiseService;

    @Test
    void save_대상이_모두_null이면_INVALID_INPUT() {
        var request = new MemberPraiseSongCreateRequest(null, null, "내 찬양");

        BusinessException ex = assertThrows(BusinessException.class, () -> praiseService.save(1L, request));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void save_큐레이션곡_중복이면_ALREADY_SAVED() {
        var request = new MemberPraiseSongCreateRequest(10L, null, "내 찬양");
        when(memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(1L, 10L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> praiseService.save(1L, request));
        assertEquals(ErrorCode.PRAISE_SONG_ALREADY_SAVED, ex.getErrorCode());
    }

    @Test
    void save_큐레이션곡_없으면_NOT_FOUND() {
        var request = new MemberPraiseSongCreateRequest(10L, null, "내 찬양");
        when(memberPraiseSongRepository.existsByMemberIdAndPraiseSongId(1L, 10L)).thenReturn(false);
        when(praiseSongRepository.findById(10L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> praiseService.save(1L, request));
        assertEquals(ErrorCode.PRAISE_SONG_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void save_디바이스곡_중복이면_ALREADY_SAVED() {
        var request = new MemberPraiseSongCreateRequest(null, "device-key-1", "내 찬양");
        when(memberPraiseSongRepository.existsByMemberIdAndDeviceSongKey(1L, "device-key-1")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> praiseService.save(1L, request));
        assertEquals(ErrorCode.PRAISE_SONG_ALREADY_SAVED, ex.getErrorCode());
    }

    @Test
    void remove_없는_저장곡이면_SAVE_NOT_FOUND() {
        when(memberPraiseSongRepository.findByIdAndMemberId(5L, 1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> praiseService.remove(1L, 5L));
        assertEquals(ErrorCode.PRAISE_SONG_SAVE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void countMy_리포지토리_카운트를_그대로_반환() {
        when(memberPraiseSongRepository.countByMemberId(1L)).thenReturn(3L);

        assertEquals(3L, praiseService.countMy(1L));
    }
}
