package com.fintech.sre.agent.postmortem;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/postmortem-drafts")
public class PostmortemDraftController {

	private final PostmortemDraftService service;

	public PostmortemDraftController(PostmortemDraftService service) {
		this.service = service;
	}

	@GetMapping("/incidents/{incidentId}")
	public Mono<PostmortemDraftResponse> generateDraft(@PathVariable String incidentId) {
		return service.generateDraft(incidentId);
	}
}
