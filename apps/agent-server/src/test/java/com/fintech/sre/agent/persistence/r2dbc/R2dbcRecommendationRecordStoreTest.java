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
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcRecommendationRecordStoreTest {

	@Test
	void shouldSaveAndQueryThroughReactiveRepositoryBoundary() {
		Map<String, RecommendationRecordEntity> entities =
				new ConcurrentHashMap<>();
		RecommendationRecordR2dbcRepository repository = repository(entities);
		RecommendationRecordEntityMapper mapper =
				new RecommendationRecordEntityMapper(new ObjectMapper());
		R2dbcRecommendationRecordStore store =
				new R2dbcRecommendationRecordStore(repository, mapper);

		RecommendationRecord record = new RecommendationRecord(
				"rec-1",
				"incident-1",
				"audit-1",
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				Instant.parse("2026-05-09T00:00:00Z"),
				1,
				0,
				"ALLOW",
				"PASS",
				java.util.List.of("RATE_LIMIT"),
				java.util.List.of(),
				Map.of("owner", "sre", "secretToken", "must-not-store")
		);

		store.save(record).block();

		assertThat(store.findById("rec-1").block())
				.isNotNull()
				.extracting(RecommendationRecord::recommendationRecordId)
				.isEqualTo("rec-1");
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(1);
		assertThat(store.findRecent(10).collectList().block())
				.hasSize(1)
				.first()
				.extracting(RecommendationRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("secretToken");
	}

	@SuppressWarnings("unchecked")
	private RecommendationRecordR2dbcRepository repository(
			Map<String, RecommendationRecordEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				RecommendationRecordEntity entity =
						(RecommendationRecordEntity) args[0];
				entities.put(entity.getRecommendationRecordId(), entity);
				yield Mono.just(entity);
			}
			case "findById" -> Mono.justOrEmpty(entities.get(args[0]));
			case "findByIncidentIdOrderByGeneratedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									RecommendationRecordEntity::getGeneratedAt
							).reversed())
			);
			case "findRecent" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									RecommendationRecordEntity::getGeneratedAt
							).reversed())
							.limit((Integer) args[0])
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (RecommendationRecordR2dbcRepository) Proxy.newProxyInstance(
				RecommendationRecordR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { RecommendationRecordR2dbcRepository.class },
				handler
		);
	}
}
