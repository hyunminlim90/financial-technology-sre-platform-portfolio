package com.fintech.sre.agent.alert.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.alert.AlertSeverity;

class PrometheusAlertEventNormalizerTest {

	private final PrometheusAlertEventNormalizer normalizer =
			new PrometheusAlertEventNormalizer();

	@Test
	void shouldNormalizePrometheusAlert() {
		PrometheusAlertWebhookRequest request = new PrometheusAlertWebhookRequest(
				"default",
				"firing",
				List.of(new PrometheusAlertWebhookRequest.PrometheusAlert(
						"firing",
						Map.of(
								"alertname", "HighP99Latency",
								"service", "payment-api",
								"domain", "payment",
								"namespace", "sre-agent",
								"severity", "critical"
						),
						Map.of("description", "p99 latency is high"),
						"2026-05-07T00:00:00Z",
						null,
						""
				)),
				Map.of(),
				Map.of(),
				Map.of(),
				""
		);

		List<AlertEvent> events = normalizer.normalize(request);

		assertThat(events).hasSize(1);
		assertThat(events.get(0).alertName()).isEqualTo("HighP99Latency");
		assertThat(events.get(0).service()).isEqualTo("payment-api");
		assertThat(events.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
	}
}
