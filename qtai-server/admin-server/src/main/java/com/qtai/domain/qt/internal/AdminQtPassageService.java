package com.qtai.domain.qt.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.qt.api.admin.CreateAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.HideAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.ListAdminQtPassagesUseCase;
import com.qtai.domain.qt.api.admin.PublishAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.UpdateAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageCommand;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageListResponse;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageResponse;
import com.qtai.domain.qt.api.admin.dto.ListAdminQtPassagesQuery;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminQtPassageService implements
        ListAdminQtPassagesUseCase,
        CreateAdminQtPassageUseCase,
        UpdateAdminQtPassageUseCase,
        PublishAdminQtPassageUseCase,
        HideAdminQtPassageUseCase {

    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String TARGET_TYPE_QT_PASSAGE = "QT_PASSAGE";

    private final QtPassageRepository qtPassageRepository;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TodayQtCacheEvictor todayQtCacheEvictor;

    @Autowired
    public AdminQtPassageService(
            QtPassageRepository qtPassageRepository,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper,
            TodayQtCacheEvictor todayQtCacheEvictor
    ) {
        this(qtPassageRepository, auditLogUseCase, objectMapper, Clock.systemDefaultZone(), todayQtCacheEvictor);
    }

    AdminQtPassageService(
            QtPassageRepository qtPassageRepository,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper,
            Clock clock,
            TodayQtCacheEvictor todayQtCacheEvictor
    ) {
        this.qtPassageRepository = qtPassageRepository;
        this.auditLogUseCase = auditLogUseCase;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.todayQtCacheEvictor = todayQtCacheEvictor;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminQtPassageListResponse list(ListAdminQtPassagesQuery query) {
        int page = Math.max(query.page(), 0);
        int size = Math.min(Math.max(query.size(), 1), 100);
        Page<QtPassage> result = qtPassageRepository.findAll(
                specification(query),
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("qtDate"), Sort.Order.desc("id")))
        );
        return new AdminQtPassageListResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Override
    @Transactional
    public AdminQtPassageResponse create(AdminQtPassageCommand command) {
        validateCommand(command);
        if (qtPassageRepository.existsByQtDate(command.qtDate())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 등록된 QT 날짜입니다.");
        }
        QtPassage passage = QtPassage.create(
                command.qtDate(),
                command.bookId(),
                command.bookId(),
                command.chapter(),
                command.endChapter(),
                command.startVerse(),
                command.endVerse(),
                command.title().trim(),
                normalized(command.mainVerseRef())
        );
        QtPassage saved = qtPassageRepository.save(passage);
        writeAudit(command.adminId(), "QT_PASSAGE_CREATE", saved.getId(), null, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminQtPassageResponse update(Long qtPassageId, AdminQtPassageCommand command) {
        validateId(qtPassageId);
        validateCommand(command);
        QtPassage passage = findPassage(qtPassageId);
        if (qtPassageRepository.existsByQtDateAndIdNot(command.qtDate(), qtPassageId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 등록된 QT 날짜입니다.");
        }
        String beforeJson = snapshot(passage);
        passage.updateAdminRange(
                command.qtDate(),
                command.bookId(),
                command.chapter(),
                command.endChapter(),
                command.startVerse(),
                command.endVerse(),
                command.title().trim(),
                normalized(command.mainVerseRef())
        );
        writeAudit(command.adminId(), "QT_PASSAGE_UPDATE", passage.getId(), beforeJson, snapshot(passage));
        todayQtCacheEvictor.evictAfterCommit();
        return toResponse(passage);
    }

    @Override
    @Transactional
    public AdminQtPassageResponse publish(Long adminId, Long qtPassageId) {
        validateAdminId(adminId);
        validateId(qtPassageId);
        QtPassage passage = findPassage(qtPassageId);
        String beforeJson = snapshot(passage);
        passage.publish(LocalDateTime.now(clock));
        writeAudit(adminId, "QT_PASSAGE_PUBLISH", passage.getId(), beforeJson, snapshot(passage));
        todayQtCacheEvictor.evictAfterCommit();
        return toResponse(passage);
    }

    @Override
    @Transactional
    public AdminQtPassageResponse hide(Long adminId, Long qtPassageId) {
        validateAdminId(adminId);
        validateId(qtPassageId);
        QtPassage passage = findPassage(qtPassageId);
        String beforeJson = snapshot(passage);
        passage.hide(LocalDateTime.now(clock));
        writeAudit(adminId, "QT_PASSAGE_HIDE", passage.getId(), beforeJson, snapshot(passage));
        todayQtCacheEvictor.evictAfterCommit();
        return toResponse(passage);
    }

    private Specification<QtPassage> specification(ListAdminQtPassagesQuery query) {
        QtPassageStatus parsedStatus = (query.status() == null || query.status().isBlank())
                ? null
                : parseStatus(query.status());
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (parsedStatus != null) {
                predicates.add(cb.equal(root.get("status"), parsedStatus));
            }
            if (query.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("qtDate"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("qtDate"), query.to()));
            }
            if (query.q() != null && !query.q().isBlank()) {
                String keyword = "%" + query.q().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), keyword),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("mainVerseRef"), "")), keyword)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private QtPassage findPassage(Long qtPassageId) {
        return qtPassageRepository.findById(qtPassageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));
    }

    private static QtPassageStatus parseStatus(String status) {
        try {
            return QtPassageStatus.fromApiValue(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 QT 상태입니다.");
        }
    }

    private static void validateCommand(AdminQtPassageCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        validateAdminId(command.adminId());
        if (command.qtDate() == null
                || !positive(command.bookId())
                || !positive(command.chapter())
                || !positive(command.endChapter())
                || !positive(command.startVerse())
                || !positive(command.endVerse())
                || command.title() == null
                || command.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (command.endChapter() < command.chapter()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "종료 장은 시작 장 이상이어야 합니다.");
        }
        // 같은 장 범위에서만 절 순서를 강제한다. 장 교차(예: 9:1-10:5)는 종료 절이 더 작을 수 있다.
        if (command.chapter().equals(command.endChapter()) && command.startVerse() > command.endVerse()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 장에서는 시작 절이 종료 절보다 클 수 없습니다.");
        }
    }

    private static void validateAdminId(Long adminId) {
        if (adminId == null || adminId < 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private static void validateId(Long id) {
        if (id == null || id < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private static boolean positive(Short value) {
        return value != null && value > 0;
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AdminQtPassageResponse toResponse(QtPassage passage) {
        return new AdminQtPassageResponse(
                passage.getId(),
                passage.getQtDate(),
                passage.getBookId(),
                passage.getChapter(),
                passage.getEndChapter(),
                passage.getStartVerse(),
                passage.getEndVerse(),
                passage.getTitle(),
                passage.getMainVerseRef(),
                passage.getStatus().apiValue(),
                passage.getPublishedAt(),
                passage.getCollectedAt(),
                passage.getHiddenAt(),
                passage.getCreatedAt(),
                passage.getUpdatedAt()
        );
    }

    private void writeAudit(Long adminId, String actionType, Long targetId, String beforeJson, String afterJson) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                adminId,
                ACTOR_TYPE_ADMIN,
                adminId,
                ACTOR_TYPE_ADMIN + ":" + adminId,
                actionType,
                TARGET_TYPE_QT_PASSAGE,
                targetId,
                beforeJson,
                afterJson
        ));
    }

    private String snapshot(QtPassage passage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", passage.getId());
        payload.put("qtDate", passage.getQtDate());
        payload.put("bookId", passage.getBookId());
        payload.put("chapter", passage.getChapter());
        payload.put("endChapter", passage.getEndChapter());
        payload.put("startVerse", passage.getStartVerse());
        payload.put("endVerse", passage.getEndVerse());
        payload.put("title", passage.getTitle());
        payload.put("mainVerseRef", passage.getMainVerseRef());
        payload.put("status", passage.getStatus().apiValue());
        payload.put("publishedAt", passage.getPublishedAt());
        payload.put("collectedAt", passage.getCollectedAt());
        payload.put("hiddenAt", passage.getHiddenAt());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "audit snapshot serialization failed");
        }
    }
}
