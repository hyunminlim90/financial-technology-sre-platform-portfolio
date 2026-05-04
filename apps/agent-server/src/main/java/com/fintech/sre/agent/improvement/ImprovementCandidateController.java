package com.fintech.sre.agent.improvement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/improvement-candidates")
public class ImprovementCandidateController {

	private final ImprovementCandidateService service;

	public ImprovementCandidateController(ImprovementCandidateService service) {
		this.service = service;
	}

	@PostMapping("/incidents/{incidentId}/generate")
	public Flux<ImprovementCandidateResponse> generateFromIncident(@PathVariable String incidentId) {
		return service.generateFromIncident(incidentId)
				.map(ImprovementCandidateResponse::from);
	}

	@GetMapping("/incidents/{incidentId}")
	public Flux<ImprovementCandidateResponse> findByIncidentId(@PathVariable String incidentId) {
		return service.findByIncidentId(incidentId)
				.map(ImprovementCandidateResponse::from);
	}

	@GetMapping("/proposed")
	public Flux<ImprovementCandidateResponse> findProposed() {
		return service.findProposed()
				.map(ImprovementCandidateResponse::from);
	}

	@PostMapping("/{candidateId}/accept")
	public Mono<ImprovementCandidateResponse> accept(
			@PathVariable String candidateId,
			@RequestBody ImprovementCandidateDecisionRequest request
	) {
		return service.accept(candidateId, request.reason())
				.map(ImprovementCandidateResponse::from);
	}

	@PostMapping("/{candidateId}/reject")
	public Mono<ImprovementCandidateResponse> reject(
			@PathVariable String candidateId,
			@RequestBody ImprovementCandidateDecisionRequest request
	) {
		return service.reject(candidateId, request.reason())
				.map(ImprovementCandidateResponse::from);
	}

	@PostMapping("/{candidateId}/applied-externally")
	public Mono<ImprovementCandidateResponse> markAppliedExternally(
			@PathVariable String candidateId,
			@RequestBody ImprovementCandidateDecisionRequest request
	) {
		return service.markAppliedExternally(candidateId, request.reason())
				.map(ImprovementCandidateResponse::from);
	}
}
