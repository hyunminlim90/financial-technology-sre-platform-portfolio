package com.fintech.sre.agent.recommendation.execution.result;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class HumanExecutionResultController {

	private final HumanExecutionResultService service;

	public HumanExecutionResultController(HumanExecutionResultService service) {
		this.service = service;
	}

	@PostMapping("/internal/execution-plans/{id}/result")
	public Mono<HumanExecutionResultResponse> record(
			@PathVariable String id,
			@RequestBody HumanExecutionResultRequest request
	) {
		return service.record(id, request);
	}

	@GetMapping("/internal/execution-results/{id}")
	public Mono<HumanExecutionResultRecord> findById(@PathVariable String id) {
		return service.findById(id);
	}

	@ExceptionHandler(HumanExecutionResultRejectedException.class)
	public ResponseEntity<HumanExecutionResultErrorResponse> rejected(
			HumanExecutionResultRejectedException ex
	) {
		return ResponseEntity.badRequest().body(new HumanExecutionResultErrorResponse(
				ex.code(),
				ex.getMessage()
		));
	}
}
