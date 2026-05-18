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
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class R2dbcPostmortemReviewStoreTest {

	@Test
	void shouldSaveAndQueryPostmortemReviewsThroughReactiveBoundary() {
		Map<String, PostmortemReviewEntity> entities =
				new ConcurrentHashMap<>();
		PostmortemReviewR2dbcRepository repository =
				repository(entities);
		PostmortemReviewEntityMapper mapper =
				new PostmortemReviewEntityMapper(new ObjectMapper());
		R2dbcPostmortemReviewStore store =
				new R2dbcPostmortemReviewStore(repository, mapper);

		PostmortemReviewRecord older = record(
				"review-1",
				"draft-1",
				"incident-1",
				Instant.parse("2026-05-09T04:00:00Z"),
				Map.of("owner", "sre")
		);
		PostmortemReviewRecord newer = record(
				"review-2",
				"draft-1",
				"incident-1",
				Instant.parse("2026-05-09T05:00:00Z"),
				Map.of("paymentPayload", "must-not-store")
		);

		store.save(older).block();
		store.save(newer).block();

		assertThat(store.findLatestByDraftId("draft-1").block())
				.isNotNull()
				.extracting(PostmortemReviewRecord::postmortemReviewId)
				.isEqualTo("review-2");
		assertThat(store.findByDraftId("draft-1").collectList().block())
				.hasSize(2);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
		assertThat(store.findRecent(1).collectList().block())
				.hasSize(1)
				.first()
				.extracting(PostmortemReviewRecord::metadata)
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.doesNotContainKey("paymentPayload");
	}

	private PostmortemReviewRecord record(
			String reviewId,
			String draftId,
			String incidentId,
			Instant reviewedAt,
			Map<String, String> metadata
	) {
		return new PostmortemReviewRecord(
				reviewId,
				draftId,
				incidentId,
				PostmortemReviewStatus.NEEDS_REVISION,
				"reviewer-a",
				"More evidence is needed.",
				"This draft does not yet establish sufficient causal support.",
				reviewedAt,
				metadata
		);
	}

	@SuppressWarnings("unchecked")
	private PostmortemReviewR2dbcRepository repository(
			Map<String, PostmortemReviewEntity> entities
	) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "save" -> {
				PostmortemReviewEntity entity = (PostmortemReviewEntity) args[0];
				entities.put(entity.getPostmortemReviewId(), entity);
				yield Mono.just(entity);
			}
			case "findByPostmortemDraftIdOrderByReviewedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getPostmortemDraftId()))
							.sorted(Comparator.comparing(
									PostmortemReviewEntity::getReviewedAt
							).reversed())
			);
			case "findFirstByPostmortemDraftIdOrderByReviewedAtDesc" -> Mono.justOrEmpty(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getPostmortemDraftId()))
							.max(Comparator.comparing(PostmortemReviewEntity::getReviewedAt))
			);
			case "findByIncidentIdOrderByReviewedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.filter(entity -> args[0] != null
									&& args[0].equals(entity.getIncidentId()))
							.sorted(Comparator.comparing(
									PostmortemReviewEntity::getReviewedAt
							).reversed())
			);
			case "findTop500ByOrderByReviewedAtDesc" -> Flux.fromStream(
					entities.values().stream()
							.sorted(Comparator.comparing(
									PostmortemReviewEntity::getReviewedAt
							).reversed())
							.limit(500)
			);
			default -> throw new UnsupportedOperationException(
					"Unsupported repository method in test: " + method.getName()
			);
		};

		return (PostmortemReviewR2dbcRepository) Proxy.newProxyInstance(
				PostmortemReviewR2dbcRepository.class.getClassLoader(),
				new Class<?>[] { PostmortemReviewR2dbcRepository.class },
				handler
		);
	}
}
