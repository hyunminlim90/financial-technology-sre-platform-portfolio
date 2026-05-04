package com.fintech.sre.agent.actionlog.repository;

import java.util.List;
import java.util.Optional;

import com.fintech.sre.agent.actionlog.entity.ExecutedActionEntity;

public interface ExecutedActionRepository {

	ExecutedActionEntity save(ExecutedActionEntity entity);

	Optional<ExecutedActionEntity> findById(Long id);

	List<ExecutedActionEntity> findByIncidentId(String incidentId);
}
