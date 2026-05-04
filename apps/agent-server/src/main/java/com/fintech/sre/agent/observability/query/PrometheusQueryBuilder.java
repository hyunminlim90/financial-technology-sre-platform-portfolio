package com.fintech.sre.agent.observability.query;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.observability.model.QueryEvidence;

@Component
public class PrometheusQueryBuilder {

	public List<QueryEvidence> build(IncidentAnalyzeRequest request) {
		String service = request.service();

		return List.of(
				p95Latency(service),
				errorRate(service),
				retryRate(service),
				dbPending(service),
				redisTimeout(service),
				kafkaLag(service)
		);
	}

	private QueryEvidence p95Latency(String service) {
		return new QueryEvidence(
				"p95_latency_ms",
				"""
				histogram_quantile(
				  0.95,
				  rate(http_server_requests_seconds_bucket{service="%s"}[5m])
				) * 1000
				""".formatted(service),
				300.0,
				"ms",
				"API p95 latency"
		);
	}

	private QueryEvidence errorRate(String service) {
		return new QueryEvidence(
				"error_rate",
				"""
				sum(rate(http_server_requests_seconds_count{service="%s",status=~"5.."}[5m]))
				/
				sum(rate(http_server_requests_seconds_count{service="%s"}[5m]))
				""".formatted(service, service),
				0.01,
				"ratio",
				"5xx error rate"
		);
	}

	private QueryEvidence retryRate(String service) {
		return new QueryEvidence(
				"retry_rate",
				"""
				sum(rate(payment_retry_total{service="%s"}[5m]))
				/
				sum(rate(payment_request_total{service="%s"}[5m]))
				""".formatted(service, service),
				0.2,
				"ratio",
				"payment retry rate"
		);
	}

	private QueryEvidence dbPending(String service) {
		return new QueryEvidence(
				"db_connection_pending",
				"""
				r2dbc_pool_pending_connections{service="%s"}
				""".formatted(service),
				0.0,
				"count",
				"R2DBC pending connection count"
		);
	}

	private QueryEvidence redisTimeout(String service) {
		return new QueryEvidence(
				"redis_timeout_count",
				"""
				increase(redis_timeout_total{service="%s"}[5m])
				""".formatted(service),
				0.0,
				"count",
				"Redis timeout count in 5 minutes"
		);
	}

	private QueryEvidence kafkaLag(String service) {
		return new QueryEvidence(
				"kafka_consumer_lag",
				"""
				kafka_consumer_lag{service="%s"}
				""".formatted(service),
				1000.0,
				"count",
				"Kafka consumer lag"
		);
	}
}
