package com.fintech.sre.agent.operator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fintech.sre.agent.actionlog.ActionLogResponse;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/operator")
public class OperatorReviewController {

	private final OperatorReviewService service;

	public OperatorReviewController(OperatorReviewService service) {
		this.service = service;
	}

	@GetMapping("/incidents/{incidentId}/review")
	public Mono<OperatorReviewSummary> getIncidentReviewSummary(
			@PathVariable String incidentId
	) {
		return service.getIncidentReviewSummary(incidentId);
	}

	@PostMapping("/action-logs/{actionLogId}/approve")
	public Mono<ActionLogResponse> approveAction(
			@PathVariable String actionLogId,
			@RequestBody(required = false) OperatorActionDecisionRequest request
	) {
		return service.approveAction(actionLogId, request);
	}

	@PostMapping("/action-logs/{actionLogId}/reject")
	public Mono<ActionLogResponse> rejectAction(
			@PathVariable String actionLogId,
			@RequestBody(required = false) OperatorActionDecisionRequest request
	) {
		return service.rejectAction(actionLogId, request);
	}

	@PostMapping("/action-logs/{actionLogId}/outcome")
	public Mono<ActionLogResponse> reportOutcome(
			@PathVariable String actionLogId,
			@RequestBody OperatorOutcomeReportRequest request
	) {
		return service.reportOutcome(actionLogId, request);
	}
}
