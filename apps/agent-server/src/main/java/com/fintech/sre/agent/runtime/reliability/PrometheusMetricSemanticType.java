package com.fintech.sre.agent.runtime.reliability;

public enum PrometheusMetricSemanticType {
	LATENCY,
	ERROR_RATE,
	TRAFFIC,
	SATURATION,
	QUEUE_DEPTH,
	RETRY_RATE,
	RESOURCE_UTILIZATION,
	PAYMENT_CONSISTENCY,
	UNKNOWN
}
