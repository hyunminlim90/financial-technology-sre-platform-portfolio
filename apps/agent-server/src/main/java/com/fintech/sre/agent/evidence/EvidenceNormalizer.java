package com.fintech.sre.agent.evidence;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.IncidentContext;

@Component
public class EvidenceNormalizer {

	public EvidenceContext normalize(List<String> rawSignals) {
		if (rawSignals == null || rawSignals.isEmpty()) {
			return EvidenceContext.empty(null);
		}

		List<EvidenceSignal> signals = new ArrayList<>();
		for (String signal : rawSignals) {
			signals.add(normalizeSignal(signal));
		}

		return new EvidenceContext(
				null,
				signals.stream().distinct().toList(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
	}

	public EvidenceContext normalize(IncidentContext incidentContext) {
		if (incidentContext == null) {
			return EvidenceContext.empty(null);
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
				normalized.signals(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
	}

	public EvidenceContext merge(EvidenceContext primary, EvidenceContext secondary) {
		if (primary == null) {
			return secondary;
		}
		if (secondary == null) {
			return primary;
		}

		List<EvidenceSignal> mergedSignals = new ArrayList<>();
		mergedSignals.addAll(primary.signals());
		mergedSignals.addAll(secondary.signals());

		return new EvidenceContext(
				secondary.incidentId() != null ? secondary.incidentId() : primary.incidentId(),
				mergedSignals.stream().distinct().toList(),
				mergeIds(primary.matchedScenarioIds(), secondary.matchedScenarioIds()),
				mergeIds(primary.matchedRunbookIds(), secondary.matchedRunbookIds()),
				mergeIds(primary.matchedPostmortemIds(), secondary.matchedPostmortemIds()),
				mergeIds(primary.matchedImprovementIds(), secondary.matchedImprovementIds()),
				mergeIds(primary.matchedPreventiveDesignIds(), secondary.matchedPreventiveDesignIds()),
				mergeIds(primary.ragDocumentIds(), secondary.ragDocumentIds())
		);
	}

	private List<String> mergeIds(List<String> left, List<String> right) {
		List<String> merged = new ArrayList<>();
		if (left != null) {
			merged.addAll(left);
		}
		if (right != null) {
			merged.addAll(right);
		}
		return merged.stream().distinct().toList();
	}

	private EvidenceSignal normalizeSignal(String signal) {
		return switch (signal) {
			case "DB_POOL_SATURATED", "DB_POOL_PENDING_HIGH" -> EvidenceSignal.DB_POOL_PENDING_HIGH;
			case "REBALANCE_STORM" -> EvidenceSignal.KAFKA_REBALANCE_STORM;
			case "RETRY_STORM" -> EvidenceSignal.RETRY_STORM;
			case "RETRY_RATE_HIGH" -> EvidenceSignal.RETRY_RATE_HIGH;
			case "TRAFFIC_SPIKE" -> EvidenceSignal.TRAFFIC_SPIKE;
			case "KAFKA_CONSUMER_LAG_HIGH" -> EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH;
			case "OBSERVABILITY_SOURCE_DOWN" -> EvidenceSignal.OBSERVABILITY_SOURCE_DOWN;
			case "P99_LATENCY_HIGH" -> EvidenceSignal.P99_LATENCY_HIGH;
			case "ERROR_RATE_HIGH" -> EvidenceSignal.ERROR_RATE_HIGH;
			case "REDIS_TIMEOUT_HIGH" -> EvidenceSignal.REDIS_TIMEOUT_HIGH;
			case "PAYMENT_DUPLICATE_ATTEMPT" -> EvidenceSignal.PAYMENT_DUPLICATE_ATTEMPT;
			default -> EvidenceSignal.UNKNOWN;
		};
	}
}
