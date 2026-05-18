package com.fintech.sre.agent.alert.webhook;

import java.util.List;
import java.util.Map;

public record PrometheusAlertWebhookRequest(
		String receiver,
		String status,
		List<PrometheusAlert> alerts,
		Map<String, String> groupLabels,
		Map<String, String> commonLabels,
		Map<String, String> commonAnnotations,
		String externalURL
) {
	public record PrometheusAlert(
			String status,
			Map<String, String> labels,
			Map<String, String> annotations,
			String startsAt,
			String endsAt,
			String generatorURL
	) {
	}
}
