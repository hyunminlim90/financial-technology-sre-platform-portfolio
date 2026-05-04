package com.fintech.sre.agent.model.common;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fintech.sre.agent.model.request.DeploymentInfo;
import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.model.request.LogSample;
import com.fintech.sre.agent.model.request.MetricsSnapshot;
import com.fintech.sre.agent.observability.model.ObservabilityEvidence;

import lombok.Builder;

@Builder
public record IncidentContext(
		String incidentId,
		String alertName,
		String service,
		String environment,
		String severityHint,
		Instant occurredAt,
		Map<String, String> labels,
		MetricsSnapshot metricsSnapshot,
		List<LogSample> logsSample,
		List<String> traceIds,
		DeploymentInfo deploymentInfo,
		String operatorNote,
		ObservabilityEvidence observabilityEvidence
) {

	public static IncidentContext from(IncidentAnalyzeRequest request, ObservabilityEvidence observabilityEvidence) {
		return IncidentContext.builder()
				.incidentId(request.incidentId())
				.alertName(request.alertName())
				.service(request.service())
				.environment(request.environment())
				.severityHint(request.severityHint())
				.occurredAt(request.occurredAt())
				.labels(request.labels())
				.metricsSnapshot(request.metricsSnapshot())
				.logsSample(request.logsSample())
				.traceIds(request.traceIds())
				.deploymentInfo(request.deploymentInfo())
				.operatorNote(request.operatorNote())
				.observabilityEvidence(observabilityEvidence)
				.build();
	}

	public int availableEvidenceSources() {
		int count = 0;
		if (observabilityEvidence != null && observabilityEvidence.metrics() != null && !observabilityEvidence.metrics().isEmpty()) {
			count++;
		}
		if (observabilityEvidence != null && observabilityEvidence.logs() != null && !observabilityEvidence.logs().isEmpty()) {
			count++;
		}
		if (observabilityEvidence != null && observabilityEvidence.traces() != null && !observabilityEvidence.traces().isEmpty()) {
			count++;
		}
		return count;
	}

	public Evidence evidence() {
		if (observabilityEvidence == null) {
			return new Evidence(List.of(), List.of(), List.of());
		}
		return new Evidence(
				observabilityEvidence.metrics() == null ? List.of() : observabilityEvidence.metrics().stream()
						.map(metric -> new MetricEvidence(
								metric.name(),
								metric.value(),
								metric.threshold(),
								metric.status(),
								metric.query()
						))
						.toList(),
				observabilityEvidence.logs() == null ? List.of() : observabilityEvidence.logs().stream()
						.map(log -> "[%s] %s traceId=%s :: %s".formatted(
								log.level(),
								log.timestamp(),
								log.traceId(),
								log.message()
						))
						.toList(),
				observabilityEvidence.traces() == null ? List.of() : observabilityEvidence.traces().stream()
						.map(trace -> "traceId=%s span=%s dependency=%s durationMs=%s status=%s".formatted(
								trace.traceId(),
								trace.spanName(),
								trace.dependency(),
								trace.durationMs(),
								trace.status()
						))
						.toList()
		);
	}

	public String domainHint() {
		if (labels != null && labels.get("domain") != null && !labels.get("domain").isBlank()) {
			return labels.get("domain");
		}
		if (service != null && (service.contains("payment") || service.contains("checkout"))) {
			return "PAYMENTS";
		}
		return "PLATFORM";
	}

	public String failureModeHint() {
		List<String> joinedSignals = keywords();
		if (joinedSignals.stream().anyMatch(keyword -> keyword.contains("timeout"))) {
			return "LATENCY_AND_TIMEOUT";
		}
		if (joinedSignals.stream().anyMatch(keyword -> keyword.contains("duplicate"))) {
			return "DUPLICATE_PAYMENT_RISK";
		}
		return "UNKNOWN";
	}

	public String impactScopeHint() {
		if ("SEV_1".equalsIgnoreCase(severityHint) || "SEV1".equalsIgnoreCase(severityHint)) {
			return "GLOBAL";
		}
		return "PARTIAL";
	}

	public List<String> keywords() {
		List<String> keywords = new ArrayList<>();
		addKeyword(keywords, alertName);
		addKeyword(keywords, service);
		addKeyword(keywords, environment);
		if (labels != null) {
			labels.values().forEach(value -> addKeyword(keywords, value));
		}
		if (metricsSnapshot != null) {
			if (metricsSnapshot.retryRate() != null && metricsSnapshot.retryRate() > 0.10) {
				keywords.add("retry_rate high");
			}
			if (metricsSnapshot.redisTimeoutCount() != null && metricsSnapshot.redisTimeoutCount() > 0) {
				keywords.add("redis timeout");
			}
			if (metricsSnapshot.dbConnectionPending() != null && metricsSnapshot.dbConnectionPending() > 0) {
				keywords.add("db_connection_pending");
			}
		}
		if (logsSample != null) {
			logsSample.stream()
					.map(LogSample::message)
					.filter(Objects::nonNull)
					.limit(3)
					.forEach(message -> addKeyword(keywords, extractLogKeyword(message)));
		}
		if (deploymentInfo != null && Boolean.TRUE.equals(deploymentInfo.recentDeploy())) {
			keywords.add("recent deploy");
		}
		addKeyword(keywords, operatorNote);
		return keywords.stream().distinct().toList();
	}

	public Map<String, Object> metricHints() {
		if (metricsSnapshot == null) {
			return Map.of();
		}
		Map<String, Object> hints = new java.util.LinkedHashMap<>();
		putIfPresent(hints, "p95LatencyMs", metricsSnapshot.p95LatencyMs());
		putIfPresent(hints, "errorRate", metricsSnapshot.errorRate());
		putIfPresent(hints, "retryRate", metricsSnapshot.retryRate());
		putIfPresent(hints, "redisTimeoutCount", metricsSnapshot.redisTimeoutCount());
		putIfPresent(hints, "dbConnectionUsage", metricsSnapshot.dbConnectionUsage());
		putIfPresent(hints, "dbConnectionPending", metricsSnapshot.dbConnectionPending());
		putIfPresent(hints, "cpuUsage", metricsSnapshot.cpuUsage());
		putIfPresent(hints, "kafkaConsumerLag", metricsSnapshot.kafkaConsumerLag());
		return hints;
	}

	private void addKeyword(List<String> keywords, String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		keywords.add(raw.trim().toLowerCase());
	}

	private String extractLogKeyword(String message) {
		String lowered = message.toLowerCase();
		if (lowered.contains("timeout")) {
			return "timeout";
		}
		if (lowered.contains("redis")) {
			return "redis";
		}
		if (lowered.contains("db")) {
			return "db";
		}
		return lowered;
	}

	private void putIfPresent(Map<String, Object> hints, String key, Object value) {
		if (value != null) {
			hints.put(key, value);
		}
	}
}
