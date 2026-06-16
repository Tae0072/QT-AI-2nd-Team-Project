package com.qtai.domain.mission.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.mission.api.AdminMissionUseCase;
import com.qtai.domain.mission.api.dto.AdminMissionResponse;
import com.qtai.domain.mission.api.dto.MissionCreateRequest;
import com.qtai.domain.mission.api.dto.MissionUpdateRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 미션 정의 운영 서비스 (F-13).
 *
 * <p>admin-server 고유 기능. 미션 정의 CRUD를 담당하고 진행률 집계는 건드리지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMissionService implements AdminMissionUseCase {

    private final MissionDefinitionRepository missionDefinitionRepository;
    private final Clock clock;

    @Override
    public List<AdminMissionResponse> listForAdmin() {
        return missionDefinitionRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AdminMissionResponse getForAdmin(Long id) {
        return toResponse(load(id));
    }

    @Override
    @Transactional
    public AdminMissionResponse create(MissionCreateRequest request) {
        if (missionDefinitionRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 존재하는 미션 code입니다: " + request.code());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        MissionDefinition definition = MissionDefinition.builder()
                .code(request.code())
                .title(request.title())
                .metricType(MissionMetricType.valueOf(request.metricType()))
                .periodType(MissionPeriodType.valueOf(request.periodType()))
                .targetCount(request.targetCount())
                .status(MissionDefinitionStatus.ACTIVE)
                .createdAt(now)
                .build();
        MissionDefinition saved = missionDefinitionRepository.save(definition);
        log.info("관리자 미션 정의 생성. id={}, code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminMissionResponse update(Long id, MissionUpdateRequest request) {
        MissionDefinition definition = load(id);
        MissionMetricType metric = request.metricType() == null
                ? null : MissionMetricType.valueOf(request.metricType());
        MissionPeriodType period = request.periodType() == null
                ? null : MissionPeriodType.valueOf(request.periodType());
        definition.update(request.title(), metric, period, request.targetCount(), LocalDateTime.now(clock));
        log.info("관리자 미션 정의 수정. id={}", id);
        return toResponse(definition);
    }

    @Override
    @Transactional
    public AdminMissionResponse changeStatus(Long id, String status) {
        MissionDefinition definition = load(id);
        MissionDefinitionStatus target;
        try {
            target = MissionDefinitionStatus.valueOf(status.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status는 ACTIVE 또는 HIDDEN만 허용됩니다.");
        }
        definition.changeStatus(target, LocalDateTime.now(clock));
        log.info("관리자 미션 상태 변경. id={}, status={}", id, target);
        return toResponse(definition);
    }

    private MissionDefinition load(Long id) {
        return missionDefinitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "미션 정의를 찾을 수 없습니다: " + id));
    }

    private AdminMissionResponse toResponse(MissionDefinition d) {
        return new AdminMissionResponse(
                d.getId(), d.getCode(), d.getTitle(),
                d.getMetricType().name(), d.getPeriodType().name(),
                d.getTargetCount(), d.getStatus().name(),
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
