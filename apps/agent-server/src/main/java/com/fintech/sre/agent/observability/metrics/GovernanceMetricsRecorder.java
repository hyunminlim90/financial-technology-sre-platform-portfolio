package com.fintech.sre.agent.observability.metrics;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceMetricsRecorder {

	private final MeterRegistry meterRegistry;

	public GovernanceMetricsRecorder(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void increment(String metricName, Map<String, String> tags) {
		Counter.Builder builder = Counter.builder(metricName);
		safeTags(tags).forEach(builder::tag);
		builder.register(meterRegistry).increment();
	}

	private Map<String, String> safeTags(Map<String, String> tags) {
		if (tags == null || tags.isEmpty()) {
			return Map.of();
		}

		return tags.entrySet().stream()
				.filter(entry -> entry.getKey() != null)
				.filter(entry -> entry.getValue() != null)
				.filter(entry -> allowedTag(entry.getKey()))
				.collect(Collectors.toUnmodifiableMap(
						Map.Entry::getKey,
						entry -> normalizeTagValue(entry.getValue())
				));
	}

	private boolean allowedTag(String key) {
		String lower = key.toLowerCase();

		return !lower.contains("payload")
				&& !lower.contains("customer")
				&& !lower.contains("secret")
				&& !lower.contains("token")
				&& !lower.contains("password")
				&& !lower.contains("prompt")
				&& !lower.contains("rawlog")
				&& !lower.contains("paymentpayload");
	}

	private String normalizeTagValue(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}

		if (value.length() > 80) {
			return value.substring(0, 80);
		}

		return value;
	}
}
