package com.fintech.sre.agent.recommendation.persistence;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class RecommendationRecordController {

	private final RecommendationPersistenceService service;

	public RecommendationRecordController(RecommendationPersistenceService service) {
		this.service = service;
	}

	@GetMapping("/internal/recommendations/{id}")
	public Mono<RecommendationRecord> findById(@PathVariable String id) {
		return service.findById(id);
	}

	@GetMapping("/internal/recommendations/by-incident/{incidentId}")
	public Flux<RecommendationRecord> findByIncidentId(@PathVariable String incidentId) {
		return service.findByIncidentId(incidentId);
	}

	@GetMapping("/internal/recommendations/recent")
	public Flux<RecommendationRecord> findRecent(@RequestParam(defaultValue = "50") int limit) {
		return service.findRecent(limit);
	}
}
