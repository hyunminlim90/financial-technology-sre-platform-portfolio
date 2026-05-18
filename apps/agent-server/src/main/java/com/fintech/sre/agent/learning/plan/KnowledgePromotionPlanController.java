package com.fintech.sre.agent.learning.plan;

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
public class KnowledgePromotionPlanController {

	private final KnowledgePromotionPlanService service;

	public KnowledgePromotionPlanController(KnowledgePromotionPlanService service) {
		this.service = service;
	}

	@PostMapping("/internal/learning-candidates/{id}/promotion-plan")
	public Mono<KnowledgePromotionPlanResponse> create(
			@PathVariable String id,
			@RequestBody KnowledgePromotionPlanRequest request
	) {
		return service.createPlan(id, request);
	}

	@GetMapping("/internal/knowledge-promotion-plans/{id}")
	public Mono<KnowledgePromotionPlanRecord> findById(@PathVariable String id) {
		return service.findById(id);
	}

	@GetMapping("/internal/incidents/{id}/knowledge-promotion-plans")
	public Flux<KnowledgePromotionPlanRecord> findByIncidentId(@PathVariable String id) {
		return service.findByIncidentId(id);
	}

	@ExceptionHandler(KnowledgePromotionPlanRejectedException.class)
	public ResponseEntity<KnowledgePromotionPlanErrorResponse> rejected(
			KnowledgePromotionPlanRejectedException ex
	) {
		return ResponseEntity.badRequest().body(new KnowledgePromotionPlanErrorResponse(
				ex.code(),
				ex.getMessage()
		));
	}
}
