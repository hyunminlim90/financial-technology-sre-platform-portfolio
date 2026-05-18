package com.fintech.sre.agent.postmortem.review;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController("internalPostmortemReviewController")
public class PostmortemReviewController {

	private final PostmortemReviewService service;

	public PostmortemReviewController(
			PostmortemReviewService service
	) {
		this.service = service;
	}

	@PostMapping("/internal/postmortem-drafts/{id}/review")
	public Mono<PostmortemReviewResponse> review(
			@PathVariable String id,
			@RequestBody PostmortemReviewRequest request
	) {
		return service.review(id, request);
	}

	@GetMapping("/internal/postmortem-drafts/{id}/review/latest")
	public Mono<PostmortemReviewRecord> latest(
			@PathVariable String id
	) {
		return service.latest(id);
	}

	@GetMapping("/internal/incidents/{id}/postmortem-review/history")
	public Flux<PostmortemReviewRecord> history(
			@PathVariable String id
	) {
		return service.history(id);
	}

	@ExceptionHandler(PostmortemReviewRejectedException.class)
	public ResponseEntity<PostmortemReviewErrorResponse> rejected(
			PostmortemReviewRejectedException ex
	) {
		return ResponseEntity.badRequest().body(
				new PostmortemReviewErrorResponse(
						ex.code(),
						ex.getMessage()
				)
		);
	}
}
