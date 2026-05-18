package com.fintech.sre.agent.admin.knowledge;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class KnowledgeIngestionAdminController {

	private final KnowledgeIngestionAdminService service;

	public KnowledgeIngestionAdminController(KnowledgeIngestionAdminService service) {
		this.service = service;
	}

	@PostMapping("/internal/admin/knowledge/ingest")
	public Mono<ResponseEntity<KnowledgeIngestionAdminResponse>> ingest(
			@RequestBody KnowledgeIngestionAdminRequest request
	) {
		return service.ingest(request)
				.map(ResponseEntity::ok);
	}

	@ExceptionHandler(KnowledgeIngestionRejectedException.class)
	public ResponseEntity<KnowledgeIngestionAdminResponse> rejected(
			KnowledgeIngestionRejectedException ex
	) {
		return ResponseEntity.badRequest().body(new KnowledgeIngestionAdminResponse(
				null,
				ex.code(),
				false,
				0,
				0,
				0,
				0,
				0,
				java.util.List.of(),
				java.util.List.of(ex.getMessage())
		));
	}
}
