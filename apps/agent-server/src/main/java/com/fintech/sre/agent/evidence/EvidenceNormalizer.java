package com.fintech.sre.agent.evidence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.IncidentContext;

@Component
public class EvidenceNormalizer {

	public EvidenceContext normalize(List<String> rawSignals) {
		if (rawSignals == null || rawSignals.isEmpty()) {
			return new EvidenceContext(
					null,
					null,
					null,
					List.of(),
					java.util.Map.of(),
					EvidenceQueryStatus.SUCCESS,
					EvidenceQueryStatus.SUCCESS,
					EvidenceQueryStatus.SUCCESS
			);
		}

		List<Evidence> evidences = new ArrayList<>();

		for (String signal : rawSignals) {
			switch (signal) {
				case "DB_POOL_SATURATED", "DB_POOL_PENDING_HIGH" -> evidences.add(new Evidence(
						EvidenceLayer.DATABASE,
						EvidenceSignal.DB_POOL_PENDING_HIGH,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.CRITICAL,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"Database connection pool pending is high"
				));
				case "REBALANCE_STORM" -> evidences.add(new Evidence(
						EvidenceLayer.QUEUE,
						EvidenceSignal.KAFKA_REBALANCE_STORM,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.CRITICAL,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"Kafka consumer rebalance storm detected"
				));
				case "RETRY_STORM" -> evidences.add(new Evidence(
						EvidenceLayer.APPLICATION,
						EvidenceSignal.RETRY_STORM,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.CRITICAL,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"Retry storm detected"
				));
				case "RETRY_RATE_HIGH" -> evidences.add(new Evidence(
						EvidenceLayer.APPLICATION,
						EvidenceSignal.RETRY_RATE_HIGH,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.WARNING,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"Retry rate is elevated"
				));
				case "TRAFFIC_SPIKE" -> evidences.add(new Evidence(
						EvidenceLayer.EDGE,
						EvidenceSignal.TRAFFIC_SPIKE,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.WARNING,
						EvidenceConfidence.MEDIUM,
						EvidenceStatus.PRESENT,
						"Traffic spike detected"
				));
				case "KAFKA_CONSUMER_LAG_HIGH" -> evidences.add(new Evidence(
						EvidenceLayer.QUEUE,
						EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH,
						5000,
						1000,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.WARNING,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						"Kafka consumer lag high"
				));
				case "OBSERVABILITY_SOURCE_DOWN" -> evidences.add(new Evidence(
						EvidenceLayer.UNKNOWN,
						EvidenceSignal.OBSERVABILITY_SOURCE_DOWN,
						1,
						1,
						Duration.ofMinutes(1),
						EvidenceSource.UNKNOWN,
						EvidenceSeverity.WARNING,
						EvidenceConfidence.MEDIUM,
						EvidenceStatus.PRESENT,
						"Observability source degraded"
				));
				default -> evidences.add(new Evidence(
						EvidenceLayer.UNKNOWN,
						EvidenceSignal.UNKNOWN,
						0,
						0,
						Duration.ZERO,
						EvidenceSource.UNKNOWN,
						EvidenceSeverity.INFO,
						EvidenceConfidence.LOW,
						EvidenceStatus.UNKNOWN,
						"Unknown raw signal: " + signal
				));
			}
		}

		EvidenceQueryStatus queryStatus = rawSignals.contains("OBSERVABILITY_SOURCE_DOWN")
				? EvidenceQueryStatus.FAILED
				: EvidenceQueryStatus.SUCCESS;

		return new EvidenceContext(
				null,
				null,
				null,
				evidences,
				java.util.Map.of(),
				queryStatus,
				queryStatus,
				queryStatus
		);
	}

