package com.fintech.sre.agent.alert.webhook;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.alert.AlertEvent;
import com.fintech.sre.agent.alert.AlertSeverity;
import com.fintech.sre.agent.alert.AlertSource;

@Component
public class PrometheusAlertEventNormalizer {

	public List<AlertEvent> normalize(PrometheusAlertWebhookRequest request) {
		if (request == null || request.alerts() == null) {
			return List.of();
		}

		return request.alerts().stream()
				.map(alert -> toEvent(request, alert))
				.toList();
	}

	private AlertEvent toEvent(
			PrometheusAlertWebhookRequest request,
			PrometheusAlertWebhookRequest.PrometheusAlert alert
	) {
		Map<String, String> labels = safe(alert.labels());
		Map<String, String> annotations = safe(alert.annotations());

		String alertName = firstNonBlank(
				labels.get("alertname"),
				labels.get("alertName"),
				"unknown-alert"
		);

		String service = firstNonBlank(
				labels.get("service"),
				labels.get("app"),
				labels.get("job"),
				"unknown-service"
		);

		String domain = firstNonBlank(
				labels.get("domain"),
				labels.get("bounded_context"),
				"unknown"
		);

		String namespace = firstNonBlank(
				labels.get("namespace"),
				labels.get("kubernetes_namespace"),
				"unknown"
		);

		return new AlertEvent(
				buildAlertId(alertName, service, alert.startsAt()),
				AlertSource.PROMETHEUS_ALERTMANAGER,
				alertName,
				AlertSeverity.from(labels.get("severity")),
				firstNonBlank(alert.status(), request.status(), "unknown"),
				service,
				domain,
				namespace,
				firstNonBlank(
						annotations.get("description"),
						annotations.get("summary"),
						""
				),
				parseInstant(alert.startsAt()),
				parseInstant(alert.endsAt()),
				labels,
				annotations
		);
	}

	private String buildAlertId(String alertName, String service, String startsAt) {
		return "alert-" + alertName + "-" + service + "-"
				+ (startsAt == null ? UUID.randomUUID() : startsAt.hashCode());
	}

	private Instant parseInstant(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return Instant.parse(value);
		} catch (Exception ex) {
			return null;
		}
	}

	private Map<String, String> safe(Map<String, String> values) {
		return values == null ? Map.of() : Map.copyOf(values);
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return "";
	}
}
