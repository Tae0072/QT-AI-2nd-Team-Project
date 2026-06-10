package com.qtai.domain.music.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MusicTrackServiceTest {

    @Mock
    private MusicTrackRepository musicTrackRepository;

    @InjectMocks
    private MusicTrackService musicTrackService;

    @Test
    void listEnabled_없으면_빈_목록() {
        when(musicTrackRepository.findByEnabledTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc())
                .thenReturn(List.of());

        assertTrue(musicTrackService.listEnabled().isEmpty());
    }

    @Test
    void getAudio_없는_음원이면_RESOURCE_NOT_FOUND() {
        when(musicTrackRepository.findEnabledAudioById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> musicTrackService.getAudio(99L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }
}
