package com.fintech.sre.agent.explanation;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/explanations")
public class ExplanationController {

	private final ExplanationService service;

	public ExplanationController(ExplanationService service) {
		this.service = service;
	}

	@PostMapping("/decision-reports/{decisionReportId}")
	public Mono<ExplanationResponse> explainDecisionReport(
			@PathVariable String decisionReportId,
			@RequestBody(required = false) ExplanationQuestionRequest request
	) {
		return service.explainDecisionReport(
				decisionReportId,
				request == null ? null : request.question()
		);
	}

	public record ExplanationQuestionRequest(
			String question
	) {
	}
}
