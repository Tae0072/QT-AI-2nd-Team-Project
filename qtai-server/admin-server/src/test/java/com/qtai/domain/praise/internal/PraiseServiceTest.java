package com.qtai.domain.praise.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.praise.api.dto.PraiseResponse;
import com.qtai.domain.praise.api.dto.PraiseUpdateRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PraiseServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-11T01:30:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private PraiseSongRepository praiseSongRepository;

    @Mock
    private MemberPraiseSongRepository memberPraiseSongRepository;

    private PraiseService service;

    @BeforeEach
    void setUp() {
        service = new PraiseService(praiseSongRepository, memberPraiseSongRepository, CLOCK);
    }

    // ── update ──

    @Test
    @DisplayName("update는 곡 메타데이터를 갱신하고 응답을 반환한다")
    void update_updatesAndReturnsResponse() {
        PraiseSong song = song(50L, "은혜", "큐레이션");
        when(praiseSongRepository.findById(50L)).thenReturn(Optional.of(song));

        PraiseResponse response = service.update(3L, 50L,
                new PraiseUpdateRequest("새 제목", "새 아티스트", "저작권 확인함"));

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.title()).isEqualTo("새 제목");
        assertThat(response.artist()).isEqualTo("새 아티스트");
        assertThat(response.licenseNote()).isEqualTo("저작권 확인함");
    }

    @Test
    @DisplayName("update는 곡이 없으면 PRAISE_SONG_NOT_FOUND")
    void update_rejectsMissingSong() {
        when(praiseSongRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(3L, 404L,
                new PraiseUpdateRequest("제목", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRAISE_SONG_NOT_FOUND);
    }

    // ── delete (cascade) ──

    @Test
    @DisplayName("delete는 회원 참조 행을 먼저 정리한 뒤 곡을 삭제한다 (FK RESTRICT 회피)")
    void delete_removesMemberReferencesBeforeDeletingSong() {
        PraiseSong song = song(50L, "은혜", "큐레이션");
        when(praiseSongRepository.findById(50L)).thenReturn(Optional.of(song));
        when(memberPraiseSongRepository.deleteByPraiseSongId(50L)).thenReturn(2L);

        service.delete(3L, 50L);

        // 순서 보장: 참조 행 삭제 → 곡 삭제
        InOrder inOrder = Mockito.inOrder(memberPraiseSongRepository, praiseSongRepository);
        inOrder.verify(memberPraiseSongRepository).deleteByPraiseSongId(50L);
        inOrder.verify(praiseSongRepository).delete(song);
    }

    @Test
    @DisplayName("delete는 곡이 없으면 PRAISE_SONG_NOT_FOUND, 참조 삭제도 시도하지 않는다")
    void delete_rejectsMissingSong() {
        when(praiseSongRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(3L, 404L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRAISE_SONG_NOT_FOUND);

        verify(memberPraiseSongRepository, never()).deleteByPraiseSongId(any());
        verify(praiseSongRepository, never()).delete(any());
    }

    // ── listAdmin ──

    @Test
    @DisplayName("listAdmin은 status가 있으면 상태별로 필터한다")
    void listAdmin_withStatus_filtersByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        when(praiseSongRepository.findByStatus(PraiseSongStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(song(50L, "은혜", "큐레이션")), pageable, 1));

        Page<PraiseResponse> result = service.listAdmin("ACTIVE", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(50L);
        verify(praiseSongRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("listAdmin은 status가 null이면 전체 조회한다")
    void listAdmin_withoutStatus_findsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(praiseSongRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(song(50L, "은혜", "큐레이션")), pageable, 1));

        Page<PraiseResponse> result = service.listAdmin(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(praiseSongRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("listAdmin은 정의되지 않은 status를 INVALID_INPUT(400)으로 거부한다")
    void listAdmin_rejectsInvalidStatus() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.listAdmin("UNKNOWN", pageable))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(praiseSongRepository, never()).findByStatus(any(), any());
    }

    // ── fixtures ──

    private static PraiseSong song(Long id, String title, String artist) {
        PraiseSong song = PraiseSong.builder()
                .title(title)
                .artist(artist)
                .sourceType(PraiseSourceType.CURATED)
                .licenseNote("운영자가 저작권 상태를 확인함")
                .status(PraiseSongStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(song, "id", id);
        return song;
    }
}
