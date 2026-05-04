package com.fintech.sre.agent.observability;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.exception.InsufficientEvidenceException;
import com.fintech.sre.agent.model.common.IncidentContext;
import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.observability.model.LogEvidence;
import com.fintech.sre.agent.observability.model.MetricEvidence;
import com.fintech.sre.agent.observability.model.ObservabilityEvidence;
import com.fintech.sre.agent.observability.model.TraceEvidence;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ObservabilityQueryService {

	private final PrometheusClient prometheusClient;
	private final LokiClient lokiClient;
	private final JaegerClient jaegerClient;

	public Mono<IncidentContext> enrich(IncidentAnalyzeRequest request) {
		Mono<List<MetricEvidence>> metrics = prometheusClient.query(request)
				.onErrorReturn(List.of());
		Mono<List<LogEvidence>> logs = lokiClient.query(request)
				.onErrorReturn(List.of());
		Mono<List<TraceEvidence>> traces = jaegerClient.query(request)
				.onErrorReturn(List.of());

		return Mono.zip(metrics, logs, traces)
				.map(tuple -> new ObservabilityEvidence(
						tuple.getT1(),
						tuple.getT2(),
						tuple.getT3()
				))
				.flatMap(evidence -> {
					if (!evidence.hasAnyEvidence()) {
						return Mono.error(new InsufficientEvidenceException("No observability evidence collected."));
					}
					return Mono.just(IncidentContext.from(request, evidence));
				});
	}
}
