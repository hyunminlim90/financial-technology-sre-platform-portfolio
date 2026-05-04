package com.fintech.sre.agent.actionlog.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.fintech.sre.agent.actionlog.entity.ExecutedActionEntity;

@Repository
public class InMemoryExecutedActionRepository implements ExecutedActionRepository {

	private final AtomicLong sequence = new AtomicLong(1);
	private final ConcurrentHashMap<Long, ExecutedActionEntity> storage = new ConcurrentHashMap<>();

	@Override
	public ExecutedActionEntity save(ExecutedActionEntity entity) {
		Long id = entity.id() == null ? sequence.getAndIncrement() : entity.id();
		ExecutedActionEntity saved = entity.toBuilder().id(id).build();
		storage.put(id, saved);
		return saved;
	}

	@Override
	public Optional<ExecutedActionEntity> findById(Long id) {
		return Optional.ofNullable(storage.get(id));
	}

	@Override
	public List<ExecutedActionEntity> findByIncidentId(String incidentId) {
		return storage.values().stream()
				.filter(entity -> entity.incidentId().equals(incidentId))
				.sorted(java.util.Comparator.comparing(ExecutedActionEntity::executedAt))
				.toList();
	}
}
