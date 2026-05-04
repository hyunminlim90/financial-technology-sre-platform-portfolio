package com.fintech.sre.agent.explanation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.decision.report.DecisionReport;
import com.fintech.sre.agent.decision.report.DecisionReportRepository;
import com.fintech.sre.agent.decision.report.DecisionReportStatus;
import com.fintech.sre.agent.decision.report.InMemoryDecisionReportRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ExplanationServiceTest {

	@Test
	void shouldForceSafetyFlags() {
		DecisionReportRepository repository = new InMemoryDecisionReportRepository();

		DecisionReport report = new DecisionReport(
				"report-1",
				"inc-1",
				"scenario-1",
				"runbook-1",
				DecisionReportStatus.HUMAN_REVIEW_REQUIRED,
				List.of(),
				List.of(),
				List.of(),
				List.of("human review required"),
				"# Decision Report",
				Instant.now(),
				Instant.now()
		);

		ExplanationPort unsafePort = request -> Mono.just(new ExplanationResponse(
				request.incidentId(),
				"unsafe explanation",
				true,
				true,
				false
		));

		ExplanationService service = new ExplanationService(repository, unsafePort);

		StepVerifier.create(repository.save(report).then(service.explainDecisionReport("report-1", "why?")))
				.expectNextMatches(response ->
						!response.rootCauseInferred()
								&& !response.actionDecisionMadeByLlm()
								&& response.requiresHumanReview()
				)
				.verifyComplete();
	}
}
