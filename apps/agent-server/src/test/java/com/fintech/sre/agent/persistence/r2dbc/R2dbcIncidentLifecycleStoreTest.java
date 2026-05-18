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
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcIncidentLifecycleStoreTest {

	@Test
	void shouldSaveAndQueryIncidentLifecyclesThroughReactiveBoundary() {
		Map<String, IncidentLifecycleEntity> entities =
				new ConcurrentHashMap<>();
		IncidentLifecycleR2dbcRepository repository =
				repository(entities);
		IncidentLifecycleEntityMapper mapper =
				new IncidentLifecycleEntityMapper(new ObjectMapper());
		R2dbcIncidentLifecycleStore store =
				new R2dbcIncidentLifecycleStore(repository, mapper);

		IncidentLifecycleRecord older = record(
				"lifecycle-1",
				"incident-1",
				Instant.parse("2026-05-09T00:30:00Z"),
				Map.of("owner", "sre")
		);
		IncidentLifecycleRecord newer = record(
				"lifecycle-2",
				"incident-1",
				Instant.parse("2026-05-09T01:30:00Z"),
				Map.of("secretToken", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findLatestByIncidentId("incident-1").block())
				.isNotNull()
				.extracting(IncidentLifecycleRecord::incidentLifecycleId)
				.isEqualTo("lifecycle-2");
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(IncidentLifecycleRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("secretToken");
	}

	private IncidentLifecycleRecord record(
			String lifecycleId,
			String incidentId,
			Instant transitionedAt,
			Map<String, String> metadata
	) {
		return new IncidentLifecycleRecord(
				lifecycleId,
				incidentId,
				IncidentStatus.MITIGATING,
				IncidentStatus.STABILIZING,
				IncidentTransitionReason.STABILIZATION_WINDOW_STARTED,
				"operator-a",
				"manual stabilization confirmed",
				transitionedAt,
				metadata
		);
	}

	@SuppressWarnings("unchecked")
	private IncidentLifecycleR2dbcRepository repository(
			Map<String, IncidentLifecycleEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				IncidentLifecycleEntity entity = (IncidentLifecycleEntity) args[0];
				entities.put(entity.getIncidentLifecycleId(), entity);
				yield Mono.just(entity);
			}
			case "findFirstByIncidentIdOrderByTransitionedAtDesc" -> Mono.justOrEmpty(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.max(Comparator.comparing(IncidentLifecycleEntity::getTransitionedAt))
			);
			case "findByIncidentIdOrderByTransitionedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									IncidentLifecycleEntity::getTransitionedAt
							).reversed())
			);
			case "findTop500ByOrderByTransitionedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									IncidentLifecycleEntity::getTransitionedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (IncidentLifecycleR2dbcRepository) Proxy.newProxyInstance(
				IncidentLifecycleR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { IncidentLifecycleR2dbcRepository.class },
				handler
		);
	}
}
