package com.fintech.sre.agent.evidence;

import java.time.Duration;

public record Evidence(
		EvidenceLayer layer,
		EvidenceSignal signal,
		double value,
		double threshold,
		Duration duration,
		EvidenceSource source,
		EvidenceSeverity severity,
		EvidenceConfidence confidence,
		EvidenceStatus status,
		String description
) {
	public EvidenceSignalType signalType() {
		return switch (signal) {
			case P99_LATENCY_HIGH,
				 ERROR_RATE_HIGH,
				 TRAFFIC_SPIKE,
				 DB_POOL_PENDING_HIGH,
				 DB_LOCK_WAIT_HIGH,
				 DB_CONNECTION_EXHAUSTED,
				 KAFKA_CONSUMER_LAG_HIGH,
				 KAFKA_REBALANCE_STORM,
				 KAFKA_DLQ_RATE_HIGH,
				 KAFKA_CONSUMER_RATE_LOW,
				 REDIS_TIMEOUT_HIGH,
				 REDIS_ERROR_RATE_HIGH,
				 RETRY_RATE_HIGH,
				 RETRY_STORM,
				 POD_RESTART_HIGH,
				 CPU_SATURATION,
				 MEMORY_SATURATION,
				 PAYMENT_APPROVAL_DELAY_HIGH -> EvidenceSignalType.METRIC;
			case PAYMENT_DUPLICATE_ATTEMPT,
				 PAYMENT_STATE_TRANSITION_ERROR -> EvidenceSignalType.EVENT;
			case OBSERVABILITY_SOURCE_DOWN,
				 UNKNOWN -> EvidenceSignalType.LOG;
		};
	}

	public String signalName() {
		return switch (signal) {
			case P99_LATENCY_HIGH -> "latency.p95";
			case ERROR_RATE_HIGH -> "error.rate";
			case TRAFFIC_SPIKE -> "traffic.spike";
			case DB_POOL_PENDING_HIGH -> "db.pool.pending";
			case DB_LOCK_WAIT_HIGH -> "db.lock.wait";
			case DB_CONNECTION_EXHAUSTED -> "db.connection.exhausted";
			case KAFKA_CONSUMER_LAG_HIGH -> "kafka.consumer.lag";
			case KAFKA_REBALANCE_STORM -> "kafka.rebalance.storm";
			case KAFKA_DLQ_RATE_HIGH -> "kafka.dlq.rate";
			case KAFKA_CONSUMER_RATE_LOW -> "kafka.consumer.rate";
			case REDIS_TIMEOUT_HIGH -> "redis.timeout";
			case REDIS_ERROR_RATE_HIGH -> "redis.error.rate";
			case RETRY_RATE_HIGH -> "retry.rate";
			case RETRY_STORM -> "retry.storm";
			case POD_RESTART_HIGH -> "pod.restart";
			case CPU_SATURATION -> "cpu.usage";
			case MEMORY_SATURATION -> "memory.usage";
			case PAYMENT_DUPLICATE_ATTEMPT -> "payment.duplicate.attempt";
			case PAYMENT_APPROVAL_DELAY_HIGH -> "payment.approval.delay";
			case PAYMENT_STATE_TRANSITION_ERROR -> "payment.state.transition.error";
			case OBSERVABILITY_SOURCE_DOWN -> "observability.source.down";
			case UNKNOWN -> "unknown";
		};
	}

	public String signalValue() {
		return exceedsThreshold() || status == EvidenceStatus.PRESENT ? "high" : "normal";
	}

	public boolean exceedsThreshold() {
		return value >= threshold;
	}

	public boolean isReliable() {
		return confidence == EvidenceConfidence.HIGH || confidence == EvidenceConfidence.MEDIUM;
	}
}
