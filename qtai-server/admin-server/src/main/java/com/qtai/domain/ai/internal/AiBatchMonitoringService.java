package com.qtai.domain.ai.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class AiBatchMonitoringService {

    private final AiBatchRunLogRepository repository;

    AiBatchMonitoringService(AiBatchRunLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiBatchRunLogCommand command) {
        repository.save(AiBatchRunLog.create(command));
    }
}
