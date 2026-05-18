package com.fintech.sre.agent.alert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.alert.evidence.AlertEvidenceCodeResolver;
import com.fintech.sre.agent.alert.evidence.AlertEvidenceMapper;
import com.fintech.sre.agent.alert.evidence.AlertEvidenceSeverityMapper;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

class AlertToIncidentRequestMapperTest {

	private final AlertToIncidentRequestMapper mapper =
			new AlertToIncidentRequestMapper(new AlertEvidenceMapper(
					new AlertEvidenceCodeResolver(),
					new AlertEvidenceSeverityMapper()
			));

	@Test
	void shouldMapAlertToIncidentRequest() {
		AlertEvent alert = new AlertEvent(
				"alert-1",
				AlertSource.PROMETHEUS_ALERTMANAGER,
				"HighP99Latency",
				AlertSeverity.CRITICAL,
				"firing",
				"payment-api",
				"payment",
				"sre-agent",
				"p99 latency is high",
				null,
				null,
				Map.of("severity", "critical"),
				Map.of()
		);

		IncidentRecommendationRequest request = mapper.toRequest(alert);

		assertThat(request.incidentId()).isEqualTo("alert-1");
		assertThat(request.service()).isEqualTo("payment-api");
		assertThat(request.labels()).containsEntry("alertName", "HighP99Latency");
		assertThat(request.labels()).containsEntry("evidenceCode", "LATENCY_SPIKE");
	}
}
