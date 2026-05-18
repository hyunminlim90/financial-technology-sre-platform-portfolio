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
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcPostmortemDraftStoreTest {

	@Test
	void shouldSaveAndQueryPostmortemDraftsThroughReactiveBoundary() {
		Map<String, PostmortemDraftEntity> entities =
				new ConcurrentHashMap<>();
		PostmortemDraftR2dbcRepository repository =
				repository(entities);
		PostmortemDraftEntityMapper mapper =
				new PostmortemDraftEntityMapper(new ObjectMapper());
		R2dbcPostmortemDraftStore store =
				new R2dbcPostmortemDraftStore(repository, mapper);

		PostmortemDraftRecord older = record(
				"draft-1",
				"incident-1",
				Instant.parse("2026-05-09T02:00:00Z"),
				Map.of("owner", "sre")
		);
		PostmortemDraftRecord newer = record(
				"draft-2",
				"incident-1",
				Instant.parse("2026-05-09T03:00:00Z"),
				Map.of("paymentPayload", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findById("draft-1").block())
				.isNotNull()
				.extracting(PostmortemDraftRecord::postmortemDraftId)
				.isEqualTo("draft-1");
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(PostmortemDraftRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("paymentPayload");
	}

	private PostmortemDraftRecord record(
			String draftId,
			String incidentId,
			Instant createdAt,
			Map<String, String> metadata
	) {
		return new PostmortemDraftRecord(
				draftId,
				incidentId,
				PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED,
				"operator-a",
				"This draft does not assert root cause certainty.",
				List.of("00:00 alert"),
				List.of("review rollback path"),
				List.of("manual restart completed"),
				List.of("verification passed"),
				List.of("confirm remaining unknowns"),
				List.of("update runbook"),
				List.of("What evidence is missing?"),
				createdAt,
				metadata
		);
	}

	@SuppressWarnings("unchecked")
	private PostmortemDraftR2dbcRepository repository(
			Map<String, PostmortemDraftEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				PostmortemDraftEntity entity = (PostmortemDraftEntity) args[0];
				entities.put(entity.getPostmortemDraftId(), entity);
				yield Mono.just(entity);
			}
			case "findById" -> Mono.justOrEmpty(entities.get(args[0]));
			case "findByIncidentIdOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									PostmortemDraftEntity::getCreatedAt
							).reversed())
			);
			case "findTop500ByOrderByCreatedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									PostmortemDraftEntity::getCreatedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (PostmortemDraftR2dbcRepository) Proxy.newProxyInstance(
				PostmortemDraftR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { PostmortemDraftR2dbcRepository.class },
				handler
		);
	}
}
