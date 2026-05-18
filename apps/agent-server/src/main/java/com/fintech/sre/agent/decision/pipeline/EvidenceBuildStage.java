package com.fintech.sre.agent.decision.pipeline;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.evidence.EvidenceContextProvider;
import com.fintech.sre.agent.evidence.EvidenceNormalizer;

import reactor.core.publisher.Mono;

@Component
@Order(10)
public class EvidenceBuildStage implements DecisionPipelineStage {

	private final EvidenceContextProvider evidenceContextProvider;
	private final EvidenceNormalizer evidenceNormalizer;

	public EvidenceBuildStage(
			EvidenceContextProvider evidenceContextProvider,
			EvidenceNormalizer evidenceNormalizer
	) {
		this.evidenceContextProvider = evidenceContextProvider;
		this.evidenceNormalizer = evidenceNormalizer;
	}

	@Override
	public Mono<DecisionContext> execute(DecisionContext context) {
		return evidenceContextProvider.build(context.request())
				.map(provided -> context.input() == null
						? provided
						: evidenceNormalizer.merge(
								provided,
								evidenceNormalizer.normalize(context.input().incidentContext())
						))
				.map(context::withEvidenceContext);
	}
}
