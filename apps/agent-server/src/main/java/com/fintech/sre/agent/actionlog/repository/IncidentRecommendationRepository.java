package com.fintech.sre.agent.actionlog.repository;

import java.util.List;
import java.util.Optional;

import com.fintech.sre.agent.actionlog.entity.IncidentRecommendationEntity;

public interface IncidentRecommendationRepository {

	IncidentRecommendationEntity save(IncidentRecommendationEntity entity);

	Optional<IncidentRecommendationEntity> findByRecommendationId(String recommendationId);

	List<IncidentRecommendationEntity> findByIncidentId(String incidentId);
}
