package com.fintech.sre.agent.actionlog.repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.fintech.sre.agent.actionlog.entity.RollbackRecordEntity;

@Repository
public class InMemoryRollbackRecordRepository implements RollbackRecordRepository {

	private final AtomicLong sequence = new AtomicLong(1);
	private final ConcurrentHashMap<Long, RollbackRecordEntity> storage = new ConcurrentHashMap<>();

	@Override
	public RollbackRecordEntity save(RollbackRecordEntity entity) {
		Long id = entity.id() == null ? sequence.getAndIncrement() : entity.id();
		RollbackRecordEntity saved = entity.toBuilder().id(id).build();
		storage.put(id, saved);
		return saved;
	}

	@Override
	public List<RollbackRecordEntity> findByIncidentId(String incidentId) {
		return storage.values().stream()
				.filter(entity -> entity.incidentId().equals(incidentId))
				.sorted(java.util.Comparator.comparing(RollbackRecordEntity::rollbackAt))
				.toList();
	}
}
