package com.fintech.sre.agent.actionlog.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.fintech.sre.agent.actionlog.entity.RecommendationActionEntity;

@Repository
public class InMemoryRecommendationActionRepository implements RecommendationActionRepository {

	private final AtomicLong sequence = new AtomicLong(1);
	private final ConcurrentHashMap<Long, RecommendationActionEntity> storage = new ConcurrentHashMap<>();

	@Override
	public RecommendationActionEntity save(RecommendationActionEntity entity) {
		Long id = entity.id() == null ? sequence.getAndIncrement() : entity.id();
		RecommendationActionEntity saved = entity.toBuilder().id(id).build();
		storage.put(id, saved);
		return saved;
	}

	@Override
	public List<RecommendationActionEntity> findByIncidentId(String incidentId) {
		return storage.values().stream()
				.filter(entity -> entity.incidentId().equals(incidentId))
				.sorted(java.util.Comparator.comparing(RecommendationActionEntity::step))
				.toList();
	}

	@Override
	public List<RecommendationActionEntity> findByRecommendationId(String recommendationId) {
		return storage.values().stream()
				.filter(entity -> entity.recommendationId().equals(recommendationId))
				.sorted(java.util.Comparator.comparing(RecommendationActionEntity::step))
				.toList();
	}

	@Override
	public Optional<RecommendationActionEntity> findById(Long id) {
		return Optional.ofNullable(storage.get(id));
	}
}
