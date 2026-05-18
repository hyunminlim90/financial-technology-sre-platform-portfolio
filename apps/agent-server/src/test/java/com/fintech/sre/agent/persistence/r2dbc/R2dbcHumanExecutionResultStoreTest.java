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
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcHumanExecutionResultStoreTest {

	@Test
	void shouldSaveAndQueryHumanExecutionResultsThroughReactiveBoundary() {
		Map<String, HumanExecutionResultEntity> entities =
				new ConcurrentHashMap<>();
		HumanExecutionResultR2dbcRepository repository =
				repository(entities);
		HumanExecutionResultEntityMapper mapper =
				new HumanExecutionResultEntityMapper(new ObjectMapper());
		R2dbcHumanExecutionResultStore store =
				new R2dbcHumanExecutionResultStore(repository, mapper);

		HumanExecutionResultRecord older = record(
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				Instant.parse("2026-05-09T00:00:00Z"),
				Map.of("owner", "sre")
		);
		HumanExecutionResultRecord newer = record(
				"result-2",
				"plan-1",
				"rec-1",
				"incident-1",
				Instant.parse("2026-05-09T01:00:00Z"),
				Map.of("secretToken", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findById("result-1").block())
				.isNotNull()
				.extracting(HumanExecutionResultRecord::executionResultId)
				.isEqualTo("result-1");
		assertThat(store.findByExecutionPlanId("plan-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByRecommendationRecordId("rec-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(HumanExecutionResultRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("secretToken");
	}

	private HumanExecutionResultRecord record(
			String resultId,
			String executionPlanId,
			String recommendationRecordId,
			String incidentId,
			Instant recordedAt,
			Map<String, String> metadata
	) {
		return new HumanExecutionResultRecord(
				resultId,
				executionPlanId,
				recommendationRecordId,
				incidentId,
				HumanExecutionStatus.EXECUTED,
				"operator-a",
				"manual action completed",
				recordedAt.minusSeconds(60),
				recordedAt.minusSeconds(10),
				recordedAt,
				metadata
		);
	}

	@SuppressWarnings("unchecked")
	private HumanExecutionResultR2dbcRepository repository(
			Map<String, HumanExecutionResultEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				HumanExecutionResultEntity entity =
						(HumanExecutionResultEntity) args[0];
				entities.put(entity.getExecutionResultId(), entity);
				yield Mono.just(entity);
			}
			case "findById" -> Mono.justOrEmpty(entities.get(args[0]));
			case "findByExecutionPlanIdOrderByRecordedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getExecutionPlanId()))
							.sorted(Comparator.comparing(
									HumanExecutionResultEntity::getRecordedAt
							).reversed())
			);
			case "findByRecommendationRecordIdOrderByRecordedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getRecommendationRecordId()))
							.sorted(Comparator.comparing(
									HumanExecutionResultEntity::getRecordedAt
							).reversed())
			);
			case "findByIncidentIdOrderByRecordedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									HumanExecutionResultEntity::getRecordedAt
							).reversed())
			);
			case "findTop500ByOrderByRecordedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									HumanExecutionResultEntity::getRecordedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (HumanExecutionResultR2dbcRepository) Proxy.newProxyInstance(
				HumanExecutionResultR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { HumanExecutionResultR2dbcRepository.class },
				handler
		);
	}
}
