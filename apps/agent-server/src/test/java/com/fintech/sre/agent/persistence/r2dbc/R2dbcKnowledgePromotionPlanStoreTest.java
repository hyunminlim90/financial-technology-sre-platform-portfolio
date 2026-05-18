package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanTarget;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionTargetType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcKnowledgePromotionPlanStoreTest {

	@Test
	void shouldSaveAndQueryKnowledgePromotionPlansThroughReactiveBoundary() {
		Map<String, KnowledgePromotionPlanEntity> entities =
				new ConcurrentHashMap<>();
		KnowledgePromotionPlanR2dbcRepository repository =
				repository(entities);
		KnowledgePromotionPlanEntityMapper mapper =
				new KnowledgePromotionPlanEntityMapper(new ObjectMapper());
		R2dbcKnowledgePromotionPlanStore store =
				new R2dbcKnowledgePromotionPlanStore(repository, mapper);

		KnowledgePromotionPlanRecord older = record(
				"promotion-plan-1",
				"candidate-1",
				"incident-1",
				Instant.parse("2026-05-09T10:00:00Z"),
				Map.of("owner", "sre")
		);
		KnowledgePromotionPlanRecord newer = record(
				"promotion-plan-2",
				"candidate-1",
				"incident-1",
				Instant.parse("2026-05-09T11:00:00Z"),
				Map.of("promptPayload", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findById("promotion-plan-1").block())
				.isNotNull()
				.extracting(KnowledgePromotionPlanRecord::promotionPlanId)
				.isEqualTo("promotion-plan-1");
		assertThat(store.findByLearningCandidateId("candidate-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(KnowledgePromotionPlanRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("promptPayload");
	}

	private KnowledgePromotionPlanRecord record(
			String promotionPlanId,
			String learningCandidateId,
			String incidentId,
			Instant createdAt,
			Map<String, String> metadata
	) {
		return new KnowledgePromotionPlanRecord(
				promotionPlanId,
				learningCandidateId,
				incidentId,
				KnowledgePromotionPlanStatus.PLAN_CREATED,
				"planner-a",
				"Human should prepare a runbook update plan.",
				List.of(new KnowledgePromotionPlanTarget(
						KnowledgePromotionTargetType.RUNBOOK,
						"runbooks/payments/payment-api-runbook.md",
						"Update rollback and verification guidance.",
						List.of("Add rollback verification step."),
						List.of("Confirm rollback step exists.")
				)),
				List.of("Human must edit portfolio knowledge files manually."),
				List.of(),
				createdAt,
				metadata
		);
	}

	@SuppressWarnings("unchecked")
	private KnowledgePromotionPlanR2dbcRepository repository(
			Map<String, KnowledgePromotionPlanEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				KnowledgePromotionPlanEntity entity =
						(KnowledgePromotionPlanEntity) args[0];
				entities.put(entity.getPromotionPlanId(), entity);
				yield Mono.just(entity);
			}
			case "findById" -> Mono.justOrEmpty(entities.get(args[0]));
			case "findByLearningCandidateIdOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getLearningCandidateId()))
							.sorted(Comparator.comparing(
									KnowledgePromotionPlanEntity::getCreatedAt
							).reversed())
			);
			case "findByIncidentIdOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									KnowledgePromotionPlanEntity::getCreatedAt
							).reversed())
			);
			case "findTop500ByOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									KnowledgePromotionPlanEntity::getCreatedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (KnowledgePromotionPlanR2dbcRepository) Proxy.newProxyInstance(
				KnowledgePromotionPlanR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { KnowledgePromotionPlanR2dbcRepository.class },
				handler
		);
	}
}
