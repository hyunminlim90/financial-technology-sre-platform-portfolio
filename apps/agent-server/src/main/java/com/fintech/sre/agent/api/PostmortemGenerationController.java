package com.fintech.sre.agent.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fintech.sre.agent.application.PostmortemGenerationService;
import com.fintech.sre.agent.model.request.PostmortemGenerateByIncidentRequest;
import com.fintech.sre.agent.model.response.PostmortemDraftResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/postmortems")
@RequiredArgsConstructor
public class PostmortemGenerationController {

	private final PostmortemGenerationService postmortemGenerationService;

	@PostMapping("/generate")
	public Mono<PostmortemDraftResponse> generate(
			@Valid @RequestBody PostmortemGenerateByIncidentRequest request
	) {
		return postmortemGenerationService.generate(request);
	}
}
