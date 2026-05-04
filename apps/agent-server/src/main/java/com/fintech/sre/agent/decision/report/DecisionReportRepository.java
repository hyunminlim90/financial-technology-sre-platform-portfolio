package com.fintech.sre.agent.decision.report;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DecisionReportRepository {

	Mono<DecisionReport> save(DecisionReport report);

	Mono<DecisionReport> findById(String id);

	Flux<DecisionReport> findByIncidentId(String incidentId);
}
