package com.fintech.sre.agent.evidence;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record EvidenceSignal(
		String id,
		EvidenceLayer layer,
		EvidenceSource source,
		EvidenceSeverity severity,
		String code,
		String summary,
		String observedValue,
		String expectedValue,
		String reference
) {
	private static final Map<String, EvidenceSignal> KNOWN = new LinkedHashMap<>();

	public static final EvidenceSignal P99_LATENCY_HIGH = register(new EvidenceSignal(
			"p99-latency-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"P99_LATENCY_HIGH", "p95 latency spike detected.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal ERROR_RATE_HIGH = register(new EvidenceSignal(
			"error-rate-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"ERROR_RATE_HIGH", "5xx error rate increased.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal TRAFFIC_SPIKE = register(new EvidenceSignal(
			"traffic-spike", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"TRAFFIC_SPIKE", "Traffic spike detected.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal DB_POOL_PENDING_HIGH = register(new EvidenceSignal(
			"db-pool-pending-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.CRITICAL,
			"DB_POOL_PENDING_HIGH", "Database connection pool pending is high.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal DB_LOCK_WAIT_HIGH = register(new EvidenceSignal(
			"db-lock-wait-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.CRITICAL,
			"DB_LOCK_WAIT_HIGH", "Database lock wait is high.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal DB_CONNECTION_EXHAUSTED = register(new EvidenceSignal(
			"db-connection-exhausted", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.CRITICAL,
			"DB_CONNECTION_EXHAUSTED", "Database connections exhausted.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal KAFKA_CONSUMER_LAG_HIGH = register(new EvidenceSignal(
			"kafka-consumer-lag-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"KAFKA_CONSUMER_LAG_HIGH", "Kafka consumer lag high.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal KAFKA_REBALANCE_STORM = register(new EvidenceSignal(
			"kafka-rebalance-storm", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.CRITICAL,
			"KAFKA_REBALANCE_STORM", "Kafka rebalance storm detected.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal KAFKA_DLQ_RATE_HIGH = register(new EvidenceSignal(
			"kafka-dlq-rate-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"KAFKA_DLQ_RATE_HIGH", "Kafka DLQ rate high.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal KAFKA_CONSUMER_RATE_LOW = register(new EvidenceSignal(
			"kafka-consumer-rate-low", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"KAFKA_CONSUMER_RATE_LOW", "Kafka consumer rate low.", "low", "normal", "prometheus"
	));
	public static final EvidenceSignal REDIS_TIMEOUT_HIGH = register(new EvidenceSignal(
			"redis-timeout-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"REDIS_TIMEOUT_HIGH", "Redis timeout high.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal REDIS_ERROR_RATE_HIGH = register(new EvidenceSignal(
			"redis-error-rate-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"REDIS_ERROR_RATE_HIGH", "Redis error rate high.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal RETRY_RATE_HIGH = register(new EvidenceSignal(
			"retry-rate-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"RETRY_RATE_HIGH", "Retry rate is elevated.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal RETRY_STORM = register(new EvidenceSignal(
			"retry-storm", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.CRITICAL,
			"RETRY_STORM", "Retry storm detected.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal POD_RESTART_HIGH = register(new EvidenceSignal(
			"pod-restart-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"POD_RESTART_HIGH", "Pod restart count high.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal CPU_SATURATION = register(new EvidenceSignal(
			"cpu-saturation", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"CPU_SATURATION", "CPU saturation detected.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal MEMORY_SATURATION = register(new EvidenceSignal(
			"memory-saturation", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"MEMORY_SATURATION", "Memory saturation detected.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal PAYMENT_DUPLICATE_ATTEMPT = register(new EvidenceSignal(
			"payment-duplicate-attempt", EvidenceLayer.OBSERVABILITY, EvidenceSource.LOKI, EvidenceSeverity.CRITICAL,
			"PAYMENT_DUPLICATE_ATTEMPT", "Duplicate payment attempt detected.", "present", "absent", "loki"
	));
	public static final EvidenceSignal PAYMENT_APPROVAL_DELAY_HIGH = register(new EvidenceSignal(
			"payment-approval-delay-high", EvidenceLayer.OBSERVABILITY, EvidenceSource.PROMETHEUS, EvidenceSeverity.WARNING,
			"PAYMENT_APPROVAL_DELAY_HIGH", "Payment approval delay high.", "high", "normal", "prometheus"
	));
	public static final EvidenceSignal PAYMENT_STATE_TRANSITION_ERROR = register(new EvidenceSignal(
			"payment-state-transition-error", EvidenceLayer.OBSERVABILITY, EvidenceSource.LOKI, EvidenceSeverity.CRITICAL,
			"PAYMENT_STATE_TRANSITION_ERROR", "Payment state transition error detected.", "present", "absent", "loki"
	));
	public static final EvidenceSignal OBSERVABILITY_SOURCE_DOWN = register(new EvidenceSignal(
			"observability-source-down", EvidenceLayer.OBSERVABILITY, EvidenceSource.MANUAL_INPUT, EvidenceSeverity.WARNING,
			"OBSERVABILITY_SOURCE_DOWN", "Observability source degraded.", "down", "up", "manual"
	));
	public static final EvidenceSignal UNKNOWN = register(new EvidenceSignal(
			"unknown", EvidenceLayer.REQUEST, EvidenceSource.MANUAL_INPUT, EvidenceSeverity.INFO,
			"UNKNOWN", "Unknown evidence signal.", "unknown", "unknown", "unknown"
	));

	public static EvidenceSignal valueOf(String value) {
		if (value == null || value.isBlank()) {
			return UNKNOWN;
		}
		return KNOWN.getOrDefault(value.trim().toUpperCase(), UNKNOWN);
	}

	@JsonIgnore
	public String name() {
		return code;
	}

	private static EvidenceSignal register(EvidenceSignal signal) {
		KNOWN.put(signal.code().toUpperCase(), signal);
		return signal;
	}
}
