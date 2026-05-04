package com.fintech.sre.agent.observability;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.model.request.MetricsSnapshot;
import com.fintech.sre.agent.observability.model.MetricEvidence;
import com.fintech.sre.agent.observability.model.QueryEvidence;
import com.fintech.sre.agent.observability.query.PrometheusQueryBuilder;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PrometheusClient {

	@Qualifier("prometheusWebClient")
	private final WebClient prometheusWebClient;
	private final PrometheusQueryBuilder queryBuilder;

	public Mono<List<MetricEvidence>> query(IncidentAnalyzeRequest request) {
		List<QueryEvidence> queries = queryBuilder.build(request);
		return Flux.fromIterable(queries)
				.flatMap(query -> executeQuery(request, query))
				.filter(metric -> metric.value() != null)
				.collectList();
	}

	private Mono<MetricEvidence> executeQuery(IncidentAnalyzeRequest request, QueryEvidence query) {
		return Mono.just(toMetricEvidence(query, request.metricsSnapshot()));
	}

	private MetricEvidence toMetricEvidence(QueryEvidence query, MetricsSnapshot snapshot) {
		Double value = extractValue(query.name(), snapshot);
		String status = value != null && query.threshold() != null && value > query.threshold()
				? "abnormal"
				: "normal";

		return new MetricEvidence(
				query.name(),
				value,
				query.threshold(),
				status,
				query.query(),
				query.unit(),
				query.description()
		);
	}

	private Double extractValue(String metricName, MetricsSnapshot snapshot) {
		if (snapshot == null) {
			return null;
		}
		return switch (metricName) {
			case "p95_latency_ms" -> snapshot.p95LatencyMs();
			case "error_rate" -> snapshot.errorRate();
			case "retry_rate" -> snapshot.retryRate();
			case "redis_timeout_count" -> toDouble(snapshot.redisTimeoutCount());
			case "db_connection_usage" -> snapshot.dbConnectionUsage();
			case "db_connection_pending" -> toDouble(snapshot.dbConnectionPending());
			case "cpu_usage" -> snapshot.cpuUsage();
			case "kafka_consumer_lag" -> toDouble(snapshot.kafkaConsumerLag());
			default -> null;
		};
	}

	private Double toDouble(Long value) {
		return value == null ? null : value.doubleValue();
	}
}
