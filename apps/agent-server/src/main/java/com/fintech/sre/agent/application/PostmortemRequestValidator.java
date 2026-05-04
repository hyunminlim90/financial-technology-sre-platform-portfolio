package com.fintech.sre.agent.application;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.request.PostmortemGenerateByIncidentRequest;

import reactor.core.publisher.Mono;

@Component
public class PostmortemRequestValidator {

	public Mono<Void> validate(PostmortemGenerateByIncidentRequest request) {
		if (request == null) {
			return Mono.error(new IllegalArgumentException("request must not be null"));
		}
		if (request.incidentId() == null || request.incidentId().isBlank()) {
			return Mono.error(new IllegalArgumentException("incidentId must not be blank"));
		}

		return Mono.empty();
	}
}
