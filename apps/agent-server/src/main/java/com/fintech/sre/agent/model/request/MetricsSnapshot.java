package com.fintech.sre.agent.model.request;

public record MetricsSnapshot(
		Double p95LatencyMs,
		Double errorRate,
		Double retryRate,
		Long redisTimeoutCount,
		Double dbConnectionUsage,
		Long dbConnectionPending,
		Double cpuUsage,
		Long kafkaConsumerLag
) {
}
