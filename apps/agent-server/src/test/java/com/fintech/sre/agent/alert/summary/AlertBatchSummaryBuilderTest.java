package com.fintech.sre.agent.alert.summary;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.alert.AlertSeverity;
import com.fintech.sre.agent.alert.AlertSource;

class AlertBatchSummaryBuilderTest {

	private final AlertBatchSummaryBuilder builder = new AlertBatchSummaryBuilder();

	@Test
	void shouldBuildBatchSummary() {
		List<AlertEvent> alerts = List.of(
				alert("HighP99Latency", "payment-api", "payment", AlertSeverity.CRITICAL, "firing"),
				alert("HighErrorRate", "payment-api", "payment", AlertSeverity.CRITICAL, "firing"),
				alert("KafkaLagHigh", "settlement-worker", "settlement", AlertSeverity.WARNING, "firing")
		);

		AlertBatchSummary summary = builder.build(alerts, 2, 1, 0);

		assertThat(summary.totalAlerts()).isEqualTo(3);
		assertThat(summary.generatedRecommendations()).isEqualTo(2);
		assertThat(summary.suppressedDuplicates()).isEqualTo(1);
		assertThat(summary.bySeverity()).containsEntry("CRITICAL", 2L);
		assertThat(summary.byService()).containsEntry("payment-api", 2L);
		assertThat(summary.byDomain()).containsEntry("payment", 2L);
	}

	private AlertEvent alert(
			String alertName,
			String service,
			String domain,
			AlertSeverity severity,
			String status
	) {
		return new AlertEvent(
				"alert-" + alertName,
				AlertSource.PROMETHEUS_ALERTMANAGER,
				alertName,
				severity,
				status,
				service,
				domain,
				"sre-agent",
				alertName,
				null,
				null,
				Map.of(),
				Map.of()
		);
	}
}
