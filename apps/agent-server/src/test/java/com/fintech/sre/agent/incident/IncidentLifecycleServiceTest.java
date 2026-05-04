package com.fintech.sre.agent.incident;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

class IncidentLifecycleServiceTest {

	@Test
	void shouldRejectInvalidTransition() {
		IncidentLifecycleService service = new IncidentLifecycleService(new InMemoryIncidentLifecycleRepository());

		StepVerifier.create(service.transition("INC-LIFECYCLE-1", IncidentStatus.ACTION_APPROVED, "skip ahead"))
				.expectErrorSatisfies(error -> assertThat(error)
						.isInstanceOf(IllegalStateException.class)
						.hasMessageContaining("Invalid incident transition"))
				.verify();
	}

	@Test
	void shouldTrackValidLifecycleTransitions() {
		IncidentLifecycleService service = new IncidentLifecycleService(new InMemoryIncidentLifecycleRepository());

		StepVerifier.create(service.createIfAbsent("INC-LIFECYCLE-2")
						.flatMap(ignored -> service.transition("INC-LIFECYCLE-2", IncidentStatus.RECOMMENDATION_CREATED, "recommendation created"))
						.flatMap(ignored -> service.transition("INC-LIFECYCLE-2", IncidentStatus.HUMAN_REVIEW_REQUIRED, "review required")))
				.expectNextMatches(lifecycle ->
						lifecycle.status() == IncidentStatus.HUMAN_REVIEW_REQUIRED
								&& lifecycle.history().size() == 3
				)
				.verifyComplete();
	}
}
