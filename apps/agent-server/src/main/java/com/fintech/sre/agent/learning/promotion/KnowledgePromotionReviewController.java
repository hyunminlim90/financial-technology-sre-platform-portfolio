package com.fintech.sre.agent.learning.promotion;

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
public class KnowledgePromotionReviewController {

	private final KnowledgePromotionReviewService service;

	public KnowledgePromotionReviewController(KnowledgePromotionReviewService service) {
		this.service = service;
	}

	@PostMapping("/internal/learning-candidates/{id}/promotion-review")
	public Mono<KnowledgePromotionReviewResponse> review(
			@PathVariable String id,
			@RequestBody KnowledgePromotionReviewRequest request
	) {
		return service.review(id, request);
	}

	@GetMapping("/internal/learning-candidates/{id}/promotion-review/latest")
	public Mono<KnowledgePromotionReviewRecord> latest(@PathVariable String id) {
		return service.latest(id);
	}

	@GetMapping("/internal/incidents/{id}/knowledge-promotion-review/history")
	public Flux<KnowledgePromotionReviewRecord> historyByIncident(@PathVariable String id) {
		return service.historyByIncident(id);
	}

	@ExceptionHandler(KnowledgePromotionReviewRejectedException.class)
	public ResponseEntity<KnowledgePromotionReviewErrorResponse> rejected(
			KnowledgePromotionReviewRejectedException ex
	) {
		return ResponseEntity.badRequest().body(new KnowledgePromotionReviewErrorResponse(
				ex.code(),
				ex.getMessage()
		));
	}
}
