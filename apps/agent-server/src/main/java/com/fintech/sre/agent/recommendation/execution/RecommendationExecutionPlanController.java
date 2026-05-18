package com.fintech.sre.agent.recommendation.execution;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class RecommendationExecutionPlanController {

	private final RecommendationExecutionPlanService service;

	public RecommendationExecutionPlanController(
			RecommendationExecutionPlanService service
	) {
		this.service = service;
	}

	@PostMapping("/internal/recommendations/{id}/execution-plan")
	public Mono<ExecutionPlanResponse> create(
			@PathVariable String id,
			@RequestBody ExecutionPlanRequest request
	) {
		return service.createDryRunPlan(id, request);
	}

	@GetMapping("/internal/execution-plans/{id}")
	public Mono<RecommendationExecutionPlan> findById(
			@PathVariable String id
	) {
		return service.findById(id);
	}

	@ExceptionHandler(ExecutionPlanRejectedException.class)
	public ResponseEntity<ExecutionPlanErrorResponse> rejected(
			ExecutionPlanRejectedException ex
	) {
		return ResponseEntity.badRequest().body(new ExecutionPlanErrorResponse(
				ex.code(),
				ex.getMessage()
		));
	}
}
