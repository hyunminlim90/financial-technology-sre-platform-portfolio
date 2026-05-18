package com.fintech.sre.agent.recommendation.verification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class VerificationResultController {

	private final VerificationResultService service;

	public VerificationResultController(
			VerificationResultService service
	) {
		this.service = service;
	}

	@PostMapping("/internal/execution-results/{id}/verification")
	public Mono<VerificationResultResponse> verify(
			@PathVariable String id,
			@RequestBody VerificationResultRequest request
	) {
		return service.verify(id, request);
	}

	@GetMapping("/internal/verification-results/{id}")
	public Mono<VerificationResultRecord> findById(
			@PathVariable String id
	) {
		return service.findById(id);
	}

	@ExceptionHandler(VerificationResultRejectedException.class)
	public ResponseEntity<VerificationResultErrorResponse> rejected(
			VerificationResultRejectedException ex
	) {
		return ResponseEntity.badRequest().body(
				new VerificationResultErrorResponse(
						ex.code(),
						ex.getMessage()
				)
		);
	}
}
