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
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcRecommendationApprovalStoreTest {

	@Test
	void shouldSaveAndQueryApprovalRecordsThroughReactiveBoundary() {
		Map<String, RecommendationApprovalRecordEntity> entities =
				new ConcurrentHashMap<>();
		RecommendationApprovalRecordR2dbcRepository repository =
				repository(entities);
		RecommendationApprovalRecordEntityMapper mapper =
				new RecommendationApprovalRecordEntityMapper(new ObjectMapper());
		R2dbcRecommendationApprovalStore store =
				new R2dbcRecommendationApprovalStore(repository, mapper);

		RecommendationApprovalRecord older = new RecommendationApprovalRecord(
				"approval-1",
				"rec-1",
				"incident-1",
				RecommendationApprovalStatus.PENDING,
				"operator-a",
				"pending",
				Instant.parse("2026-05-09T00:00:00Z"),
				Map.of("owner", "sre")
		);
		RecommendationApprovalRecord newer = new RecommendationApprovalRecord(
				"approval-2",
				"rec-1",
				"incident-1",
				RecommendationApprovalStatus.APPROVED,
				"operator-b",
				"approved",
				Instant.parse("2026-05-09T01:00:00Z"),
				Map.of("secretToken", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findLatestByRecommendationRecordId("rec-1").block())
				.isNotNull()
				.extracting(RecommendationApprovalRecord::approvalId)
				.isEqualTo("approval-2");
		assertThat(store.findByRecommendationRecordId("rec-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(RecommendationApprovalRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("secretToken");
	}

	@SuppressWarnings("unchecked")
	private RecommendationApprovalRecordR2dbcRepository repository(
			Map<String, RecommendationApprovalRecordEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				RecommendationApprovalRecordEntity entity =
						(RecommendationApprovalRecordEntity) args[0];
				entities.put(entity.getApprovalId(), entity);
				yield Mono.just(entity);
			}
			case "findByIncidentIdOrderByDecidedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									RecommendationApprovalRecordEntity::getDecidedAt
							).reversed())
			);
			case "findByRecommendationRecordIdOrderByDecidedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getRecommendationRecordId()))
							.sorted(Comparator.comparing(
									RecommendationApprovalRecordEntity::getDecidedAt
							).reversed())
			);
			case "findFirstByRecommendationRecordIdOrderByDecidedAtDesc" -> Mono.justOrEmpty(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getRecommendationRecordId()))
							.max(Comparator.comparing(
									RecommendationApprovalRecordEntity::getDecidedAt
							))
			);
			case "findTop500ByOrderByDecidedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									RecommendationApprovalRecordEntity::getDecidedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (RecommendationApprovalRecordR2dbcRepository) Proxy.newProxyInstance(
				RecommendationApprovalRecordR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { RecommendationApprovalRecordR2dbcRepository.class },
				handler
		);
	}
}
