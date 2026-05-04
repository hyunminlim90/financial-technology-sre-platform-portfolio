package com.fintech.sre.agent.actionlog.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.fintech.sre.agent.actionlog.entity.IncidentRecommendationEntity;

@Repository
public class InMemoryIncidentRecommendationRepository implements IncidentRecommendationRepository {

	private final AtomicLong sequence = new AtomicLong(1);
	private final ConcurrentHashMap<Long, IncidentRecommendationEntity> storage = new ConcurrentHashMap<>();

	@Override
	public IncidentRecommendationEntity save(IncidentRecommendationEntity entity) {
		Long id = entity.id() == null ? sequence.getAndIncrement() : entity.id();
		IncidentRecommendationEntity saved = entity.toBuilder().id(id).build();
		storage.put(id, saved);
		return saved;
	}

	@Override
	public Optional<IncidentRecommendationEntity> findByRecommendationId(String recommendationId) {
		return storage.values().stream()
				.filter(entity -> entity.recommendationId().equals(recommendationId))
				.findFirst();
	}

	@Override
	public List<IncidentRecommendationEntity> findByIncidentId(String incidentId) {
		return storage.values().stream()
				.filter(entity -> entity.incidentId().equals(incidentId))
				.sorted(java.util.Comparator.comparing(IncidentRecommendationEntity::createdAt))
				.toList();
	}
}
