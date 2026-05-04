package com.fintech.sre.agent.api;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fintech.sre.agent.application.IncidentRecommendationService;
import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentRecommendationController {

	private final IncidentRecommendationService recommendationService;

	@PostMapping("/analyze")
	public Mono<IncidentRecommendationResponse> analyze(
			@Valid @RequestBody IncidentAnalyzeRequest request
	) {
		return recommendationService.analyze(request);
	}
}
