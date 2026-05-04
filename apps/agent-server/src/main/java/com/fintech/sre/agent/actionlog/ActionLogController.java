package com.fintech.sre.agent.actionlog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController("learningActionLogController")
@RequestMapping("/api/action-logs")
public class ActionLogController {

	private final ActionLogService service;

	public ActionLogController(ActionLogService service) {
		this.service = service;
	}

	@GetMapping("/incidents/{incidentId}")
	public Flux<ActionLogResponse> findByIncidentId(@PathVariable String incidentId) {
		return service.findByIncidentId(incidentId)
				.map(ActionLogResponse::from);
	}

	@PostMapping("/{actionLogId}/approve")
	public Mono<ActionLogResponse> approve(
			@PathVariable String actionLogId,
			@RequestBody ActionDecisionRequest request
	) {
		return service.approve(actionLogId, request.reason())
				.map(ActionLogResponse::from);
	}

	@PostMapping("/{actionLogId}/reject")
	public Mono<ActionLogResponse> reject(
			@PathVariable String actionLogId,
			@RequestBody ActionDecisionRequest request
	) {
		return service.reject(actionLogId, request.reason())
				.map(ActionLogResponse::from);
	}

	@PostMapping("/{actionLogId}/outcome")
	public Mono<ActionLogResponse> reportOutcome(
			@PathVariable String actionLogId,
			@RequestBody ActionOutcomeRequest request
	) {
		return service.reportOutcome(actionLogId, request)
				.map(ActionLogResponse::from);
	}

	@GetMapping("/postmortem-required")
	public Flux<ActionLogResponse> findPostmortemRequired() {
		return service.findPostmortemRequired()
				.map(ActionLogResponse::from);
	}
}
