package com.fintech.sre.agent.actionlog.repository;

import java.util.List;

import com.fintech.sre.agent.actionlog.entity.RollbackRecordEntity;

public interface RollbackRecordRepository {

	RollbackRecordEntity save(RollbackRecordEntity entity);

	List<RollbackRecordEntity> findByIncidentId(String incidentId);
}
