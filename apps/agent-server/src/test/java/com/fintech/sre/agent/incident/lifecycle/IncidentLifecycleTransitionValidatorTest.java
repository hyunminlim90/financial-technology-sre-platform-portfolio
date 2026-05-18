package com.fintech.sre.agent.incident.lifecycle;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IncidentLifecycleTransitionValidatorTest {

	@Test
	void shouldAllowOpenToMitigating() {
		IncidentLifecycleTransitionValidator validator =
				new IncidentLifecycleTransitionValidator();

		validator.validate(
				IncidentStatus.OPEN,
				IncidentStatus.MITIGATING
		);
	}

	@Test
	void shouldRejectResolvedToOpen() {
		IncidentLifecycleTransitionValidator validator =
				new IncidentLifecycleTransitionValidator();

		assertThatThrownBy(() ->
				validator.validate(
						IncidentStatus.RESOLVED,
						IncidentStatus.OPEN
				)
		).isInstanceOf(IncidentLifecycleRejectedException.class);
	}
}
