package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcKnowledgePromotionReviewStoreTest {

	@Test
	void shouldSaveAndQueryKnowledgePromotionReviewsThroughReactiveBoundary() {
		Map<String, KnowledgePromotionReviewEntity> entities =
				new ConcurrentHashMap<>();
		KnowledgePromotionReviewR2dbcRepository repository =
				repository(entities);
		KnowledgePromotionReviewEntityMapper mapper =
				new KnowledgePromotionReviewEntityMapper(new ObjectMapper());
		R2dbcKnowledgePromotionReviewStore store =
				new R2dbcKnowledgePromotionReviewStore(repository, mapper);

		KnowledgePromotionReviewRecord older = record(
				"promotion-review-1",
				"candidate-1",
				"incident-1",
				Instant.parse("2026-05-09T08:00:00Z"),
				Map.of("owner", "sre")
		);
		KnowledgePromotionReviewRecord newer = record(
				"promotion-review-2",
				"candidate-1",
				"incident-1",
				Instant.parse("2026-05-09T09:00:00Z"),
				Map.of("paymentPayload", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findLatestByLearningCandidateId("candidate-1").block())
				.isNotNull()
				.extracting(KnowledgePromotionReviewRecord::promotionReviewId)
				.isEqualTo("promotion-review-2");
		assertThat(store.findByLearningCandidateId("candidate-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(KnowledgePromotionReviewRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("paymentPayload");
	}

	private KnowledgePromotionReviewRecord record(
			String promotionReviewId,
			String learningCandidateId,
			String incidentId,
			Instant reviewedAt,
			Map<String, String> metadata
	) {
		return new KnowledgePromotionReviewRecord(
				promotionReviewId,
				learningCandidateId,
				incidentId,
				KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
				"reviewer-a",
				"Ready for human promotion planning.",
				"Eligible for planning but not yet a file update.",
				reviewedAt,
				metadata
		);
	}

	@SuppressWarnings("unchecked")
	private KnowledgePromotionReviewR2dbcRepository repository(
			Map<String, KnowledgePromotionReviewEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				KnowledgePromotionReviewEntity entity =
						(KnowledgePromotionReviewEntity) args[0];
				entities.put(entity.getPromotionReviewId(), entity);
				yield Mono.just(entity);
			}
			case "findFirstByLearningCandidateIdOrderByReviewedAtDesc" -> Mono.justOrEmpty(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getLearningCandidateId()))
							.max(Comparator.comparing(KnowledgePromotionReviewEntity::getReviewedAt))
			);
			case "findByLearningCandidateIdOrderByReviewedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getLearningCandidateId()))
							.sorted(Comparator.comparing(
									KnowledgePromotionReviewEntity::getReviewedAt
							).reversed())
			);
			case "findByIncidentIdOrderByReviewedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									KnowledgePromotionReviewEntity::getReviewedAt
							).reversed())
			);
			case "findTop500ByOrderByReviewedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									KnowledgePromotionReviewEntity::getReviewedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (KnowledgePromotionReviewR2dbcRepository) Proxy.newProxyInstance(
				KnowledgePromotionReviewR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { KnowledgePromotionReviewR2dbcRepository.class },
				handler
		);
	}
}
