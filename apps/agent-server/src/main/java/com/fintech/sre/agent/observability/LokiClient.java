package com.fintech.sre.agent.observability;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.model.request.LogSample;
import com.fintech.sre.agent.observability.model.LogEvidence;
import com.fintech.sre.agent.observability.model.QueryEvidence;
import com.fintech.sre.agent.observability.query.LokiQueryBuilder;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LokiClient {

	@Qualifier("lokiWebClient")
	private final WebClient lokiWebClient;
	private final LokiQueryBuilder queryBuilder;

	public Mono<List<LogEvidence>> query(IncidentAnalyzeRequest request) {
		List<QueryEvidence> queries = queryBuilder.build(request);
		return Flux.fromIterable(queries)
				.flatMap(query -> executeQuery(request, query))
				.flatMapIterable(list -> list)
				.collectList();
	}

	private Mono<List<LogEvidence>> executeQuery(IncidentAnalyzeRequest request, QueryEvidence query) {
		if (request.logsSample() != null && !request.logsSample().isEmpty()) {
			return Mono.just(request.logsSample().stream()
					.map(sample -> toLogEvidence(sample, query))
					.toList());
		}

		return Mono.just(List.of());
	}

	private LogEvidence toLogEvidence(LogSample sample, QueryEvidence query) {
		return new LogEvidence(
				sample.timestamp(),
				sample.level(),
				sample.message(),
				sample.traceId(),
				query.query(),
				"loki"
		);
	}
}
