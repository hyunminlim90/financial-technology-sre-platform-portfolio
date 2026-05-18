package com.fintech.sre.agent.learning.plan;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryKnowledgePromotionPlanStore implements KnowledgePromotionPlanStore {

	private final Map<String, KnowledgePromotionPlanRecord> records =
			new ConcurrentHashMap<>();

	@Override
	public Mono<KnowledgePromotionPlanRecord> save(KnowledgePromotionPlanRecord record) {
		if (record == null) {
			return Mono.empty();
		}

		records.put(record.promotionPlanId(), record);
		return Mono.just(record);
	}

	@Override
	public Mono<KnowledgePromotionPlanRecord> findById(String promotionPlanId) {
		KnowledgePromotionPlanRecord record = records.get(promotionPlanId);
		return record == null ? Mono.empty() : Mono.just(record);
	}

	@Override
	public Flux<KnowledgePromotionPlanRecord> findByLearningCandidateId(String learningCandidateId) {
		return Flux.fromStream(records.values().stream()
				.filter(record -> learningCandidateId != null
						&& learningCandidateId.equals(record.learningCandidateId()))
				.sorted(Comparator.comparing(KnowledgePromotionPlanRecord::createdAt).reversed()));
	}

	@Override
	public Flux<KnowledgePromotionPlanRecord> findByIncidentId(String incidentId) {
		return Flux.fromStream(records.values().stream()
				.filter(record -> incidentId != null && incidentId.equals(record.incidentId()))
				.sorted(Comparator.comparing(KnowledgePromotionPlanRecord::createdAt).reversed()));
	}

	@Override
	public Flux<KnowledgePromotionPlanRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(records.values().stream()
				.sorted(Comparator.comparing(KnowledgePromotionPlanRecord::createdAt).reversed())
				.limit(safeLimit));
	}
}
