package com.fintech.sre.agent.actionlog.repository;

import java.util.List;
import java.util.Optional;

import com.fintech.sre.agent.actionlog.entity.RecommendationActionEntity;

public interface RecommendationActionRepository {

	RecommendationActionEntity save(RecommendationActionEntity entity);

	List<RecommendationActionEntity> findByIncidentId(String incidentId);

	List<RecommendationActionEntity> findByRecommendationId(String recommendationId);

	Optional<RecommendationActionEntity> findById(Long id);
}
