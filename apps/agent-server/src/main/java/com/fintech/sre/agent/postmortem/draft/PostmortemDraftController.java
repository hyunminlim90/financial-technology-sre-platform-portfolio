package com.fintech.sre.agent.postmortem.draft;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController("internalPostmortemDraftController")
public class PostmortemDraftController {

	private final PostmortemDraftService service;

	public PostmortemDraftController(PostmortemDraftService service) {
		this.service = service;
	}

	@PostMapping("/internal/incidents/{id}/postmortem-draft")
	public Mono<PostmortemDraftResponse> create(
			@PathVariable String id,
			@RequestBody PostmortemDraftRequest request
	) {
		return service.create(id, request);
	}

	@GetMapping("/internal/postmortem-drafts/{id}")
	public Mono<PostmortemDraftRecord> findById(@PathVariable String id) {
		return service.findById(id);
	}

	@ExceptionHandler(PostmortemDraftRejectedException.class)
	public ResponseEntity<PostmortemDraftErrorResponse> rejected(
			PostmortemDraftRejectedException ex
	) {
		return ResponseEntity.badRequest().body(new PostmortemDraftErrorResponse(
				ex.code(),
				ex.getMessage()
		));
	}
}
