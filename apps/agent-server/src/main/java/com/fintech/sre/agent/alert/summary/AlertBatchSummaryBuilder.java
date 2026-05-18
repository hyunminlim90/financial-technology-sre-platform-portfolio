package com.fintech.sre.agent.alert.summary;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.alert.AlertEvent;

@Component
public class AlertBatchSummaryBuilder {

	public AlertBatchSummary build(
			List<AlertEvent> alerts,
			int generatedRecommendations,
			int suppressedDuplicates,
			int rateLimitedAlerts
	) {
		List<AlertEvent> safeAlerts = alerts == null ? List.of() : alerts;

		return new AlertBatchSummary(
				safeAlerts.size(),
				generatedRecommendations,
				suppressedDuplicates,
				rateLimitedAlerts,
				countBy(safeAlerts, alert -> alert.severity() == null ? "UNKNOWN" : alert.severity().name()),
				countBy(safeAlerts, AlertEvent::service),
				countBy(safeAlerts, AlertEvent::domain),
				countBy(safeAlerts, AlertEvent::status)
		);
	}

	private Map<String, Long> countBy(
			List<AlertEvent> alerts,
			Function<AlertEvent, String> classifier
	) {
		return alerts.stream()
				.map(classifier)
				.map(this::normalize)
				.collect(Collectors.groupingBy(
						Function.identity(),
						java.util.LinkedHashMap::new,
						Collectors.counting()
				));
	}

	private String normalize(String value) {
		return value == null || value.isBlank()
				? "unknown"
				: value.trim();
	}
}
