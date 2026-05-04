package com.fintech.sre.agent.evidence;

import java.util.List;
import java.util.Map;

public record EvidenceContext(
		String incidentId,
		String service,
		String environment,
		List<Evidence> evidences,
		Map<String, String> tags,
		EvidenceQueryStatus prometheusStatus,
		EvidenceQueryStatus lokiStatus,
		EvidenceQueryStatus jaegerStatus
) {
	public EvidenceContext(
			List<Evidence> evidences,
			EvidenceQueryStatus prometheusStatus,
			EvidenceQueryStatus lokiStatus,
			EvidenceQueryStatus jaegerStatus
	) {
		this(null, null, null, evidences, Map.of(), prometheusStatus, lokiStatus, jaegerStatus);
	}

	public boolean hasSignal(EvidenceSignal signal) {
		return evidences.stream()
				.anyMatch(evidence -> evidence.signal() == signal && evidence.status() == EvidenceStatus.PRESENT);
	}

	public boolean hasReliableSignal(EvidenceSignal signal) {
		return evidences.stream()
				.anyMatch(evidence ->
						evidence.signal() == signal
								&& evidence.status() == EvidenceStatus.PRESENT
								&& evidence.isReliable()
				);
	}

	public boolean hasDatabaseSaturation() {
		return hasReliableSignal(EvidenceSignal.DB_POOL_PENDING_HIGH)
				|| hasReliableSignal(EvidenceSignal.DB_LOCK_WAIT_HIGH)
				|| hasReliableSignal(EvidenceSignal.DB_CONNECTION_EXHAUSTED);
	}

	public boolean hasKafkaRebalanceStorm() {
		return hasReliableSignal(EvidenceSignal.KAFKA_REBALANCE_STORM);
	}

	public boolean hasRetryStorm() {
		return hasReliableSignal(EvidenceSignal.RETRY_STORM)
				|| hasReliableSignal(EvidenceSignal.RETRY_RATE_HIGH);
	}

	public boolean hasPaymentConsistencyRisk() {
		return hasReliableSignal(EvidenceSignal.PAYMENT_DUPLICATE_ATTEMPT)
				|| hasReliableSignal(EvidenceSignal.PAYMENT_STATE_TRANSITION_ERROR);
	}

	public boolean observabilityDegraded() {
		return prometheusStatus == EvidenceQueryStatus.FAILED
				|| lokiStatus == EvidenceQueryStatus.FAILED
				|| jaegerStatus == EvidenceQueryStatus.FAILED;
	}

	public boolean hasMetric(String name) {
		return evidences.stream()
				.anyMatch(evidence ->
						evidence.signalType() == EvidenceSignalType.METRIC
								&& name.equals(evidence.signalName())
								&& evidence.status() == EvidenceStatus.PRESENT
				);
	}

	public String metricValue(String name) {
		return evidences.stream()
				.filter(evidence ->
						evidence.signalType() == EvidenceSignalType.METRIC
								&& name.equals(evidence.signalName())
				)
				.map(Evidence::signalValue)
				.findFirst()
				.orElse(null);
	}

	public boolean hasErrorSpike() {
		return hasReliableSignal(EvidenceSignal.ERROR_RATE_HIGH)
				|| "high".equalsIgnoreCase(metricValue("error.rate"));
	}

	public boolean hasLatencySpike() {
		return hasReliableSignal(EvidenceSignal.P99_LATENCY_HIGH)
				|| "high".equalsIgnoreCase(metricValue("latency.p95"));
	}

	public List<String> signalNames() {
		return evidences.stream()
				.filter(evidence -> evidence.status() == EvidenceStatus.PRESENT)
				.map(Evidence::signalName)
				.distinct()
				.toList();
	}
}
