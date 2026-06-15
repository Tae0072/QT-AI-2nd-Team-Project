package com.qtai.domain.music.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.music.api.dto.AdminMusicTrackCommand;
import com.qtai.domain.music.api.dto.AdminMusicTrackListResponse;
import com.qtai.domain.music.api.dto.AdminMusicTrackResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminMusicTrackServiceTest {

    @Mock
    private MusicTrackRepository musicTrackRepository;

    @Mock
    private WriteAuditLogUseCase auditLogUseCase;

    private MusicTrackService service;

    @BeforeEach
    void setUp() {
        service = new MusicTrackService(
                musicTrackRepository,
                auditLogUseCase,
                new MusicTrackAuditSnapshotFactory(new ObjectMapper().findAndRegisterModules())
        );
    }

    @Test
    @DisplayName("listAdmin은 문서화된 페이징 응답을 반환한다")
    void listAdmin_returnsDocumentedPageResponse() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(musicTrackRepository.findAdminSummaries(eq(Boolean.TRUE), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(summary(10L, true)), pageable, 1));

        AdminMusicTrackListResponse response = service.listAdmin("ACTIVE", pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo("ACTIVE");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
        assertThat(response.sort()).isEqualTo("createdAt,desc");
    }

    @Test
    @DisplayName("createAdmin은 업로드된 음원을 HIDDEN 상태로 등록하고 감사 로그를 남긴다")
    void createAdmin_savesHiddenTrackAndWritesAudit() {
        when(musicTrackRepository.save(any(MusicTrack.class)))
                .thenAnswer(invocation -> {
                    MusicTrack track = invocation.getArgument(0);
                    ReflectionTestUtils.setField(track, "id", 10L);
                    return track;
                });

        AdminMusicTrackResponse response = service.createAdmin(3L, command("새 배경음악", audio("mp3")));

        ArgumentCaptor<MusicTrack> captor = ArgumentCaptor.forClass(MusicTrack.class);
        verify(musicTrackRepository).save(captor.capture());
        MusicTrack saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("새 배경음악");
        assertThat(saved.getCategory()).isEqualTo(MusicCategory.BGM);
        assertThat(saved.getEnabled()).isFalse();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("HIDDEN");
        verify(auditLogUseCase).write(any());
    }

    @Test
    @DisplayName("updateAdmin은 파일이 없으면 기존 음원을 유지한다")
    void updateAdmin_withoutAudio_keepsCurrentAudio() {
        MusicTrack track = track(10L, true, audio("old"));
        when(musicTrackRepository.findById(10L)).thenReturn(Optional.of(track));

        AdminMusicTrackResponse response = service.updateAdmin(3L, 10L,
                command("수정 배경음악", null));

        assertThat(track.getTitle()).isEqualTo("수정 배경음악");
        assertThat(track.getAudioData()).isEqualTo(audio("old"));
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(auditLogUseCase).write(any());
    }

    @Test
    @DisplayName("updateAdmin은 누락된 PATCH 필드를 기존 값으로 보존한다")
    void updateAdmin_partialCommand_preservesOmittedFields() {
        MusicTrack track = track(
                10L,
                true,
                MusicCategory.HYMN,
                "audio/wav",
                120,
                7,
                "기존 라이선스",
                audio("old")
        );
        when(musicTrackRepository.findById(10L)).thenReturn(Optional.of(track));

        service.updateAdmin(3L, 10L, new AdminMusicTrackCommand(
                "제목만 수정",
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(track.getTitle()).isEqualTo("제목만 수정");
        assertThat(track.getCategory()).isEqualTo(MusicCategory.HYMN);
        assertThat(track.getMimeType()).isEqualTo("audio/wav");
        assertThat(track.getDurationSec()).isEqualTo(120);
        assertThat(track.getSortOrder()).isEqualTo(7);
        assertThat(track.getLicenseNote()).isEqualTo("기존 라이선스");
        assertThat(track.getAudioData()).isEqualTo(audio("old"));
    }

    @Test
    @DisplayName("updateAdmin은 파일이 있으면 음원을 교체한다")
    void updateAdmin_withAudio_replacesAudio() {
        MusicTrack track = track(10L, true, audio("old"));
        when(musicTrackRepository.findById(10L)).thenReturn(Optional.of(track));

        service.updateAdmin(3L, 10L, command("수정 배경음악", audio("new")));

        assertThat(track.getAudioData()).isEqualTo(audio("new"));
        assertThat(track.getByteSize()).isEqualTo(3L);
    }

    @Test
    @DisplayName("publishAdmin은 HIDDEN 음원을 ACTIVE로 전환한다")
    void publishAdmin_hiddenTrack_enablesTrack() {
        MusicTrack track = track(10L, false, audio("mp3"));
        when(musicTrackRepository.findById(10L)).thenReturn(Optional.of(track));

        AdminMusicTrackResponse response = service.publishAdmin(3L, 10L);

        assertThat(track.getEnabled()).isTrue();
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(auditLogUseCase).write(any());
    }

    @Test
    @DisplayName("publishAdmin은 이미 ACTIVE이면 INVALID_STATUS_TRANSITION")
    void publishAdmin_activeTrack_rejectsTransition() {
        MusicTrack track = track(10L, true, audio("mp3"));
        when(musicTrackRepository.findById(10L)).thenReturn(Optional.of(track));

        assertThatThrownBy(() -> service.publishAdmin(3L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);

        verify(auditLogUseCase, never()).write(any());
    }

    @Test
    @DisplayName("hideAdmin은 ACTIVE 음원을 HIDDEN으로 전환한다")
    void hideAdmin_activeTrack_disablesTrack() {
        MusicTrack track = track(10L, true, audio("mp3"));
        when(musicTrackRepository.findById(10L)).thenReturn(Optional.of(track));

        AdminMusicTrackResponse response = service.hideAdmin(3L, 10L);

        assertThat(track.getEnabled()).isFalse();
        assertThat(response.status()).isEqualTo("HIDDEN");
        verify(auditLogUseCase).write(any());
    }

    @Test
    @DisplayName("없는 음원은 RESOURCE_NOT_FOUND")
    void missingTrack_notFound() {
        when(musicTrackRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.hideAdmin(3L, 404L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private static AdminMusicTrackCommand command(String title, byte[] audioData) {
        return new AdminMusicTrackCommand(
                title,
                MusicCategory.BGM.name(),
                "audio/mpeg",
                180,
                5,
                "운영자가 라이선스를 확인함",
                audioData
        );
    }

    private static MusicTrack track(Long id, boolean enabled, byte[] audioData) {
        return track(
                id,
                enabled,
                MusicCategory.BGM,
                "audio/mpeg",
                120,
                1,
                "라이선스 확인",
                audioData
        );
    }

    private static MusicTrack track(
            Long id,
            boolean enabled,
            MusicCategory category,
            String mimeType,
            Integer durationSec,
            Integer sortOrder,
            String licenseNote,
            byte[] audioData) {
        MusicTrack track = MusicTrack.builder()
                .title("기존 배경음악")
                .category(category)
                .mimeType(mimeType)
                .byteSize((long) audioData.length)
                .durationSec(durationSec)
                .sortOrder(sortOrder)
                .enabled(enabled)
                .licenseNote(licenseNote)
                .audioData(audioData)
                .build();
        ReflectionTestUtils.setField(track, "id", id);
        return track;
    }

    private static byte[] audio(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static AdminMusicTrackSummary summary(Long id, boolean enabled) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 15, 10, 0);
        return new AdminMusicTrackSummary() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return "Morning BGM";
            }

            @Override
            public MusicCategory getCategory() {
                return MusicCategory.BGM;
            }

            @Override
            public String getMimeType() {
                return "audio/mpeg";
            }

            @Override
            public Long getByteSize() {
                return 5L;
            }

            @Override
            public Integer getDurationSec() {
                return 180;
            }

            @Override
            public Integer getSortOrder() {
                return 5;
            }

            @Override
            public Boolean getEnabled() {
                return enabled;
            }

            @Override
            public String getLicenseNote() {
                return "license checked";
            }

            @Override
            public LocalDateTime getCreatedAt() {
                return now;
            }

            @Override
            public LocalDateTime getUpdatedAt() {
                return now;
            }
        };
    }
}
