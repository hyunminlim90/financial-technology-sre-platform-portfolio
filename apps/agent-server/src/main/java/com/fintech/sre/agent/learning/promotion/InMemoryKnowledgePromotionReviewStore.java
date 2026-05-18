package com.fintech.sre.agent.learning.promotion;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryKnowledgePromotionReviewStore implements KnowledgePromotionReviewStore {

	private final List<KnowledgePromotionReviewRecord> records = new CopyOnWriteArrayList<>();

	@Override
	public Mono<KnowledgePromotionReviewRecord> save(KnowledgePromotionReviewRecord record) {
		if (record != null) {
			records.add(record);
		}

		return Mono.justOrEmpty(record);
	}

	@Override
	public Mono<KnowledgePromotionReviewRecord> findLatestByLearningCandidateId(String learningCandidateId) {
		return findByLearningCandidateId(learningCandidateId).next();
	}

	@Override
	public Flux<KnowledgePromotionReviewRecord> findByLearningCandidateId(String learningCandidateId) {
		return Flux.fromStream(records.stream()
				.filter(record -> learningCandidateId != null
						&& learningCandidateId.equals(record.learningCandidateId()))
				.sorted(Comparator.comparing(KnowledgePromotionReviewRecord::reviewedAt).reversed()));
	}

	@Override
	public Flux<KnowledgePromotionReviewRecord> findByIncidentId(String incidentId) {
		return Flux.fromStream(records.stream()
				.filter(record -> incidentId != null && incidentId.equals(record.incidentId()))
				.sorted(Comparator.comparing(KnowledgePromotionReviewRecord::reviewedAt).reversed()));
	}

	@Override
	public Flux<KnowledgePromotionReviewRecord> findRecent(int limit) {
		int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);

		return Flux.fromStream(records.stream()
				.sorted(Comparator.comparing(KnowledgePromotionReviewRecord::reviewedAt).reversed())
				.limit(safeLimit));
	}
}
