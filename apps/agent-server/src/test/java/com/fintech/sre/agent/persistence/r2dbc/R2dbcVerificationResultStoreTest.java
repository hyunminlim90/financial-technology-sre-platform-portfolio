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
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcVerificationResultStoreTest {

	@Test
	void shouldSaveAndQueryVerificationResultsThroughReactiveBoundary() {
		Map<String, VerificationResultEntity> entities =
				new ConcurrentHashMap<>();
		VerificationResultR2dbcRepository repository =
				repository(entities);
		VerificationResultEntityMapper mapper =
				new VerificationResultEntityMapper(new ObjectMapper());
		R2dbcVerificationResultStore store =
				new R2dbcVerificationResultStore(repository, mapper);

		VerificationResultRecord older = record(
				"verification-1",
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				Instant.parse("2026-05-09T00:30:00Z"),
				Map.of("owner", "sre")
		);
		VerificationResultRecord newer = record(
				"verification-2",
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				Instant.parse("2026-05-09T01:30:00Z"),
				Map.of("paymentPayload", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findById("verification-1").block())
				.isNotNull()
				.extracting(VerificationResultRecord::verificationResultId)
				.isEqualTo("verification-1");
		assertThat(store.findByExecutionResultId("result-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByRecommendationRecordId("rec-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(VerificationResultRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("paymentPayload");
	}

	private VerificationResultRecord record(
			String verificationResultId,
			String executionResultId,
			String executionPlanId,
			String recommendationRecordId,
			String incidentId,
			Instant verifiedAt,
			Map<String, String> metadata
	) {
		return new VerificationResultRecord(
				verificationResultId,
				executionResultId,
				executionPlanId,
				recommendationRecordId,
				incidentId,
				VerificationStatus.VERIFIED,
				"operator-a",
				"manual verification completed",
				verifiedAt,
				metadata
		);
	}

	@SuppressWarnings("unchecked")
	private VerificationResultR2dbcRepository repository(
			Map<String, VerificationResultEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				VerificationResultEntity entity = (VerificationResultEntity) args[0];
				entities.put(entity.getVerificationResultId(), entity);
				yield Mono.just(entity);
			}
			case "findById" -> Mono.justOrEmpty(entities.get(args[0]));
			case "findByExecutionResultIdOrderByVerifiedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getExecutionResultId()))
							.sorted(Comparator.comparing(
									VerificationResultEntity::getVerifiedAt
							).reversed())
			);
			case "findByRecommendationRecordIdOrderByVerifiedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getRecommendationRecordId()))
							.sorted(Comparator.comparing(
									VerificationResultEntity::getVerifiedAt
							).reversed())
			);
			case "findByIncidentIdOrderByVerifiedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									VerificationResultEntity::getVerifiedAt
							).reversed())
			);
			case "findTop500ByOrderByVerifiedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									VerificationResultEntity::getVerifiedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (VerificationResultR2dbcRepository) Proxy.newProxyInstance(
				VerificationResultR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { VerificationResultR2dbcRepository.class },
				handler
		);
	}
}
