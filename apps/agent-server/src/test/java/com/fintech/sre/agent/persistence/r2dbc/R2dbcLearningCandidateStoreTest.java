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
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcLearningCandidateStoreTest {

	@Test
	void shouldSaveAndQueryLearningCandidatesThroughReactiveBoundary() {
		Map<String, LearningCandidateEntity> entities =
				new ConcurrentHashMap<>();
		LearningCandidateR2dbcRepository repository = repository(entities);
		LearningCandidateEntityMapper mapper =
				new LearningCandidateEntityMapper(new ObjectMapper());
		R2dbcLearningCandidateStore store =
				new R2dbcLearningCandidateStore(repository, mapper);

		LearningCandidateRecord older = record(
				"candidate-1",
				"incident-1",
				Instant.parse("2026-05-09T06:00:00Z"),
				Map.of("owner", "sre")
		);
		LearningCandidateRecord newer = record(
				"candidate-2",
				"incident-1",
				Instant.parse("2026-05-09T07:00:00Z"),
				Map.of("promptPayload", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findById("candidate-1").block())
				.isNotNull()
				.extracting(LearningCandidateRecord::learningCandidateId)
				.isEqualTo("candidate-1");
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(LearningCandidateRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("promptPayload");
	}

	private LearningCandidateRecord record(
			String learningCandidateId,
			String incidentId,
			Instant createdAt,
			Map<String, String> metadata
	) {
		return new LearningCandidateRecord(
				learningCandidateId,
				incidentId,
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.REVIEW_REQUIRED,
				"operator-a",
				"Promote a runbook update candidate.",
				List.of("Add rollback verification step."),
				createdAt,
				metadata
		);
	}

	@SuppressWarnings("unchecked")
	private LearningCandidateR2dbcRepository repository(
			Map<String, LearningCandidateEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				LearningCandidateEntity entity = (LearningCandidateEntity) args[0];
				entities.put(entity.getLearningCandidateId(), entity);
				yield Mono.just(entity);
			}
			case "findById" -> Mono.justOrEmpty(entities.get(args[0]));
			case "findByIncidentIdOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									LearningCandidateEntity::getCreatedAt
							).reversed())
			);
			case "findTop500ByOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									LearningCandidateEntity::getCreatedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (LearningCandidateR2dbcRepository) Proxy.newProxyInstance(
				LearningCandidateR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { LearningCandidateR2dbcRepository.class },
				handler
		);
	}
}
