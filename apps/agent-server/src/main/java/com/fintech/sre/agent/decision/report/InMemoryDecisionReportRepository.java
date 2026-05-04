package com.fintech.sre.agent.decision.report;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class InMemoryDecisionReportRepository implements DecisionReportRepository {

	private final ConcurrentHashMap<String, DecisionReport> store = new ConcurrentHashMap<>();

	@Override
	public Mono<DecisionReport> save(DecisionReport report) {
		store.put(report.id(), report);
		return Mono.just(report);
	}

	@Override
	public Mono<DecisionReport> findById(String id) {
		DecisionReport report = store.get(id);
		return report == null ? Mono.empty() : Mono.just(report);
	}

	@Override
	public Flux<DecisionReport> findByIncidentId(String incidentId) {
		return Flux.fromIterable(store.values())
				.filter(report -> incidentId.equals(report.incidentId()));
	}
}
