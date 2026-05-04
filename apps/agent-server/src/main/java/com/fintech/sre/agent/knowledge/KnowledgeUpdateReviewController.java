package com.fintech.sre.agent.knowledge;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/knowledge-update-reviews")
public class KnowledgeUpdateReviewController {

	private final KnowledgeUpdateReviewService service;

	public KnowledgeUpdateReviewController(KnowledgeUpdateReviewService service) {
		this.service = service;
	}

	@PostMapping
	public Mono<KnowledgeUpdateReviewResponse> create(@RequestBody KnowledgeUpdateCreateRequest request) {
		return service.create(request)
				.map(KnowledgeUpdateReviewResponse::from);
	}

	@PostMapping("/improvement-candidates/{candidateId}/create")
	public Flux<KnowledgeUpdateReviewResponse> createFromAcceptedImprovementCandidate(
			@PathVariable String candidateId
	) {
		return service.createFromAcceptedImprovementCandidate(candidateId)
				.map(KnowledgeUpdateReviewResponse::from);
	}

	@GetMapping("/incidents/{incidentId}")
	public Flux<KnowledgeUpdateReviewResponse> findByIncidentId(@PathVariable String incidentId) {
		return service.findByIncidentId(incidentId)
				.map(KnowledgeUpdateReviewResponse::from);
	}

	@GetMapping("/requested")
	public Flux<KnowledgeUpdateReviewResponse> findRequested() {
		return service.findRequested()
				.map(KnowledgeUpdateReviewResponse::from);
	}

	@PostMapping("/{reviewId}/approve")
	public Mono<KnowledgeUpdateReviewResponse> approve(
			@PathVariable String reviewId,
			@RequestBody KnowledgeUpdateDecisionRequest request
	) {
		return service.approve(reviewId, request.reason())
				.map(KnowledgeUpdateReviewResponse::from);
	}

	@PostMapping("/{reviewId}/reject")
	public Mono<KnowledgeUpdateReviewResponse> reject(
			@PathVariable String reviewId,
			@RequestBody KnowledgeUpdateDecisionRequest request
	) {
		return service.reject(reviewId, request.reason())
				.map(KnowledgeUpdateReviewResponse::from);
	}

	@PostMapping("/{reviewId}/applied-externally")
	public Mono<KnowledgeUpdateReviewResponse> markAppliedExternally(
			@PathVariable String reviewId,
			@RequestBody KnowledgeUpdateDecisionRequest request
	) {
		return service.markAppliedExternally(reviewId, request.reason())
				.map(KnowledgeUpdateReviewResponse::from);
	}

	@PostMapping("/{reviewId}/cancel")
	public Mono<KnowledgeUpdateReviewResponse> cancel(
			@PathVariable String reviewId,
			@RequestBody KnowledgeUpdateDecisionRequest request
	) {
		return service.cancel(reviewId, request.reason())
				.map(KnowledgeUpdateReviewResponse::from);
	}
}
