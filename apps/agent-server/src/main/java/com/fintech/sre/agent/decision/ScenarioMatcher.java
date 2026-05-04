package com.fintech.sre.agent.decision;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class ScenarioMatcher {

	public Mono<MatchedScenario> match(DecisionInput input) {
		return Mono.justOrEmpty(input.ragSearchResult().scenarios())
				.flatMapMany(reactor.core.publisher.Flux::fromIterable)
				.sort(Comparator.comparingDouble(com.fintech.sre.agent.rag.RagDocument::score).reversed())
				.next()
				.filter(doc -> doc.score() >= 0.75)
				.map(doc -> new MatchedScenario(
						doc.failureMode(),
						doc.domain(),
						doc.title(),
						doc.path(),
						parseSeverity(doc.metadata() == null ? null : doc.metadata().severity()),
						parseImpactScope(doc.metadata() == null ? null : doc.metadata().impactScope())
				));
	}

	private com.fintech.sre.agent.model.common.Severity parseSeverity(String severity) {
		if (severity == null || severity.isBlank()) {
			return com.fintech.sre.agent.model.common.Severity.SEV_2;
		}
		return switch (severity.trim().toUpperCase()) {
			case "SEV_1", "SEV1" -> com.fintech.sre.agent.model.common.Severity.SEV_1;
			case "SEV_3", "SEV3" -> com.fintech.sre.agent.model.common.Severity.SEV_3;
			default -> com.fintech.sre.agent.model.common.Severity.SEV_2;
		};
	}

	private com.fintech.sre.agent.model.common.ImpactScope parseImpactScope(String impactScope) {
		if (impactScope == null || impactScope.isBlank()) {
			return com.fintech.sre.agent.model.common.ImpactScope.PARTIAL;
		}
		return com.fintech.sre.agent.model.common.ImpactScope.valueOf(impactScope.trim().toUpperCase());
	}
}
