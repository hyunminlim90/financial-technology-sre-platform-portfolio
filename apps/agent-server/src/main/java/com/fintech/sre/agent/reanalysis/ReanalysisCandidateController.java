package com.fintech.sre.agent.reanalysis;

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
public class ReanalysisCandidateController {

	private final ReanalysisCandidateService service;

	public ReanalysisCandidateController(
			ReanalysisCandidateService service
	) {
		this.service = service;
	}

	@PostMapping("/internal/incidents/{id}/reanalysis-candidates")
	public Mono<ReanalysisCandidateResponse> create(
			@PathVariable String id,
			@RequestBody ReanalysisCandidateRequest request
	) {
		return service.create(id, request);
	}

	@GetMapping("/internal/incidents/{id}/reanalysis-candidates")
	public Flux<ReanalysisTriggerCandidate> list(
			@PathVariable String id
	) {
		return service.findByIncidentId(id);
	}

	@ExceptionHandler(ReanalysisCandidateRejectedException.class)
	public ResponseEntity<ReanalysisCandidateErrorResponse> rejected(
			ReanalysisCandidateRejectedException ex
	) {
		return ResponseEntity.badRequest().body(
				new ReanalysisCandidateErrorResponse(
						ex.code(),
						ex.getMessage()
				)
		);
	}
}
