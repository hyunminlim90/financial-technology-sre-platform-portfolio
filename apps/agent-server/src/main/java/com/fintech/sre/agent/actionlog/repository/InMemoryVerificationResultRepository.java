package com.fintech.sre.agent.actionlog.repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.fintech.sre.agent.actionlog.entity.VerificationResultEntity;

@Repository
public class InMemoryVerificationResultRepository implements VerificationResultRepository {

	private final AtomicLong sequence = new AtomicLong(1);
	private final ConcurrentHashMap<Long, VerificationResultEntity> storage = new ConcurrentHashMap<>();

	@Override
	public VerificationResultEntity save(VerificationResultEntity entity) {
		Long id = entity.id() == null ? sequence.getAndIncrement() : entity.id();
		VerificationResultEntity saved = entity.toBuilder().id(id).build();
		storage.put(id, saved);
		return saved;
	}

	@Override
	public List<VerificationResultEntity> findByIncidentId(String incidentId) {
		return storage.values().stream()
				.filter(entity -> entity.incidentId().equals(incidentId))
				.sorted(java.util.Comparator.comparing(VerificationResultEntity::checkedAt))
				.toList();
	}
}
