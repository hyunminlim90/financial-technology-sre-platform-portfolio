package com.fintech.sre.agent.evidence;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record EvidenceContext(
		String incidentId,
		List<EvidenceSignal> signals,
		List<String> matchedScenarioIds,
		List<String> matchedRunbookIds,
		List<String> matchedPostmortemIds,
		List<String> matchedImprovementIds,
		List<String> matchedPreventiveDesignIds,
		List<String> ragDocumentIds
) {
	public EvidenceContext(
			String incidentId,
			String service,
			String environment,
			List<Evidence> evidences,
			Map<String, String> tags,
			EvidenceQueryStatus prometheusStatus,
			EvidenceQueryStatus lokiStatus,
			EvidenceQueryStatus jaegerStatus
	) {
		this(
				incidentId,
				evidences == null ? List.of() : evidences.stream().map(Evidence::signal).distinct().toList(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
	}

	public EvidenceContext(
			List<Evidence> evidences,
			EvidenceQueryStatus prometheusStatus,
			EvidenceQueryStatus lokiStatus,
			EvidenceQueryStatus jaegerStatus
	) {
		this(
				null,
				evidences == null ? List.of() : evidences.stream().map(Evidence::signal).distinct().toList(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
	}

	public EvidenceContext {
		signals = safe(signals);
		matchedScenarioIds = safe(matchedScenarioIds);
		matchedRunbookIds = safe(matchedRunbookIds);
		matchedPostmortemIds = safe(matchedPostmortemIds);
		matchedImprovementIds = safe(matchedImprovementIds);
		matchedPreventiveDesignIds = safe(matchedPreventiveDesignIds);
		ragDocumentIds = safe(ragDocumentIds);
	}

	public static EvidenceContext empty(String incidentId) {
		return new EvidenceContext(
				incidentId,
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
	}

	public boolean hasScenarioEvidence() {
		return !matchedScenarioIds.isEmpty();
	}

	public boolean hasRunbookEvidence() {
		return !matchedRunbookIds.isEmpty();
	}

	public boolean hasOnlyRagDocsEvidence() {
		boolean hasRagDocs = !ragDocumentIds.isEmpty();
		return hasRagDocs
				&& isEmpty(matchedScenarioIds)
				&& isEmpty(matchedRunbookIds)
				&& isEmpty(matchedPostmortemIds)
				&& isEmpty(matchedImprovementIds)
				&& isEmpty(matchedPreventiveDesignIds);
	}

	public boolean hasOperationalSignals() {
		return !signals.isEmpty();
	}

	public List<EvidenceSignal> blockingSignals() {
		return signals.stream()
				.filter(signal -> signal.severity() == EvidenceSeverity.CRITICAL)
				.toList();
	}

	public boolean hasSignal(EvidenceSignal signal) {
		return hasSignalCode(signal == null ? null : signal.code());
	}

	public boolean hasReliableSignal(EvidenceSignal signal) {
		return hasSignal(signal);
	}

	public boolean hasDatabaseSaturation() {
		return hasSignal(EvidenceSignal.DB_POOL_PENDING_HIGH)
				|| hasSignal(EvidenceSignal.DB_LOCK_WAIT_HIGH)
				|| hasSignal(EvidenceSignal.DB_CONNECTION_EXHAUSTED);
	}

	public boolean hasKafkaRebalanceStorm() {
		return hasSignal(EvidenceSignal.KAFKA_REBALANCE_STORM);
	}

	public boolean hasRetryStorm() {
		return hasSignal(EvidenceSignal.RETRY_STORM) || hasSignal(EvidenceSignal.RETRY_RATE_HIGH);
	}

	public boolean hasPaymentConsistencyRisk() {
		return hasSignal(EvidenceSignal.PAYMENT_DUPLICATE_ATTEMPT)
				|| hasSignal(EvidenceSignal.PAYMENT_STATE_TRANSITION_ERROR);
	}

	public boolean observabilityDegraded() {
		return hasSignal(EvidenceSignal.OBSERVABILITY_SOURCE_DOWN);
	}

	public boolean hasMetric(String name) {
		return signals.stream()
				.anyMatch(signal ->
						signal.layer() == EvidenceLayer.OBSERVABILITY
								&& name.equals(signalName(signal))
				);
	}

	public String metricValue(String name) {
		return signals.stream()
				.filter(signal ->
						signal.layer() == EvidenceLayer.OBSERVABILITY
								&& name.equals(signalName(signal))
				)
				.map(EvidenceSignal::observedValue)
				.findFirst()
				.orElse(null);
	}

	public boolean hasErrorSpike() {
		return hasSignal(EvidenceSignal.ERROR_RATE_HIGH)
				|| "high".equalsIgnoreCase(metricValue("error.rate"));
	}

	public boolean hasLatencySpike() {
		return hasSignal(EvidenceSignal.P99_LATENCY_HIGH)
				|| "high".equalsIgnoreCase(metricValue("latency.p95"));
	}

	public List<String> signalNames() {
		return signals.stream()
				.map(this::signalName)
				.distinct()
				.toList();
	}

	public List<Evidence> evidences() {
		return signals.stream()
				.map(this::toEvidence)
				.toList();
	}

	public Map<String, String> tags() {
		return Map.of();
	}

	private boolean hasSignalCode(String code) {
		if (code == null) {
			return false;
		}
		return signals.stream().anyMatch(signal -> code.equalsIgnoreCase(signal.code()));
	}

	private String signalName(EvidenceSignal signal) {
		if (signal == null || signal.code() == null) {
			return "unknown";
		}
		return switch (signal.code()) {
			case "P99_LATENCY_HIGH" -> "latency.p95";
			case "ERROR_RATE_HIGH" -> "error.rate";
			case "TRAFFIC_SPIKE" -> "traffic.spike";
			case "DB_POOL_PENDING_HIGH" -> "db.pool.pending";
			case "DB_LOCK_WAIT_HIGH" -> "db.lock.wait";
			case "DB_CONNECTION_EXHAUSTED" -> "db.connection.exhausted";
			case "KAFKA_CONSUMER_LAG_HIGH" -> "kafka.consumer.lag";
			case "KAFKA_REBALANCE_STORM" -> "kafka.rebalance.storm";
			case "KAFKA_DLQ_RATE_HIGH" -> "kafka.dlq.rate";
			case "KAFKA_CONSUMER_RATE_LOW" -> "kafka.consumer.rate";
			case "REDIS_TIMEOUT_HIGH" -> "redis.timeout";
			case "REDIS_ERROR_RATE_HIGH" -> "redis.error.rate";
			case "RETRY_RATE_HIGH" -> "retry.rate";
			case "RETRY_STORM" -> "retry.storm";
			case "POD_RESTART_HIGH" -> "pod.restart";
			case "CPU_SATURATION" -> "cpu.usage";
			case "MEMORY_SATURATION" -> "memory.usage";
			case "PAYMENT_DUPLICATE_ATTEMPT" -> "payment.duplicate.attempt";
			case "PAYMENT_APPROVAL_DELAY_HIGH" -> "payment.approval.delay";
			case "PAYMENT_STATE_TRANSITION_ERROR" -> "payment.state.transition.error";
			case "OBSERVABILITY_SOURCE_DOWN" -> "observability.source.down";
			default -> signal.code().toLowerCase();
		};
	}

	private Evidence toEvidence(EvidenceSignal signal) {
		return new Evidence(
				signal.layer(),
				signal,
				"high".equalsIgnoreCase(signal.observedValue()) ? 1 : 0,
				1,
				java.time.Duration.ZERO,
				signal.source(),
				signal.severity(),
				blockingSignals().contains(signal) ? EvidenceConfidence.HIGH : EvidenceConfidence.MEDIUM,
				EvidenceStatus.PRESENT,
				signal.summary()
		);
	}

	public String service() {
		if (signals.stream().anyMatch(signal -> contains(signal.reference(), "payment") || contains(signal.summary(), "payment"))) {
			return "payment-service";
		}
		return null;
	}

	public String environment() {
		return null;
	}

	public EvidenceQueryStatus prometheusStatus() {
		return observabilityDegraded() ? EvidenceQueryStatus.FAILED : EvidenceQueryStatus.SUCCESS;
	}

	public EvidenceQueryStatus lokiStatus() {
		return observabilityDegraded() ? EvidenceQueryStatus.FAILED : EvidenceQueryStatus.SUCCESS;
	}

	public EvidenceQueryStatus jaegerStatus() {
		return observabilityDegraded() ? EvidenceQueryStatus.FAILED : EvidenceQueryStatus.SUCCESS;
	}

	private boolean isEmpty(List<?> values) {
		return values == null || values.isEmpty();
	}

	private static <T> List<T> safe(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}

	private boolean contains(String value, String keyword) {
		return value != null && value.toLowerCase().contains(keyword.toLowerCase());
	}
}
