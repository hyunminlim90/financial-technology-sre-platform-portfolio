package com.fintech.sre.agent.alert.evidence;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class AlertEvidenceCodeResolver {

	public String resolve(String alertName) {
		if (alertName == null || alertName.isBlank()) {
			return "UNKNOWN_ALERT";
		}

		String normalized = alertName.toLowerCase(Locale.ROOT);

		if (containsAny(normalized, "latency", "p99", "p95", "response_time")) {
			return "LATENCY_SPIKE";
		}

		if (containsAny(normalized, "error", "5xx", "failure", "fail")) {
			return "ERROR_RATE_SPIKE";
		}

		if (containsAny(normalized, "consumerlag", "consumer_lag", "kafka_lag", "lag")) {
			return "CONSUMER_LAG_SPIKE";
		}

		if (containsAny(normalized, "cpu")) {
			return "CPU_SATURATION";
		}

		if (containsAny(normalized, "memory", "oom")) {
			return "MEMORY_PRESSURE";
		}

		if (containsAny(normalized, "podrestart", "restart", "crashloop")) {
			return "POD_RESTART_SPIKE";
		}

		if (containsAny(normalized, "paymentduplicate", "duplicate_payment", "duplicate")) {
			return "DUPLICATE_PAYMENT_RISK";
		}

		return "ALERT_" + alertName
				.replaceAll("[^A-Za-z0-9]+", "_")
				.replaceAll("_+", "_")
				.replaceAll("^_|_$", "")
				.toUpperCase(Locale.ROOT);
	}

	private boolean containsAny(String value, String... keywords) {
		for (String keyword : keywords) {
			if (value.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}
