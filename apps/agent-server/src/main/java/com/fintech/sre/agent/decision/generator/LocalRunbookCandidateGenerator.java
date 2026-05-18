package com.fintech.sre.agent.decision.generator;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.DecisionCandidate;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;
import com.fintech.sre.agent.runbook.RunbookActionMapper;
import com.fintech.sre.agent.runbook.RunbookLoader;

import reactor.core.publisher.Mono;

@Component
@Profile({"local", "dev", "test"})
public class LocalRunbookCandidateGenerator implements CandidateGenerator {

	private final RunbookLoader runbookLoader;
	private final RunbookActionMapper runbookActionMapper;

	public LocalRunbookCandidateGenerator(
			RunbookLoader runbookLoader,
			RunbookActionMapper runbookActionMapper
	) {
		this.runbookLoader = runbookLoader;
		this.runbookActionMapper = runbookActionMapper;
	}

	@Override
	public CandidateGenerationSource source() {
		return CandidateGenerationSource.LOCAL_BOOTSTRAP_RUNBOOK;
	}

	@Override
	public Mono<List<DecisionCandidate>> generate(
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	) {
		return runbookLoader.loadMatching(request, evidenceContext)
				.map(runbook -> runbookActionMapper.toCandidate(
						runbook,
						request,
						evidenceContext
				).withCandidateGenerationSource(source()))
				.collectList();
	}
}