	public EvidenceContext normalize(IncidentContext incidentContext) {
		if (incidentContext == null) {
			return normalize(List.of());
		}

		List<String> rawSignals = new ArrayList<>();
		var metrics = incidentContext.metricsSnapshot();
		if (metrics != null) {
			if (metrics.p95LatencyMs() != null && metrics.p95LatencyMs() > 300) {
				rawSignals.add("P99_LATENCY_HIGH");
			}
			if (metrics.errorRate() != null && metrics.errorRate() > 0.01) {
				rawSignals.add("ERROR_RATE_HIGH");
			}
			if (metrics.dbConnectionPending() != null && metrics.dbConnectionPending() > 0) {
				rawSignals.add("DB_POOL_PENDING_HIGH");
			}
			if (metrics.retryRate() != null && metrics.retryRate() >= 0.20) {
				rawSignals.add("RETRY_STORM");
				rawSignals.add("RETRY_RATE_HIGH");
			}
			if (metrics.redisTimeoutCount() != null && metrics.redisTimeoutCount() > 0) {
				rawSignals.add("REDIS_TIMEOUT_HIGH");
			}
			if (metrics.kafkaConsumerLag() != null && metrics.kafkaConsumerLag() > 1_000) {
				rawSignals.add("KAFKA_CONSUMER_LAG_HIGH");
			}
		}

		if (incidentContext.logsSample() != null) {
			incidentContext.logsSample().forEach(log -> {
				String message = log.message() == null ? "" : log.message().toLowerCase();
				if (message.contains("rebalance storm")) {
					rawSignals.add("REBALANCE_STORM");
				}
				if (message.contains("duplicate payment") || message.contains("duplicate attempt")) {
					rawSignals.add("PAYMENT_DUPLICATE_ATTEMPT");
				}
			});
		}

		String operatorNote = incidentContext.operatorNote() == null ? "" : incidentContext.operatorNote().toLowerCase();
		String alertName = incidentContext.alertName() == null ? "" : incidentContext.alertName().toLowerCase();
		if (operatorNote.contains("duplicate payment")
				|| operatorNote.contains("duplicate attempt")
				|| alertName.contains("duplicate")) {
			rawSignals.add("PAYMENT_DUPLICATE_ATTEMPT");
		}

		EvidenceContext normalized = normalize(rawSignals.stream().distinct().toList());
		return new EvidenceContext(
				incidentContext.incidentId(),
				incidentContext.service(),
				incidentContext.environment(),
				normalized.evidences(),
				incidentContext.labels() == null ? java.util.Map.of() : incidentContext.labels(),
				normalized.prometheusStatus(),
				normalized.lokiStatus(),
				normalized.jaegerStatus()
		);
	}

	public EvidenceContext merge(EvidenceContext primary, EvidenceContext secondary) {
		if (primary == null) {
			return secondary;
		}
		if (secondary == null) {
			return primary;
		}

		List<Evidence> merged = new ArrayList<>();
		merged.addAll(primary.evidences());
		merged.addAll(secondary.evidences());

		java.util.LinkedHashMap<String, String> mergedTags = new java.util.LinkedHashMap<>();
		if (primary.tags() != null) {
			mergedTags.putAll(primary.tags());
		}
		if (secondary.tags() != null) {
			mergedTags.putAll(secondary.tags());
		}

		return new EvidenceContext(
				secondary.incidentId() != null ? secondary.incidentId() : primary.incidentId(),
				secondary.service() != null ? secondary.service() : primary.service(),
				secondary.environment() != null ? secondary.environment() : primary.environment(),
				merged.stream().distinct().toList(),
				mergedTags,
				worst(primary.prometheusStatus(), secondary.prometheusStatus()),
				worst(primary.lokiStatus(), secondary.lokiStatus()),
				worst(primary.jaegerStatus(), secondary.jaegerStatus())
		);
	}

	private EvidenceQueryStatus worst(EvidenceQueryStatus left, EvidenceQueryStatus right) {
		if (left == EvidenceQueryStatus.FAILED || right == EvidenceQueryStatus.FAILED) {
			return EvidenceQueryStatus.FAILED;
		}
		if (left == EvidenceQueryStatus.PARTIAL_SUCCESS || right == EvidenceQueryStatus.PARTIAL_SUCCESS) {
			return EvidenceQueryStatus.PARTIAL_SUCCESS;
		}
		return EvidenceQueryStatus.SUCCESS;
	}
}
