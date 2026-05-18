package com.fintech.sre.agent.learning.candidate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class LearningCandidateController {

	private final LearningCandidatePromotionService service;

	public LearningCandidateController(
			LearningCandidatePromotionService service
	) {
		this.service = service;
	}

	@PostMapping(
			"/internal/postmortem-drafts/{id}/learning-candidate"
	)
	public Mono<LearningCandidatePromotionResponse> promote(
			@PathVariable String id,
			@RequestBody
			LearningCandidatePromotionRequest request
	) {
		return service.promote(id, request);
	}

	@GetMapping("/internal/learning-candidates/{id}")
	public Mono<LearningCandidateRecord> findById(
			@PathVariable String id
	) {
		return service.findById(id);
	}

	@GetMapping(
			"/internal/incidents/{id}/learning-candidates"
	)
	public Flux<LearningCandidateRecord> findByIncidentId(
			@PathVariable String id
	) {
		return service.findByIncidentId(id);
	}

	@ExceptionHandler(LearningCandidateRejectedException.class)
	public ResponseEntity<LearningCandidateErrorResponse> rejected(
			LearningCandidateRejectedException ex
	) {
		return ResponseEntity.badRequest().body(
				new LearningCandidateErrorResponse(
						ex.code(),
						ex.getMessage()
				)
		);
	}
}
