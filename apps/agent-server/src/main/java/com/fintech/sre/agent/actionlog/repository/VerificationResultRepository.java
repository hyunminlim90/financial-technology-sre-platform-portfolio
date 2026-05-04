package com.fintech.sre.agent.actionlog.repository;

import java.util.List;

import com.fintech.sre.agent.actionlog.entity.VerificationResultEntity;

public interface VerificationResultRepository {

	VerificationResultEntity save(VerificationResultEntity entity);

	List<VerificationResultEntity> findByIncidentId(String incidentId);
}
