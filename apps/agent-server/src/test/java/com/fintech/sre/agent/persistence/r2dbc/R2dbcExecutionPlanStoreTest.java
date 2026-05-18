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
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStatus;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStep;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcExecutionPlanStoreTest {

	@Test
	void shouldSaveAndQueryExecutionPlansThroughReactiveBoundary() {
		Map<String, RecommendationExecutionPlanEntity> entities =
				new ConcurrentHashMap<>();
		RecommendationExecutionPlanR2dbcRepository repository =
				repository(entities);
		RecommendationExecutionPlanEntityMapper mapper =
				new RecommendationExecutionPlanEntityMapper(new ObjectMapper());
		R2dbcExecutionPlanStore store =
				new R2dbcExecutionPlanStore(repository, mapper);

		RecommendationExecutionPlan older = plan(
				"plan-1",
				"rec-1",
				"incident-1",
				Instant.parse("2026-05-09T00:00:00Z")
		);
		RecommendationExecutionPlan newer = plan(
				"plan-2",
				"rec-1",
				"incident-1",
				Instant.parse("2026-05-09T01:00:00Z")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findById("plan-1").block())
				.isNotNull()
				.extracting(RecommendationExecutionPlan::executionPlanId)
				.isEqualTo("plan-1");
		assertThat(store.findByRecommendationRecordId("rec-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(RecommendationExecutionPlan::executionPlanId)
				.isEqualTo("plan-2");
	}

	private RecommendationExecutionPlan plan(
			String planId,
			String recommendationRecordId,
			String incidentId,
			Instant createdAt
	) {
		return new RecommendationExecutionPlan(
				planId,
				recommendationRecordId,
				incidentId,
				ExecutionPlanStatus.DRY_RUN_PLAN_CREATED,
				false,
				true,
				"operator-a",
				"dry-run",
				createdAt,
				List.of(new ExecutionPlanStep(
						"RATE_LIMIT",
						"payment-api",
						"application",
						"HIGH",
						"SINGLE_SERVICE",
						true,
						true,
						true,
						true,
						true,
						Map.of("threshold", "100")
				)),
				List.of("NO_SCENARIO")
		);
	}

	@SuppressWarnings("unchecked")
	private RecommendationExecutionPlanR2dbcRepository repository(
			Map<String, RecommendationExecutionPlanEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				RecommendationExecutionPlanEntity entity =
						(RecommendationExecutionPlanEntity) args[0];
				entities.put(entity.getExecutionPlanId(), entity);
				yield Mono.just(entity);
			}
			case "findById" -> Mono.justOrEmpty(entities.get(args[0]));
			case "findByRecommendationRecordIdOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getRecommendationRecordId()))
							.sorted(Comparator.comparing(
									RecommendationExecutionPlanEntity::getCreatedAt
							).reversed())
			);
			case "findByIncidentIdOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									RecommendationExecutionPlanEntity::getCreatedAt
							).reversed())
			);
			case "findTop500ByOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									RecommendationExecutionPlanEntity::getCreatedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (RecommendationExecutionPlanR2dbcRepository) Proxy.newProxyInstance(
				RecommendationExecutionPlanR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { RecommendationExecutionPlanR2dbcRepository.class },
				handler
		);
	}
}
