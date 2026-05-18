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
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcKnowledgeUpdateApplicationStoreTest {

	@Test
	void shouldSaveAndQueryKnowledgeUpdateApplicationsThroughReactiveBoundary() {
		Map<String, KnowledgeUpdateApplicationEntity> entities =
				new ConcurrentHashMap<>();
		KnowledgeUpdateApplicationR2dbcRepository repository =
				repository(entities);
		KnowledgeUpdateApplicationEntityMapper mapper =
				new KnowledgeUpdateApplicationEntityMapper(new ObjectMapper());
		R2dbcKnowledgeUpdateApplicationStore store =
				new R2dbcKnowledgeUpdateApplicationStore(repository, mapper);

		KnowledgeUpdateApplicationRecord older = record(
				"knowledge-update-1",
				"incident-1",
				"candidate-1",
				Instant.parse("2026-05-09T12:00:00Z"),
				Map.of("owner", "sre")
		);
		KnowledgeUpdateApplicationRecord newer = record(
				"knowledge-update-2",
				"incident-1",
				"candidate-1",
				Instant.parse("2026-05-09T13:00:00Z"),
				Map.of("paymentPayload", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findById("knowledge-update-1").block())
				.isNotNull()
				.extracting(KnowledgeUpdateApplicationRecord::knowledgeUpdateApplicationId)
				.isEqualTo("knowledge-update-1");
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByLearningCandidateId("candidate-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(KnowledgeUpdateApplicationRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("paymentPayload");
	}

	private KnowledgeUpdateApplicationRecord record(
			String applicationId,
			String incidentId,
			String learningCandidateId,
			Instant appliedAt,
			Map<String, String> metadata
	) {
		return new KnowledgeUpdateApplicationRecord(
				applicationId,
				incidentId,
				learningCandidateId,
				"promotion-plan-1",
				"RUNBOOK",
				KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
				"runbooks/payments/payment-api-runbook.md",
				KnowledgeUpdateChangeType.UPDATED,
				"portfolio-repo",
				"main",
				"a1b2c3d4",
				"PR-100",
				"operator-a",
				"reviewer-a",
				"approver-a",
				List.of("rollback verification completed"),
				appliedAt,
				metadata
		);
	}

	@SuppressWarnings("unchecked")
	private KnowledgeUpdateApplicationR2dbcRepository repository(
			Map<String, KnowledgeUpdateApplicationEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				KnowledgeUpdateApplicationEntity entity =
						(KnowledgeUpdateApplicationEntity) args[0];
				entities.put(entity.getKnowledgeUpdateApplicationId(), entity);
				yield Mono.just(entity);
			}
			case "findById" -> Mono.justOrEmpty(entities.get(args[0]));
			case "findByIncidentIdOrderByAppliedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									KnowledgeUpdateApplicationEntity::getAppliedAt
							).reversed())
			);
			case "findByLearningCandidateIdOrderByAppliedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getLearningCandidateId()))
							.sorted(Comparator.comparing(
									KnowledgeUpdateApplicationEntity::getAppliedAt
							).reversed())
			);
			case "findTop500ByOrderByAppliedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									KnowledgeUpdateApplicationEntity::getAppliedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (KnowledgeUpdateApplicationR2dbcRepository) Proxy.newProxyInstance(
				KnowledgeUpdateApplicationR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { KnowledgeUpdateApplicationR2dbcRepository.class },
				handler
		);
	}
}
