package com.qtai.domain.appversion.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.domain.appversion.api.dto.AppVersionStateResponse;
import com.qtai.domain.appversion.api.dto.PendingAppUpdateCreateRequest;
import com.qtai.domain.appversion.api.dto.PendingAppUpdateResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 앱 버전/업데이트 서비스 단위 테스트 (AD-19, 2026-06-14 Lead 승인).
 */
@ExtendWith(MockitoExtension.class)
class AppVersionServiceTest {

    @Mock
    private AppVersionStateRepository stateRepository;

    @Mock
    private PendingAppUpdateRepository pendingRepository;

    private AppVersionService service() {
        Clock clock = Clock.systemUTC();
        return new AppVersionService(stateRepository, pendingRepository, clock);
    }

    @Test
    @DisplayName("콘텐츠 적용은 마지막 자리를 올린다 (0.1.0 → 0.1.0.1)")
    void applyContent_bumpsPatch() {
        AppVersionState state = AppVersionState.builder().contentVersion("0.1.0").build();
        when(stateRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(state));

        AppVersionStateResponse response = service().applyContent();

        assertThat(response.contentVersion()).isEqualTo("0.1.0.1");
    }

    @Test
    @DisplayName("상태가 없으면 기본값을 만들고 콘텐츠를 올린다")
    void applyContent_createsDefault() {
        when(stateRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(stateRepository.save(any(AppVersionState.class))).thenAnswer(inv -> inv.getArgument(0));

        AppVersionStateResponse response = service().applyContent();

        assertThat(response.contentVersion()).isEqualTo("0.1.0.1");
    }

    @Test
    @DisplayName("업데이트 예정 등록 시 PENDING으로 저장된다")
    void createPending_savesPending() {
        when(pendingRepository.save(any(PendingAppUpdate.class))).thenAnswer(inv -> inv.getArgument(0));

        PendingAppUpdateResponse response = service().createPending(
                new PendingAppUpdateCreateRequest("음원 대량 추가", "앱 번들 갱신", "0.2.0", "FORCED"));

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.updateMode()).isEqualTo("FORCED");
        assertThat(response.targetAppVersion()).isEqualTo("0.2.0");
    }

    @Test
    @DisplayName("업데이트 예정 적용은 앱 출시 버전을 올리고 강제 시 최소버전을 맞춘다")
    void applyPending_promotesAppVersion() {
        PendingAppUpdate pending = PendingAppUpdate.builder()
                .title("대규모 업데이트").targetAppVersion("0.2.0").updateMode(AppUpdateMode.FORCED).build();
        AppVersionState state = AppVersionState.builder()
                .contentVersion("0.1.0").appVersion("0.1.0").build();
        when(pendingRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pending));
        when(stateRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(state));

        AppVersionStateResponse response = service().applyPending(1L);

        assertThat(response.appVersion()).isEqualTo("0.2.0");
        assertThat(response.minSupportedVersion()).isEqualTo("0.2.0");
        assertThat(response.updateMode()).isEqualTo("FORCED");
        assertThat(pending.getStatus()).isEqualTo(PendingUpdateStatus.APPLIED);
    }

    @Test
    @DisplayName("이미 적용된 항목은 다시 적용할 수 없다 (400)")
    void applyPending_alreadyApplied_rejected() {
        PendingAppUpdate pending = PendingAppUpdate.builder()
                .title("x").targetAppVersion("0.2.0").updateMode(AppUpdateMode.RECOMMENDED).build();
        pending.markApplied(LocalDateTime.now());
        when(pendingRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service().applyPending(1L))
                .isInstanceOf(BusinessException.class);
        verify(stateRepository, never()).save(any());
    }

    @Test
    @DisplayName("nextContentVersion: 3자리는 패치 추가, 4자리는 마지막 증가")
    void nextContentVersion_rules() {
        assertThat(AppVersionState.nextContentVersion("0.1.0")).isEqualTo("0.1.0.1");
        assertThat(AppVersionState.nextContentVersion("0.1.0.1")).isEqualTo("0.1.0.2");
        assertThat(AppVersionState.nextContentVersion("0.1.0.9")).isEqualTo("0.1.0.10");
    }
}
