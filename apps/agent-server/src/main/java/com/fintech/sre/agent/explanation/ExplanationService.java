package com.fintech.sre.agent.explanation;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.decision.report.DecisionReportRepository;

import reactor.core.publisher.Mono;

@Service
public class ExplanationService {

	private final DecisionReportRepository decisionReportRepository;
	private final ExplanationPort explanationPort;

	public ExplanationService(
			DecisionReportRepository decisionReportRepository,
			ExplanationPort explanationPort
	) {
		this.decisionReportRepository = decisionReportRepository;
		this.explanationPort = explanationPort;
	}

	public Mono<ExplanationResponse> explainDecisionReport(
			String decisionReportId,
			String operatorQuestion
	) {
		return decisionReportRepository.findById(decisionReportId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException(
						"DecisionReport not found: " + decisionReportId
				)))
				.flatMap(report -> explanationPort.explain(new ExplanationRequest(
						report.incidentId(),
						report,
						operatorQuestion
				)))
				.map(this::enforceSafetyFlags);
	}

	private ExplanationResponse enforceSafetyFlags(ExplanationResponse response) {
		return new ExplanationResponse(
				response.incidentId(),
				response.explanation(),
				false,
				false,
				true
		);
	}
}
