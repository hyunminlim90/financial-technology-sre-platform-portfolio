package com.fintech.sre.agent.observability;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.observability.model.TraceEvidence;
import com.fintech.sre.agent.observability.query.JaegerQueryBuilder;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JaegerClient {

	@Qualifier("jaegerWebClient")
	private final WebClient jaegerWebClient;
	private final JaegerQueryBuilder queryBuilder;

	public Mono<List<TraceEvidence>> query(IncidentAnalyzeRequest request) {
		if (request.traceIds() != null && !request.traceIds().isEmpty()) {
			return Flux.fromIterable(request.traceIds())
					.flatMap(traceId -> executeTraceQuery(traceId, request))
					.collectList();
		}

		return Mono.just(List.of());
	}

	private Mono<TraceEvidence> executeTraceQuery(String traceId, IncidentAnalyzeRequest request) {
		return Mono.just(queryBuilder.toTraceEvidence(traceId, request.service()));
	}
}
