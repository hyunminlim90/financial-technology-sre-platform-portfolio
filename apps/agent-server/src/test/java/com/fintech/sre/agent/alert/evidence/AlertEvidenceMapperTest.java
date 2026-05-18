package com.fintech.sre.agent.alert.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.alert.AlertSeverity;
import com.fintech.sre.agent.alert.AlertSource;
import com.fintech.sre.agent.evidence.EvidenceSeverity;
import com.fintech.sre.agent.evidence.EvidenceSignal;
import com.fintech.sre.agent.evidence.EvidenceSource;

class AlertEvidenceMapperTest {

	private final AlertEvidenceMapper mapper = new AlertEvidenceMapper(
			new AlertEvidenceCodeResolver(),
			new AlertEvidenceSeverityMapper()
	);

	@Test
	void shouldMapAlertToEvidenceSignal() {
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
				Map.of("value", "p99=1800ms")
		);

		EvidenceSignal signal = mapper.toSignal(alert);

		assertThat(signal.code()).isEqualTo("LATENCY_SPIKE");
		assertThat(signal.severity()).isEqualTo(EvidenceSeverity.CRITICAL);
		assertThat(signal.source()).isEqualTo(EvidenceSource.PROMETHEUS);
	}
}
