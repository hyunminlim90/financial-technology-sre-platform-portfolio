package com.fintech.sre.agent.learning.application;

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
public class KnowledgeUpdateApplicationController {

	private final KnowledgeUpdateApplicationService service;

	public KnowledgeUpdateApplicationController(
			KnowledgeUpdateApplicationService service
	) {
		this.service = service;
	}

	@PostMapping(
			"/internal/learning-candidates/{id}/knowledge-update"
	)
	public Mono<KnowledgeUpdateApplicationResponse> apply(
			@PathVariable String id,
			@RequestBody
			KnowledgeUpdateApplicationRequest request
	) {
		return service.apply(id, request);
	}

	@GetMapping("/internal/knowledge-updates/{id}")
	public Mono<KnowledgeUpdateApplicationRecord> findById(
			@PathVariable String id
	) {
		return service.findById(id);
	}

	@GetMapping("/internal/incidents/{id}/knowledge-updates")
	public Flux<KnowledgeUpdateApplicationRecord> findByIncidentId(
			@PathVariable String id
	) {
		return service.findByIncidentId(id);
	}

	@ExceptionHandler(
			KnowledgeUpdateApplicationRejectedException.class
	)
	public ResponseEntity<KnowledgeUpdateApplicationErrorResponse>
	rejected(
			KnowledgeUpdateApplicationRejectedException ex
	) {
		return ResponseEntity.badRequest().body(
				new KnowledgeUpdateApplicationErrorResponse(
						ex.code(),
						ex.getMessage()
				)
		);
	}
}
