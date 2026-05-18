package com.fintech.sre.agent.incident.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;

class IncidentLifecycleServiceTest {

	@Test
	void shouldTrackValidLifecycleTransitions() {
		IncidentLifecycleService service = new IncidentLifecycleService(
				new InMemoryIncidentLifecycleStore(),
				new IncidentLifecycleIdGenerator(),
				new IncidentLifecycleTransitionValidator(),
				MetricsRecorderTestSupport.incidentLifecycleMetricsRecorder()
		);

		service.transition(
				"incident-1",
				request(
						IncidentStatus.MITIGATING,
						IncidentTransitionReason.HUMAN_INVESTIGATION_STARTED,
						"Started investigation."
				)
		).block();

		service.transition(
				"incident-1",
				request(
						IncidentStatus.STABILIZING,
						IncidentTransitionReason.MITIGATION_IN_PROGRESS,
						"Mitigation applied."
				)
		).block();

		IncidentLifecycleTransitionResponse response = service.transition(
				"incident-1",
				request(
						IncidentStatus.RESOLVED,
						IncidentTransitionReason.INCIDENT_RESOLVED,
						"Stabilization window passed."
				)
		).block();

		assertThat(response.currentStatus())
				.isEqualTo(IncidentStatus.RESOLVED);
	}

	@Test
	void shouldSanitizeMetadata() {
		InMemoryIncidentLifecycleStore store = new InMemoryIncidentLifecycleStore();
		IncidentLifecycleService service = new IncidentLifecycleService(
				store,
				new IncidentLifecycleIdGenerator(),
				new IncidentLifecycleTransitionValidator(),
				MetricsRecorderTestSupport.incidentLifecycleMetricsRecorder()
		);

		service.transition(
				"incident-2",
				new IncidentLifecycleTransitionRequest(
						IncidentStatus.MITIGATING,
						IncidentTransitionReason.HUMAN_INVESTIGATION_STARTED,
						"operator-a",
						"Started investigation.",
						Map.of(
								"team", "sre",
								"secret", "must-not-store"
						)
				)
		).block();

		IncidentLifecycleRecord latest = store.findLatestByIncidentId("incident-2").block();

		assertThat(latest.metadata())
				.containsKey("team")
				.doesNotContainKey("secret");
	}

	private IncidentLifecycleTransitionRequest request(
			IncidentStatus toStatus,
			IncidentTransitionReason reason,
			String summary
	) {
		return new IncidentLifecycleTransitionRequest(
				toStatus,
				reason,
				"operator-a",
				summary,
				Map.of()
		);
	}
}
